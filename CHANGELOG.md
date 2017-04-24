# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

[Unreleased]: https://github.com/atomist/rug-cli/compare/0.33.4...HEAD

## [0.33.4] - 2017-04-24

[0.33.4]: https://github.com/atomist/rug-cli/compare/0.33.3...0.33.4

### Added

-	New flag `-n` to get more progress updates. Now `-V` can be used to 
	dump the contents of `ArtifactSource` and the compiled JavaScript code
	if needed

## [0.33.3] - 2017-04-23

[0.33.3]: https://github.com/atomist/rug-cli/compare/0.33.2...0.33.3

### Fixed

-	Typo in usage of `login` command
- 	Update `rug.yml` gesture to reflect new group of `rug-editor

###	Changed

-	Don't attempt to load a logger if logback isn't around

## [0.33.2] - 2017-04-22

[0.33.2]: https://github.com/atomist/rug-cli/compare/0.33.1...0.33.2

### Fixed

-	ClassLoader error with writing settings

## [0.33.1] - 2017-04-22

[0.33.1]: https://github.com/atomist/rug-cli/compare/0.33.0...0.33.1

### Fixed

-	Compiler step wasn't printing output in normal mode

[0.33.1]: https://github.com/atomist/rug-cli/compare/0.33.0...0.33.1

## [0.33.0] - 2017-04-22

[0.33.0]: https://github.com/atomist/rug-cli/compare/0.32.0...0.33.0

### Changed

- 	Upgrade to Rug 0.26.0
-	**BREAKING** Changed some command and option names as per 
	https://github.com/atomist/rug-cli/issues/172
-	**BREAKING** Signature verification for all extensions specified in
	`manifest.yml` as per https://github.com/atomist/rug-resolver/issues/26 and 
	https://github.com/atomist/rug-cli/issues/171  
-	**BREAKING** Removed support for `ProjectReviewer`s from all the 
	`describe` commands.
-	Made console output less verbose. To get more information use `-V`
	and `-X	

## [0.32.0] - 2017-04-14

[0.32.0]: https://github.com/atomist/rug-cli/compare/0.31.0...0.32.0

### Added

- 	Ability to describe `RugFunction`s via `describe function <name>

## [0.31.0] - 2017-04-13

[0.31.0]: https://github.com/atomist/rug-cli/compare/0.30.2...0.31.0

### Fixed

- 	Formatting issues for `console.log`
-	`MappedParameters` show up in `describe` invoke output

### Changed

- 	Upgrade to Rug 0.25.3

## [0.30.2] - 2017-04-11

[0.30.2]: https://github.com/atomist/rug-cli/compare/0.30.1...0.30.2

### Fixed

- 	Make sure `console.log` works from `test` command

## [0.30.1] - 2017-04-11

[0.30.1]: https://github.com/atomist/rug-cli/compare/0.30.0...0.30.1

### Changed

- 	Upgraded to Rug 0.25.1

## [0.30.0] - 2017-04-11

[0.30.0]: https://github.com/atomist/rug-cli/compare/0.29.0...0.30.0

### Added

- 	New `dependencies` command to print dependency tree of Rugs an Functions as per
	https://github.com/atomist/rug-cli/issues/165

### Changed

- 	Upgraded to Rug 0.25.0

## [0.29.0] - 2017-03-31

[0.29.0]: https://github.com/atomist/rug-cli/compare/0.28.0...0.29.0

### Changed

-	Add `enable_compiler_cache` to `cli.yml` configuration section. Defaults
	to `true`. Use `false` to disable compiler caching
- 	Upgraded to Rug 0.22.0
- 	Fixed tab completion for mapped parameters as per
	https://github.com/atomist/rug-cli/issues/161
	
## [0.28.0] - 2017-03-26	

[0.28.0]: https://github.com/atomist/rug-cli/compare/0.27.1...0.28.0

### Added

- 	Rugs can now be excluded from generated metadata and thereform made
	invisible to the bot by puttting `excludes` section into `manifest.yml`
	as per https://github.com/atomist/rug-cli/issues/160

## [0.27.1] - 2017-03-24

[0.27.1]: https://github.com/atomist/rug-cli/compare/0.27.0...0.27.1

### Changed

- 	Upgraded to Rug 0.17.3  


## [0.27.0] - 2017-03-21

[0.27.0]: https://github.com/atomist/rug-cli/compare/0.26.1...0.27.0

### Changed

-   Don't compile TS before running `clean` command
- 	Renamed `--type` to `--kind` to be more in-line with the rest of the naming
- 	Allow `publish --id` to also allow the repo name as identifier  

## [0.26.1] - 2017-03-20

[0.26.1]: https://github.com/atomist/rug-cli/compare/0.26.0...0.26.1

### Changed

-   Upgraded to Rug 0.17.0 and rug-resolver

## [0.26.0] - 2017-03-20

[0.26.0]: https://github.com/atomist/rug-cli/compare/0.25.0...0.26.0

### Changed

-   Upgraded to Rug 0.16.0 and rug-resolver
-   Upgraded to Jline 3.2.0
- 	`cli.yml` now uses keys with `_` instead of `-

