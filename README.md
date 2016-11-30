# Atomist 'rug-cli'

[![Build Status](https://travis-ci.com/atomist/rug-cli.svg?token=YuitiySbpCXZTEZXx1ss&branch=master)](https://travis-ci.com/atomist/rug-cli)
[![Slack Status](https://join.atomist.com/badge.svg)](https://join.atomist.com/)

Atomist Rug command-line interface for creating and running rugs.

## Installation

You can install the Rug command-line interface using standard
packaging tools for your operating system.

* [CLI Installation for MacOS](https://github.com/atomist/homebrew-tap)
* [CLI Installation for Linux](https://github.com/atomist/rug-cli/blob/master/docs/install-linux.md)
* [CLI Intallation for Windows (Work in progress)](https://github.com/atomist/rug-cli/issues/6)

## Documentation

The following documentation is available:

* [CLI Commands and Syntax](https://github.com/atomist/rug-cli/blob/master/docs/rug-cli.md)
* [Documentation on Rug Archives](https://github.com/atomist/rug-cli/blob/master/docs/rug-archives.md)

## Support

General support questions should be discussed in the `#rug-cli`
channel on our community slack team
at [atomist-community.slack.com](https://join.atomist.com).

If you find a problem, please create an [issue][].

[issue]: https://github.com/atomist/rug-cli/issues

## Development

You can build, test, and install the project locally with [maven][].

[maven]: https://maven.apache.org/

```sh
$ mvn install
```

To create a new release of the project, simply push a tag of the form
`M.N.P` where `M`, `N`, and `P` are integers that form the next
appropriate [semantic version][semver] for release.  For example:

```sh
$ git tag -a 1.2.3
```

The [Travis CI][travis] build will automatically create a GitHub
release using the tag name for the release and the comment provided on
the annotated tag as the contents of the release notes.  It will also
automatically upload the needed artifacts and update the binary
packages.

[semver]: http://semver.org
[travis]: https://travis-ci.com/atomist/rug-cli
