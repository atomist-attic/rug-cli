#!/bin/bash

# Build a debian package using the fpm builder so 
# we don't have to worry about the internals of the debian
# packaging machinery

set -o pipefail
declare Pkg="deb-package"
declare Version="0.1.0"
declare Author="Atomist, Inc <opensource@atomist.com>"

function err() {
    echo "$Pkg: $*" 1>&2
}

function install_deps() {
    apt-get install -y -q ruby-dev build-essential jq curl
    gem install fpm
}

function gitentry2debentry() {
    local previous=$1
    if [ -z ${previous+x} ]; then
        err "missing required argument: previous"
        return 10
    fi
    shift

    local version=$1
    if [ -z ${version+x} ]; then
        err "missing required argument: version"
        return 10
    fi

    echo "rug-cli ($version) unstable; urgency=low"
    echo
    if ! git --no-pager log --format="  * %s" $previous${previous:+..}$version; then 
        err "Failed to extract logs"
        return 1
    fi
    echo
    if ! git --no-pager log --format=" -- $Author  %aD" -n 1 $version; then
        err "Failed to extract author"
        return 1
    fi
    echo
}

function git2deb_changelog() {
    local project_version=$1
    if [[ ! $project_version ]]; then
        err "missing required argument: project_version"
        return 10
    fi

    local current_lang=$LANG
    if [[ "${LANG}" != "en_US.UTF-8" ]]; then
        # make sure git log dates are in English
        # git log messes that up otherwise when using the
        # --date="format:%a %b %d %Y" argument
        LANG="en_US.UTF-8"
    fi

    # iterate over all release tag and generate debian changelog stanzas
    # borrowed from https://gist.github.com/hfs/0ff1cf243b163bd551ec22604ce702a5/
    local tags=$(git tag --sort "-version:refname" | grep -E -o "^[0-9]+\.[0-9]+\.[0-9]+$")
    if [[ -z "${tags}" ]]; then
        # no tags there yet, so let's pretend
        echo "No tags found"
        if ! gitentry2debentry "" "" > target/linux/deb/changelog; then
            err "Failed to create single changelog entry"
            return 1
        fi
        if ! sed -i "s/()/($project_version)/" target/linux/deb/changelog; then
            err "Failed setting project version in changelog"
            return 1
        fi
    else
        if ! echo "$tags" | {
            read version; while read previous; do
                    if ! gitentry2debentry $previous $version; then
                        err "Failed to create changelog entry"
                        return 1
                    fi
                    version="$previous"
            done
            if ! gitentry2debentry "" $version; then 
                err "Failed to create first changelog entry"
                return 1
            fi
        } > target/linux/deb/changelog; then
            err "Failed to create changelog"
            return 1
        fi
    fi

    LANG=$current_lang
}

function create_directory_structure() {
    # this directory structure maps how we the resources will
    # be deployed on target machines
    echo "Creating directory structure..."
    rm -rf target/linux/deb
    mkdir -p target/linux/deb/rug-cli/usr/share/{bash-completion/completions,doc/rug-cli,rug/{bin,lib}}
}

function copy_files() {
    local project_version=$1
    if [[ ! $project_version ]]; then
        err "missing required argument: project_version"
        return 10
    fi

    local source_dir="target/rug-cli-$project_version-bin/rug-cli-$project_version"
    local dest_dir="target/linux/deb/rug-cli/usr/share"

    if ! echo "ln -s /usr/share/rug/bin/rug /usr/bin/rug" > postinst; then 
        err "Failed to create postinst file"
        return 1
    fi
    if ! echo "rm /usr/bin/rug" > preuninst; then
        err "Failed to create preuninst file"
        return 1
    fi
    if ! cp src/main/scripts/debian/copyright $dest_dir/doc/rug-cli/copyright; then
        err "Failed to copy debian copyright file"
        return 1
    fi
    if ! cp src/main/bash/etc/bash_completion.d/rug $dest_dir/bash-completion/completions/rug; then 
        err "Failed to copy bash completion file"
        return 1
    fi
    if ! cp $source_dir/bin/rug $dest_dir/rug/bin/rug; then
        err "Failed to copy rug binary"
        return 1
    fi
    if ! cp $source_dir/lib/rug-cli-$project_version.jar $dest_dir/rug/lib/rug-cli-$project_version.jar; then 
        err "Failed to copy rug jar file"
        return 1
    fi
}

function upload_to_repository() {
    echo "Uploading to repository..."

    local package=$1
    if [[ ! $package ]]; then
        err "missing required argument: package"
        return 10
    fi

    local debian_repo_url="https://atomist.jfrog.io/atomist/debian/pool"
    local distributions="deb.distribution=wheezy;deb.distribution=jessie;deb.distribution=yakkety;deb.distribution=xenial;deb.distribution=wily;deb.distribution=vivid"
    local components="deb.component=main"
    local arch="deb.architecture=all"
    local checksum=$(sha1sum $package | awk '{ print $1 }')

    echo "Package checksum: $checksum"

    local response=$(curl -s -u $ARTIFACTORY_USER:$ARTIFACTORY_PASSWORD \
                          -H "X-Checksum-Sha1:$checksum"\
                          -T $package\
                          -XPUT \
                          "$debian_repo_url/$package;$distributions;$components;$arch")

    if [[ $(echo $response | jq '.errors? | length') -ne 0 ]]; then
        err $response
        return 1
    fi
}

function create_package() {
    local project_version=$1
    if [[ ! $project_version ]]; then
        err "missing required argument: project_version"
        return 10
    fi

    # generate the debian package
    echo "Creating package..."
    fpm -s dir \
        -t deb \
        -a all \
        -n rug-cli \
        -v $project_version \
        -m "$Author" \
        --description "Atomist rug CLI" \
        --url https://www.atomist.com \
        --license "GPL-3.0" \
        --vendor "Atomist, Inc" \
        -d "default-jdk" \
        --deb-changelog target/linux/deb/changelog \
        --after-install=postinst \
        --pre-uninstall=preuninst \
        -C target/linux/deb/rug-cli
}

function main() {
    if [[ $TRAVIS ]]; then
        install_deps
    fi

    if [[ ! $ARTIFACTORY_USER ]]; then
        err "missing artifactory user ARTIFACTORY_USER"
        return 1
    fi

    if [[ ! $ARTIFACTORY_PASSWORD ]]; then
        err "missing artifactory password ARTIFACTORY_PASSWORD"
        return 1
    fi

    # lookup our version from the maven pom file
    local project_version=$(mvn help:evaluate -Dexpression=project.version | grep -v "^\[")
    if [[ ! $project_version ]]; then
        err "could not locate project version"
        return 1
    fi

    if ! create_directory_structure; then
        err "failed to create directory structure"
        return 1
    fi

    # populate a bunch of files
    if ! git2deb_changelog $project_version; then
        err "failed to generate debian changelog from git log"
        return 1
    fi

    if ! copy_files $project_version; then
        err "failed to copy required resources"
        return 1
    fi

    if ! create_package $project_version; then
        err "failed to create debian package"
        return 1
    fi

    # let's push to debian repository
    if ! upload_to_repository rug-cli_${project_version}_all.deb; then
        err "failed to upload package to repository"
        return 1
    fi

    echo "created and upload $project_version"
}

main "$@" || exit 1
exit 0
