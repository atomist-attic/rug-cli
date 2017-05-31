#!/bin/bash

set -o pipefail
declare Pkg="update-docs"
declare Version="0.1.0"

function msg() {
    echo "$Pkg: $*"
}

function err() {
    msg "$*" 1>&2
}

# update PATH with current reference documentation in SLUG using
# GitHub repo TOKEN.  SLUG should be something like "owner/repo",
# default is "atomist/end-user-documentation".  The default value of
# PATH is "docs/reference/rug-cli/index.md".
# usage: main TOKEN [SLUG [PATH]]
function main() {
    local token=$1
    if [[ ! $token ]]; then
        err "missing required argument: TOKEN"
        return 10
    fi
    shift
    local slug=$1
    if [[ ! $tap ]]; then
        slug=atomist/end-user-documentation
    else
        shift
    fi
    local doc_path=$1
    if [[ ! $doc_path ]]; then
        doc_path=docs/reference/rug-cli/index.md
    else
        shift
    fi

    msg "cloning $slug"
    local repo=https://$token:x-oauth-basic@github.com/$slug.git
    local repo_dir=target/${repo##*/}
    if ! git clone --quiet "$repo" "$repo_dir" > /dev/null 2>&1; then
        err "failed to clone doc repo into $repo_dir"
        return 1
    fi

    msg "generating reference documentation"
    local full_doc_path=$repo_dir/$doc_path
    if ! java -cp target/rug-cli-*-bin/rug-cli-*/lib/rug-cli-*.jar com.atomist.rug.cli.command.MkDocs > "$full_doc_path"
    then
        err "failed to generated reference documentation"
        return 1
    fi

    if ! cd "$repo_dir"; then
        err "failed to change into $repo_dir directory"
        return 1
    fi
    local changes
    changes=$(git ls-files --modified --other --exclude-standard)
    if [[ $? -ne 0 ]]; then
        err "failed to determine changed files after generating documentation"
        return 1
    fi
    local is_changed
    is_changed=$(echo "$changes" | grep "^$doc_path\$")
    if [[ ! $is_changed ]]; then
        msg "reference documentation is unchanged from current version"
        cd -; rm -rf "$repo_dir"
        return 0
    fi
    if ! git config user.email "travis-ci@atomist.com"; then
        err "failed to set git user email"
        return 1
    fi
    if ! git config user.name "Travis CI"; then
        err "failed to set git user name"
        return 1
    fi
    if ! git add "$doc_path"; then
        err "failed to add new doc"
        return 1
    fi
    msg "committing updated reference documentation"
    if ! git commit -m "Update Rug CLI reference doc to version $TRAVIS_TAG"; then
        err "failed to commit new documentation"
        return 1
    fi
    if ! git push --quiet origin master > /dev/null 2>&1; then
        err "failed to push changes to origin"
        return 1
    fi

    cd -;rm -rf "$repo_dir"
}

main "$@" || exit 1
exit 0
