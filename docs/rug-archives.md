# A Day in the Life of a Rug Archive

This page describes the nature, structure and lifecycle of a Rug archive.

If you haven't done so and you want to follow along, please install the CLI from
[atomisthq/rug-cli](https://github.com/atomisthq/rug-cli/blob/master/README.md).

## Inception

The easiest way to create a new Rug Archive is by using the `Rug Archive` generator
via the bot or CLI. Here, we'll show how to do it with the CLI

```
git clone git@github.com:atomist-project-templates/rug-archive.git
cd rug-archive

rug generate "Rug Archive Project" -l my-new-template group_id=atomist-project-templates version=0.0.1 \
  description="My first Rug Archive project" editor_name=MyFirstEditor -C /Users/cdupuis/Desktop/generate
```

That command created a new `my-new-template` project at `/Users/cdupuis/Desktop/generate`.

## Structure

Each Rug Archive should have the following directory structure:

```
my-new-template
├── .atomist
│   ├── editors
│   │   └── MyFirstEditor.rug
│   ├── manifest.yml
│   ├── templates
│   │   └── MyFirstEditorTemplate.vm
│   └── tests
│       └── MyFirstEditor.rt
```

The editor `MyFirstEditor.rug` and corresponding test `MyFirstEditor.rt` have
been generated when running the generate command.

Let's take a look at the `manifest.yml`:

```
group: atomist-project-templates
artifact: my-new-template
version: "0.0.1"

requires: "[1.1.3,1.2)"

dependencies:
  - "atomist-project-templates:common-editors:2.5.0"

extensions:
  - "com.atomist:clj-rug:[1.2,1.3)"
```

The `manifest.yml` specifies the unique coordinates of the Rug Archive as well
as its version. Dependencies and extensions can also be declared

| Key | Description |
| --- | --- |
| `group` | The group of the Rug Archive. Should be the GitHub org |
| `artifact` | A unique identifier within the `group` |
| `version` | Version of the Rug Archive |
| `requires` | The rug-lib version this archive is being developed with. Version range is allowed|
| `dependencies` | List of archive dependencies in form group:artifact:version. Version ranges are allowed |
| `extensions` | List of binary dependencies to Rug Extension types etc. Version ranges are allowed |

## Running Tests

After making some changes to your Rug code, you should run the tests.

```
✘-1 ~/Desktop/generate/my-new-template [master|✚ 19…4]
15:44 $ rug test
Resolving dependencies for atomist-project-templates:my-new-template:0.0.1 completed
Loading atomist-project-templates:my-new-template:0.0.1 into runtime completed
Executing scenario MyFirstEditor should do something amazing for developers......              
   Testing assertion fileExists(IdentifierFunctionArg(sample_output_file,None))                
   Testing assertion fileContains(IdentifierFunctionArg(sample_output_file,None),IdentifierFunctionArg(my_value,None))
Running test scenarios in atomist-project-templates:my-new-template:0.0.1 completed
Test report: 1 of 1 tests passed

Test SUCCESS
```

## Installing

To package the Rug Archive up and make it available system-wide from your local
repository, run the following command:

```
✘-1 ~/Desktop/generate/my-new-template [master|✚ 19…4]
15:47 $ rug install
Resolving dependencies for atomist-project-templates:my-new-template:0.0.1 completed
Loading atomist-project-templates:my-new-template:0.0.1 into runtime completed
Installed atomist-project-templates/my-new-template/0.0.1/my-new-template-0.0.1.zip -> /Users/cdupuis/.atomist/repository
Installed atomist-project-templates/my-new-template/0.0.1/my-new-template-0.0.1.pom -> /Users/cdupuis/.atomist/repository
Installing archive into local repository completed
/Users/cdupuis/Desktop/generate/my-new-template/.atomist/target/my-new-template-0.0.1.zip
==> Contents
├─ .atomist
|  ├─ editors
|  |  └─ MyFirstEditor.rug
|  ├─ manifest.yml
|  ├─ templates
|  |  └─ MyFirstEditorTemplate.vm
|  └─ tests
|     └─ MyFirstEditor.rt
├─ .gitignore
├─ .provenance.txt
├─ META-INF/maven/atomist-project-templates/my-new-template
|  └─ pom.xml
└─ README.md
Successfully installed archive for atomist-project-templates:my-new-template:0.0.1
```

The `install` command takes the project, creates a Maven-compatible `pom.xml` in
`.atomist/target` as well as zip archive of your project.

The contents of the archive can be further tuned by adding entries to `.gitignore`
which the packaging step considers when packing the archive.

## Publishing

Publishing is the process of uploading an archive to `private-templates-dev` from
where it can only be used by users of a slack team that is enrolled to the sample
GitHub org the project was published from.

Open source CLI users will not have access to the `private-templates-dev`.

There are two way for publishing a Rug Archive:

### Publishing via the CLI (Atomist only)

Publishing into our Artifactory is only possible if you have write access to it.

```
✘-1 ~/Desktop/generate/my-new-template [master|✚ 19…4]
rug publish
Resolving dependencies for atomist-project-templates:my-new-template:0.0.1 completed
Loading atomist-project-templates:my-new-template:0.0.1 into runtime completed
Uploaded atomist-project-templates/my-new-template/0.0.1/my-new-template-0.0.1.zip -> https://sforzando.artifactoryonline.com/sforzando/private-templates-dev/ (3kb) succeeded
Uploaded atomist-project-templates/my-new-template/0.0.1/my-new-template-0.0.1.pom -> https://sforzando.artifactoryonline.com/sforzando/private-templates-dev/ (0kb) succeeded
Downloaded atomist-project-templates/my-new-template/maven-metadata.xml <- https://sforzando.artifactoryonline.com/sforzando/private-templates-dev/ (0kb) succeeded
Uploaded atomist-project-templates/my-new-template/maven-metadata.xml -> https://sforzando.artifactoryonline.com/sforzando/private-templates-dev/ (0kb) succeeded
Publishing archive into remote repository completed
```

Users of CLI can configure any Maven compatible repository for publishing into.
This can be configured in the CLI's config file at `~/.atomist/cli.yml`. There
should be only one `repository` marked with `publish: true`.

```
# Set up remote repositories to query for Rug archives. Additionally one of the
# repositories can also be enabled for publication (publish: true).
remote-repositories:
  maven-central:
    publish: false
    url: "http://repo.maven.apache.org/maven2/"
  public-templates:
    publish: false
    url: "https://sforzando.artifactoryonline.com/sforzando/public-templates-dev"
    authentication:
      username: "${CI_DEPLOY_USERNAME}"
      password: "${CI_DEPLOY_PASSWORD}"
  private-templates:
    publish: true
    url: "https://sforzando.artifactoryonline.com/sforzando/private-templates-dev"
    authentication:
      username: "${CI_DEPLOY_USERNAME}"
      password: "${CI_DEPLOY_PASSWORD}"
  rug-types:
    publish: false
    url: "https://sforzando.artifactoryonline.com/sforzando/rug-deps-dev"
    authentication:
      username: "${CI_DEPLOY_USERNAME}"
      password: "${CI_DEPLOY_PASSWORD}"
```

### Publishing via the Bot

The following bot command can be used to publish a Rug Archive straight out of
GitHub.

```
@atomist publish archive --repo my-new-template --version 1.0.0
```

The GitHub org will be inferred from the enrolled GitHub org of the user running
the command. `repo` is optional and defaults for the name of the service channel
the command is invoked from.

`version` can be used to overwrite the version specified in the `manifest.yml`.

### Publishing as part of a CI build

We provide template `.travis.yml` and scripts to run a CI build on your Rug archive.
The script installs the CLI, runs the Rug tests and installs and publishes the
archive.

## Releasing

Releasing describes the process of making a Rug Archive visible to users of other
orgs and teams. Generally speaking by releasing an Archive it becomes visible to
the rest of world.

Releasing is supported via the bot only.

```
@atomist release archive --repo my-new-template --version 1.0.0
```

This command will promote or copy the archive from `private-templates-dev` to
`public-templates-dev`. The GitHub org will be inferred from the enrolled GitHub
org of the user running the command. `repo` is optional and defaults for the
name of the service channel the command is invoked from.


_Note_

If you published from the CLI and/or don't have an org connected to a specific
GitHub org, you can use the following command to release an archive:

```
@atomist promote artifact --group atomist-project-templates --artifact my-new-template
  --version 0.0.1 --from_repo private-templates-dev --target_repo public-templates-dev
```
