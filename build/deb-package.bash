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
    local version=$2

    echo "rug-cli ($version) unstable; urgency=low"
    echo
    git --no-pager log --format="  * %s" $previous${previous:+..}$version
    echo
    git --no-pager log --format=" -- $Author  %aD" -n 1 $version
    echo
}

function git2deb_changelog() {
    local project_version=$1

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
    if [ "${tags}" = "" ]; then
        # no tags there yet, so let's pretend
        echo "No tags found"
        gitentry2debentry "" "" > target/linux/deb/changelog
        sed -i "s/()/($project_version)/" target/linux/deb/changelog
    else
        echo "$tags" | {
            read version; while read previous; do
                    gitentry2debentry $previous $version
                    version="$previous"
            done
            gitentry2debentry "" $version
        } > target/linux/deb/changelog
    fi

    LANG=$current_lang

    return 0
}

function create_directory_structure() {
    # this directory structure maps how we the resources will
    # be deployed on target machines
    echo "Creating directory structure..."
    rm -rf target/linux/deb
    mkdir -p target/linux/deb/rug-cli
    mkdir -p target/linux/deb/rug-cli/usr/share/bash-completion/completions
    mkdir -p target/linux/deb/rug-cli/usr/share/doc/rug-cli
    mkdir -p target/linux/deb/rug-cli/usr/share/rug/bin
    mkdir -p target/linux/deb/rug-cli/usr/share/rug/lib

    return $?
}

function copy_files() {
    local project_version=$1
    local source_dir="target/rug-cli-$project_version-bin/rug-cli-$project_version"
    local dest_dir="target/linux/deb/rug-cli/usr/share"

    echo "ln -s /usr/share/rug/bin/rug /usr/bin/rug" > postinst
    echo "rm /usr/bin/rug" > preuninst
    cp build/debian/copyright $dest_dir/doc/rug-cli/copyright
    cp $source_dir/bin/rug $dest_dir/rug/bin/rug
    cp $source_dir/lib/rug-cli-$project_version.jar $dest_dir/rug/lib/rug-cli-$project_version.jar
    cp $source_dir/etc/bash_completion.d/rug $dest_dir/bash-completion/completions/rug

    return 0
}

function upload_to_repository() {
    echo "Uploading to repository..."

    local package=$1
    local debian_repo_url="https://atomist.jfrog.io/atomist/debian/pool"
    local distributions="deb.distribution=wheezy;deb.distribution=jessie;deb.distribution=yakkety;deb.distribution=xenial;deb.distribution=wily;deb.distribution=vivid"
    local components="deb.component=main"
    local arch="deb.architecture=all"
    local checksum=$(sha1sum $package | awk '{ print $1 }')

    echo "Package checksum: $checksum"

    local response=$(curl -s -u $ATOMIST_CI_DEPLOY_USERNAME:$ATOMIST_CI_DEPLOY_PASSWORD \
                          -H "X-Checksum-Sha1:$checksum"\
                          -T $package\
                          -XPUT \
                          "$debian_repo_url/$package;$distributions;$components;$arch")

    if [[ $(echo $response | jq '.errors? | length') -ne 0 ]]; then
        err $response
        return 1
    fi
    
    return 0
}

function create_package() {
    local project_version=$1

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

    return $?
}

function main() {
    if [[ $TRAVIS ]]; then
        install_deps
    fi

    if [[ ! $ATOMIST_CI_DEPLOY_USERNAME ]]; then
        err "missing artifactory user ATOMIST_CI_DEPLOY_USERNAME"
        return 1
    fi

    if [[ ! $ATOMIST_CI_DEPLOY_PASSWORD ]]; then
        err "missing artifactory password ATOMIST_CI_DEPLOY_PASSWORD"
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

    copy_files $project_version

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

    return 0
}

main "$@" || exit 1
exit 0
