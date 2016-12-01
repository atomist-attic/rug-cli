#!/bin/bash

set -o pipefail
declare Pkg="update-brew"
declare Version="0.2.0"

function err() {
    echo "$Pkg: $*" 1>&2
}

# update formula in TAP/FORMULA_DIR using GitHub repo TOKEN.  TAP
# should be something like "owner/repo".  The default value of
# FORMULA_DIR is "Formula".
# usage: main TAP TOKEN [FORMULA_DIR]
function main() {
    local tap=$1
    if [[ ! $tap ]]; then
        err "missing required argument: TAP"
        return 10
    fi
    shift
    local token=$1
    if [[ ! $token ]]; then
        err "missing required argument: TOKEN"
        return 10
    fi
    shift
    local formula_dir=$1
    if [[ ! $formula_dir ]]; then
        formula_dir=Formula
    else
        shift
    fi

    local project=${PWD##*/}
    local formula=$project.rb
    local formula_path=target/$formula
    if [[ ! -f $formula_path ]]; then
        err "no formula found: $formula_path"
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

    local tap_repo=https://$token:x-oauth-basic@github.com/$tap.git
    local tap_dir=${tap##*/}
    if ! git clone --quiet "$tap_repo" "$tap_dir" > /dev/null 2>&1; then
        err "failed to clone tap repo into $tap_dir"
        return 1
    fi

    local target=$tap_dir/$formula_dir/$formula
    if ! cp "$formula_path" "$target"; then
        err "failed to copy $formula to $target"
        return 1
    fi
    if ! cd "$tap_dir"; then
        err "failed to change into $tap_dir directory"
        return 1
    fi
    if ! git add "$formula_dir/$formula"; then
        err "failed to add new formula"
        return 1
    fi
    if ! git commit -m "Update formula for $project"; then
        err "failed to commit new formula"
        return 1
    fi
    if ! git push --quiet origin master > /dev/null 2>&1; then
        err "failed to push changes to origin"
        return 1
    fi
}

main "$@" || exit 1
exit 0
