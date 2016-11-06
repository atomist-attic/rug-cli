# Rug CLI Commands and Syntax

This page documents syntax and functionality of the Rug CLI.

## Installing

The Rug CLI should be installable via Homebrew with support for Windows and Linux
native installer formats soon after.

```
brew tap atomisthq/tap
brew install rug-cli
```

Right now Windows and Linux are supported via zip and tar.gz archives.

### Post-Install: Repository Settings

In order to use the CLI the following file named `cli.yml` needs to be placed
in `~/.atomist` ensuring you provide your own credentials where indicated.
The CLI will install a default `cli.yml` if none is found.

```
# Set up the path to the local repository
local-repository:
  path: ${HOME}/.atomist/repository

# Set up remote repositories to query for Rug archives. Additionally one of the
# repositories can also be enabled for publication (publish: true).
remote-repositories:
  public-templates:
    publish: false
    url: https://sforzando.artifactoryonline.com/sforzando/public-templates-dev
    authentication:
      username: <username>
      password: <password>
  private-templates:
    publish: true
    url: https://sforzando.artifactoryonline.com/sforzando/private-templates-dev
    authentication:
      username: <username>
      password: <password>
  rug-types:
    publish: false
    url: https://sforzando.artifactoryonline.com/sforzando/rug-deps-dev
    authentication:
      username: <username>
      password: <password>
```

## Commands

The CLI will assume the current working directory to be the root for execution.

### Using the CLI as Rug users

#### Invoking Editors

Run an editor as follows:

```
rug edit atomist:common-editors:AddReadme --artifact-version 1.0.0 parameter1=foo
  parameter2=bar

rug edit atomist:common-editors:AddReadme parameter1=foo parameter2=bar
```

`artifact-version` is optional and defaults to `latest` semantics.
`change-dir` or `-C` for giving a generator a target directory.

#### Invoking Generators

```
rug generate "atomist-project-templates:spring-rest-service:Spring Boot Microservice"
  --artifact-version 1.0.0 MyNewProjectName parameter1=foo parameter2=bar

rug generate "atomist-project-templates:spring-rest-service:Spring Boot Microservice"
  MyNew Project parameter1=foo parameter2=bar
```

`artifact-version` is optional and defaults to `latest` semantics.
`change-dir` or `-C` for giving a generator a target directory.

### Describing Rug Artifacts

In order to list all parameters, describing an artifact is available in the
following form:

```
rug describe archive atomist-project-templates:spring-rest-service

rug describe editor "atomist-project-templates:spring-rest-service:Spring Boot Microservice"
  --artifact-version 1.0.0

rug describe generator "atomist-project-templates:spring-rest-service:Spring Boot Microservice"
  --artifact-version 1.0.0
```

#### Listing Local Archives

To list all locally available Rug archives, the following command can be used:

```
rug list -f version="[1.2,2.0)" -f group=*atomist* -f artifact=*sp?ing*
```

The local listing can be filtered by using `-f` filter expressions on `group`,
`artifact` and `version`. `group` and `artifact` support wildcards of `*` and `?`.
`version` takes any version constraint.

#### Searching for Archives

(later)

We need allow users to search for specific Generators and Editors by search terms
like tags, free text etc.

### Using the CLI as Rug developer

All the following commands need to executed from within the Rug project directory.

#### Running Tests

Run tests as follows:

```
# Running all tests
rug test

# Running a named test
rug test "Whatever Test Secanrio"

# Running all scenarios from a .rt file
rug test MyRugTestFilename
```

#### Installing a Rug archive

Creating a Rug zip archive and installing it into the local repository can be done with
the following command:

```
rug install
```

This command packages the project into a zip archive, creates a Pom and installs
both into the local repository under `.atomist/repository`.

#### Publishing a Rug archive

In order to make the archive available to others, it can be published to our
`private-templates-dev` repository:

```
rug publish --artifact-version 1.2.1
```
`--artifact-version` can be used to overwrite the `version:` value from the `manifest.ym`

## Dependency Resolution

The Rug CLI will automatically resolve and download the dependencies of the given
Rug archive when `edit` or `generate` is invoked. The archives along with their
dependencies will be downloaded to a local repo (not the one in the user's
home directory) via Aether and resolved from there.

Therefore running above commands is a two step process:
  1. Search and resolve (eventually download) the archive referenced in the command. The result of a resolution is cached for 60mins
     or until the `manifest.yml` is changed
  2. Start up `rug-lib` passing parameters over to run the editor or generator

## Turning on Verbose output for Debugging

If you want a more verbose output that includes any exceptions that Rug command
may have encountered, please add `-X` to your command.

For example:

```
rug test -X
```
