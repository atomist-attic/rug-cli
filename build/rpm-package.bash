#!/bin/bash

# Build a RPM package using the fpm builder so 
# we don't have to worry about the internals of the rpm
# packaging machinery

set -o pipefail
declare Pkg="rpm-package"
declare Version="0.1.0"
declare Author="Atomist, Inc <opensource@atomist.com>"

function err() {
    echo "$Pkg: $*" 1>&2
}

function install_deps() {
    apt-get install -y -q ruby-dev build-essential jq curl rpm
    gem install fpm
}

function gitentry2rpmentry() {
    local previous=$1
    local version=$2

    git --no-pager log --date="format:%a %b %d %Y" --format="* %ad $Author $version" -n 1 $version
    git --no-pager log --format="- %s" $previous${previous:+..}$version
    echo
}

function git2rpm_changelog() {
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
        gitentry2rpmentry "" "" > target/linux/rpm/changelog
        sed -i "s/()/($project_version)/" target/linux/rpm/changelog
    else
        echo "$tags" | {
            read version; while read previous; do
                    gitentry2rpmentry $previous $version
                    version="$previous"
            done
            gitentry2rpmentry "" $version
        } > target/linux/rpm/changelog
    fi

    LANG=$current_lang
    
    return 0
}

function create_directory_structure() {
    # this directory structure maps how we the resources will
    # be deployed on target machines
    echo "Creating directory structure..."
    rm -rf target/linux/rpm/
    mkdir -p target/linux/rpm/rug-cli
    mkdir -p target/linux/rpm/rug-cli/bash-completion/completions
    mkdir -p target/linux/rpm/rug-cli/doc/rug-cli
    mkdir -p target/linux/rpm/rug-cli/rug/bin
    mkdir -p target/linux/rpm/rug-cli/rug/lib

    return $?
}

function copy_files() {
    local project_version=$1
    local source_dir="target/rug-cli-$project_version-bin/rug-cli-$project_version"
    local dest_dir="target/linux/rpm/rug-cli"

    echo "ln -s /usr/share/rug/bin/rug /usr/bin/rug" > postinst
    echo "rm /usr/bin/rug" > preuninst
    cp $source_dir/bin/rug $dest_dir/rug/bin/rug
    cp $source_dir/lib/rug-cli-$project_version.jar $dest_dir/rug/lib/rug-cli-$project_version.jar
    cp $source_dir/etc/bash_completion.d/rug $dest_dir/bash-completion/completions/rug

    return 0
}

function upload_to_repository() {
    echo "Uploading to repository..."

    local package=$1
    local yum_repo_url="https://atomist.jfrog.io/atomist/yum"
    local checksum=$(sha1sum $package | awk '{ print $1 }')

    echo "Package checksum: $checksum"

    local response=$(curl -s -u $ATOMIST_CI_DEPLOY_USERNAME:$ATOMIST_CI_DEPLOY_PASSWORD \
                          -H "X-Checksum-Sha1:$checksum"\
                          -T $package\
                          -XPUT \
                          "$yum_repo_url/$package")

    if [[ $(echo $response | jq '.errors? | length') -ne 0 ]]; then
        err $response
        return 1
    fi
    
    return 0
}

function create_package() {
    local project_version=$1

    # generate the RPM package
    echo "Creating package..."
    fpm -s dir \
        -t rpm \
        -a all \
        -n rug-cli \
        -v $project_version \
        -m "$Author" \
        --prefix "/usr/share" \
        --description "Atomist rug CLI" \
        --url https://www.atomist.com \
        --license "GPLv3" \
        --vendor "Atomist, Inc" \
        -d "java-1.8.0-openjdk-devel" \
        --directories "/usr/share/rug" \
        --rpm-changelog target/linux/rpm/changelog \
        --rpm-os linux \
        --after-install=postinst \
        --pre-uninstall=preuninst \
        -C target/linux/rpm/rug-cli

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
    if ! git2rpm_changelog $project_version; then
        err "failed to generate rpm changelog from git log"
        return 1
    fi

    copy_files $project_version

    if ! create_package $project_version; then
        err "failed to create rpm package"
        return 1
    fi

    # let's push to rpm repository
    local rpm_version=${project_version/-/_}
    if ! upload_to_repository rug-cli-${rpm_version}-1.noarch.rpm; then
        err "failed to upload package to repository"
        return 1
    fi

    echo "created and upload $project_version"

    return 0
}

main "$@" || exit 1
exit 0
