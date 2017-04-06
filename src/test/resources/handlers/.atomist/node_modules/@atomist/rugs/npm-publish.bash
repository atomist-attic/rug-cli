#!/bin/bash

set -o pipefail

declare Pkg=npm-publish
declare Version=0.1.0

function msg() {
    echo "$Pkg: $*"
}

function err() {
    msg "$*" 1>&2
}

function main() {
    local module_version=$1
    if [[ ! $module_version ]]; then
        err "first parameter must be the version number of the module to publish"
        return 10
    fi

    local package="package.json"
    local tmp_package="$package.tmp"
    if ! jq --arg tag "$TRAVIS_TAG" '.version = $tag' "$package" > "$tmp_package"; then
        err "failed to set version in $package"
        return 1
    fi
    cp "$tmp_package" "$package"
    if [[ $NPM_TOKEN ]]; then
        msg "Creating local .npmrc using API key from environment"
        if ! ( umask 077 && echo "//registry.npmjs.org/:_authToken=$NPM_TOKEN" > "$HOME/.npmrc" ); then
            err "failed to create $HOME/.npmrc"
            return 1
        fi
    else
        msg "assuming your .npmrc is setup correctly for this project"
    fi

    # npm honors this
    rm -f "$target/.gitignore"

    if ! ( cd "$target" && npm publish --access=public ); then
        err "failed to publish node module"
        cat "$target/npm-debug.log"
        return 1
    fi
}

main "$@" || exit 1
exit 0
