#!/bin/bash

# Build a chocolatey package to install the CLI on Windows hosts

set -o pipefail
declare Pkg="chocolatey-package"
declare Version="0.1.0"
declare Author="Atomist, Inc <oss@atomist.com>"

function err() {
    echo "$Pkg: $*" 1>&2
}

function install_deps() {
    sudo apt-get install -y -q zip
}

function create_directory_structure() {
    echo "Creating directory structure..."
    rm -rf target/windows/chocolatey
    mkdir -p target/windows/chocolatey
}

function copy_files() {
    local project_version=$1
    if [[ ! $project_version ]]; then
        err "missing required argument: project_version"
        return 10
    fi

    local source_dir="target/rug-cli-$project_version-bin/rug-cli-$project_version"
    local dest_dir="target/windows/chocolatey"

    if ! cp -r src/main/scripts/chocolatey/* $dest_dir; then
        err "Failed to copy chocolatey files"
        return 1
    fi
}

function update_resources() {
    local project_version=$1
    if [[ ! $project_version ]]; then
        err "missing required argument: project_version"
        return 10
    fi
    shift

    local zippkg=$1
    if [[ ! $zippkg ]]; then
        err "missing required argument: zippkg"
        return 10
    fi

    local dest_dir="target/windows/chocolatey"
    local checksum=$(sha256sum $zippkg | awk '{ print $1 }')

    if ! sed -i "s/__VERSION__/$project_version/g" $dest_dir/rug-cli.nuspec; then
        err "failed to set version in rug-cli.nuspec"
        return 1
    fi

    if ! sed -i "s/__VERSION__/$project_version/g" $dest_dir/package/services/metadata/core-properties/3ba78ce3f3be4369bde7c8c4b4dee032.psmdcp; then
        err "failed to set version in psmdcp file"
        return 1
    fi

    if ! sed -i "s/__VERSION__/$project_version/g" $dest_dir/tools/chocolateyinstall.ps1; then
        err "failed to set version in chocolateyinstall.ps1"
        return 1
    fi

    if ! sed -i "s/__CHECKSUM__/$checksum/g" $dest_dir/tools/chocolateyinstall.ps1; then
        err "failed to set checksum in chocolateyinstall.ps1"
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

    local nuget_repo_url="https://atomist.jfrog.io/atomist/api/nuget/nuget/rug-cli"
    local checksum=$(sha1sum $package | awk '{ print $1 }')

    echo "Package checksum: $checksum"

    local response=$(curl -s -u $ARTIFACTORY_USER:$ARTIFACTORY_PASSWORD \
                          -H "X-Checksum-Sha1:$checksum" \
                          -F package=@$package \
                          -XPUT \
                          $nuget_repo_url)

    # this will emit a warning message on successful uploads because
    # the server respones with plain text in that case
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

    # generate the chocolatey package
    echo "Creating package..."
    cd target/windows/chocolatey
    if ! zip -r rug-cli.$project_version.nupkg .; then
        err "failed to create chocolatey package"
        return 1
    fi
    cd - 
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

    if ! copy_files $project_version; then
        err "failed to copy required resources"
        return 1
    fi

    if ! update_resources $project_version target/rug-cli-${project_version}-bin.zip; then
        err "failed updating chocolatey resources"
        return 1
    fi

    if ! create_package $project_version; then
        err "failed to create chocolatey package"
        return 1
    fi

    # let's push to the nuget repository
    if ! upload_to_repository "target/windows/chocolatey/rug-cli.$project_version.nupkg"; then
        err "failed to upload package to repository"
        return 1
    fi

    echo "created and uploaded rug-cli.$project_version.nupkg"
}

main "$@" || exit 1
exit 0