## [0.25.0] - 2017-03-10

[0.25.0]: https://github.com/atomist/rug-cli/compare/0.24.0...0.25.0

### Added

-   Support for describing handlers
-   Generate Markdown documentation for CLI
- 	Support for CLI gestures as per https://github.com/atomist/rug-cli/issues/155
-	 	Add `--requires` option to overwrite the Rug version of the archive as per
		https://github.com/atomist/rug-cli/issues/152
- 	Automatically reload the shell on changes to underlying files as per
		https://github.com/atomist/rug-cli/issues/142
- 	Search lists matching operations with `--operations` as per
		https://github.com/atomist/rug-cli/issues/100

### Changed

-   Removed executors
-   Upgraded to rug 0.13.+ and rug-resolver

## [0.24.0] - 2017-02-20

[0.24.0]: https://github.com/atomist/rug-cli/compare/0.23.0...0.24.0

### Added

-   New `repositories` command to login and provision your team-scoped
	Rug archive repositories.

-   Lots of enhancement on the `shell`.

	-	`shell` can now be started from any directory as per https://github.com/atomist/rug-cli/issues/112

	- 	Tab completion for archive, operation and parameter names in the `shell`.

	- 	Ability to execute any command by escaping the `shell` with `/sh`. Use `!` for
		event expansion.

-	Added short aliases for commands. Inspect the command help for a list
	of aliases.

- 	Ability to run the CLI from a docker image as per
	https://github.com/atomist/rug-cli/issues/115

### Changed

- 	Merged PR contributed by @janekdb cleaning up some code as per
	https://github.com/atomist/rug-cli/pull/107

## [0.23.0] - 2017-02-14

[0.23.0]: https://github.com/atomist/rug-cli/compare/0.22.0...0.23.0

### Added

-   New `tree` command to evaluate path expressions against a project as per
 	https://github.com/atomist/rug-cli/issues/96

-   New `shell` command to step into a repl session within the scope of the
    selected Rug archive.

## [0.22.0] - 2017-02-02

[0.22.0]: https://github.com/atomist/rug-cli/compare/0.21.4...0.22.0

### Changed

-   Updates to support Rug 0.11.0

-   Correctly removed `project_name` parameter as per
	https://github.com/atomist/rug-cli/issues/85

-   Add support for Rug log entries as per
	https://github.com/atomist/rug-cli/issues/87

-   `describe` command now allows `-O` to specify output format
	https://github.com/atomist/rug-cli/issues/86

## [0.21.4] - 2017-01-27

[0.21.4]: https://github.com/atomist/rug-cli/compare/0.21.3...0.21.4

### Fixed

-   Fix leading `.` in operation names of metadata.json
    https://github.com/atomist/rug-cli/issues/88

## [0.21.3] - 2017-01-25

[0.21.3]: https://github.com/atomist/rug-cli/compare/0.21.2...0.21.3

### Changed

-   Allow `.` (period) in archive group ids as per
    https://github.com/atomist/rug-cli/issues/84

-   Styled output of `rug help` and `rug -v` a bit as per
    https://github.com/atomist/rug-cli/issues/83

## [0.21.2] - 2017-01-23

[0.21.2]: https://github.com/atomist/rug-cli/compare/0.21.1...0.21.2

### Changed

-   Fixed NPE when running `rug install` and rug archive is not in a git repo

### Changed

-   Suggesting Java 8 as a dependency on Debian/Ubuntu (#70)
-   Add Ubuntu trusty as a supported target (#70)

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
