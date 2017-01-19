# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

[Unreleased]: https://github.com/atomist/rug-cli/compare/0.21.1...HEAD

## [0.21.1] - 2017-01-19

[0.21.1]: https://github.com/atomist/rug-cli/compare/0.21.0...0.21.1

### Changed

-   Add search filter for operation type, `rug search --type editor`

-   Parameter default values added to output of `rug describe`

## [0.21.0] - 2017-01-18

[0.21.0]: https://github.com/atomist/rug-cli/compare/0.20.0...0.21.0

### Changed

-   Update to Rug 0.10.0

-   Allow overwriting artifact group, artifact and version on `publish`

-   Introduce `--verbose -V` to get more verbose output

-   Better error messages on certain errors

## [0.20.0] - 2017-01-08

[0.20.0]: https://github.com/atomist/rug-cli/compare/0.19.1...0.20.0

### Changed

-   Interactive mode `--interactive` or `-I` for entering parameters for edit and generate

-   New `search` command to allow searching our online catalog of Rugs

-   Write ArtifactSource content to the console when running with `-X`to allow debugging for filters

-   Print remote repository URL on publish

## [0.19.1] - 2017-01-04

[0.19.1]: https://github.com/atomist/rug-cli/compare/0.19.0...0.19.1

### Changed

-   Fix caching of latest version resolutions

## [0.19.0] - 2017-01-04

[0.19.0]: https://github.com/atomist/rug-cli/compare/0.18.1...0.19.0

### Changed

-   Update for rug 0.8.0

-   CLI now compiles TypeScript into the resulting archive for `install` and `publish`

-   Removed support for reading archive metadate from `package.json`

-   Update to Java 8 thanks to @cchacin
