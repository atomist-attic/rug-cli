#!/bin/bash
# test and publish rug archive

set -o pipefail

declare Pkg=travis-build
declare Version=0.3.0

function msg() {
    echo "$Pkg: $*"
}

function err() {
    msg "$*" 1>&2
}

# usage: main "$@"
function main () {
    local formula_url=https://raw.githubusercontent.com/atomist/homebrew-tap/master/Formula/rug-cli.rb
    local formula
    formula=$(curl -s -f "$formula_url")
    if [[ $? -ne 0 || ! $formula ]]; then
        err "failed to download homebrew formula $formula_url: $formula"
        return 1
    fi

    local version
    version=$(echo "$formula" | sed -n '/^ *url /s,.*/\([0-9]*\.[0-9]*\.[0-9]*\)/.*,\1,p')
    if [[ $? -ne 0 || ! $version ]]; then
        err "failed to parse brew formula for version: $version"
        err "$formula"
        return 1
    fi
    msg "rug CLI version: $version"

    local rug=$HOME/.atomist/rug-cli-$version/bin/rug
    if [[ ! -x $rug ]]; then
        msg "downloading rug CLI"
        if ! mkdir -p "$HOME/.atomist"; then
            err "failed to make ~/.atomist directory"
            return 1
        fi

        local rug_cli_url=https://github.com/atomist/rug-cli/releases/download/$version/rug-cli-$version-bin.tar.gz
        local rug_cli_tgz=$HOME/.atomist/rug-cli-$version.tar.gz
        if ! curl -s -f -L -o "$rug_cli_tgz" "$rug_cli_url"; then
            err "failed to download rug CLI from $rug_cli_url"
            return 1
        fi

        if ! tar -xzf "$rug_cli_tgz" -C "$HOME/.atomist"; then
            err "failed to extract rug CLI archive"
            return 1
        fi
    fi
    rug="$rug -qurX"

    local build_dir=.atomist/build
    local cli_user=$HOME/.atomist/cli.yml
    if ! install --mode=0600 "$build_dir/cli-build.yml" "$cli_user"; then
        err "failed to install build cli.yml"
        return 1
    fi
    trap "rm -f $cli_user" RETURN

    if [[ -f .atomist/package.json ]]; then
        msg "running npm install"
        if ! ( cd .atomist && npm install ); then
            err "npm install failed"
            return 1
        fi
    fi

    msg "running tests"
    if ! $rug test; then
        err "rug test failed"
        return 1
    fi

    msg "installing archive"
    if ! $rug install; then
        err "rug install failed"
        return 1
    fi

    [[ $TRAVIS_PULL_REQUEST == false ]] || return 0

    local archive_version
    local manifest=.atomist/manifest.yml package=.atomist/package.json
    if [[ -f $manifest ]]; then
        archive_version=$(awk -F: '$1 == "version" { print $2 }' "$manifest" | sed 's/[^.0-9]//g')
    elif [[ -f $package ]]; then
        archive_version=$(jq --raw-output --exit-status .version "$package")
    else
        err "no manifest.yml or package.json in archive"
        return 1
    fi
    if [[ $? -ne 0 || ! $archive_version ]]; then
        err "failed to extract archive version: $archive_version"
        return 1
    fi
    local project_version cli_yml
    if [[ $TRAVIS_TAG =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        if [[ $archive_version != $TRAVIS_TAG ]]; then
            err "archive version ($archive_version) does not match git tag ($TRAVIS_TAG)"
            return 1
        fi
        project_version=$TRAVIS_TAG
        cli_yml=$build_dir/cli-release.yml
    else
        local timestamp
        timestamp=$(date +%Y%m%d%H%M%S)
        if [[ $? -ne 0 || ! $timestamp ]]; then
            err "failed to generate timestamp: $timestamp"
            return 1
        fi
        project_version=$archive_version-$timestamp
        cli_yml=$build_dir/cli-dev.yml
    fi
    msg "branch: $TRAVIS_BRANCH"
    msg "archive version: $project_version"

    if [[ $TRAVIS_BRANCH == master || $TRAVIS_TAG =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        if ! install --mode=0600 "$cli_yml" "$cli_user"; then
            err "failed to install $cli_yml"
            return 1
        fi

        msg "publishing archive"
        if ! $rug publish -a "$project_version"; then
            err "failed to publish archive $project_version"
            return 1
        fi
        if ! git config --global user.email "travis-ci@atomist.com"; then
            err "failed to set git user email"
            return 1
        fi
        if ! git config --global user.name "Travis CI"; then
            err "failed to set git user name"
            return 1
        fi
        local git_tag=$project_version+travis$TRAVIS_BUILD_NUMBER
        if ! git tag "$git_tag" -m "Generated tag from TravisCI build $TRAVIS_BUILD_NUMBER"; then
            err "failed to create git tag: $git_tag"
            return 1
        fi
        if ! git push --quiet --tags "https://$GITHUB_TOKEN@github.com/$TRAVIS_REPO_SLUG" > /dev/null 2>&1; then
            err "failed to push git tags"
            return 1
        fi
    fi
}

main "$@" || exit 1
exit 0
