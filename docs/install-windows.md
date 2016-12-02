# Install the RUG CLI on Windows

## Automated installation via Chocolatey

We used [Nuget](https://docs.nuget.org/) and 
[Chocolatey](https://chocolatey.org/) to package and distribute the CLI on
Windows systems (actually wherever .NET and Powershell run).

The following steps have been tested on Windows 10, your mileage may vary.

* Install Chocolatey on your host as per the [doc](https://chocolatey.org/install)
* Install the [jdk8](https://chocolatey.org/packages/jdk8) dependency
  using chocolatey as an Administrator:
  
  ```
  (admin) C:\ > choco install jdk8
  ```

* Then, install the CLI using Chocolatey as an administrator:

  ```
  (admin) C:\ > choco install rug-cli -s "'https://atomist.jfrog.io/atomist/api/nuget/nuget'"
  ```

  The CLI will be installed in `%programdata%\Chocolatey\lib\rug-cli` and
  available to your `%PATH%`. You can now run as a normal user:

  ```
  (user) C:\ > rug --version
  rug 0.13.0
  atomist/rug-cli.git (git revision 2cde8f5: last commit 2016-12-01)
  ```

  Notice, you will find the `.atomist` directory for settings and
  artifacts in `%USERPROFILE%\.atomist`

* Upgrading is done as follows:

  ```
  (admin) C:\ > choco upgrade rug-cli -s "'https://atomist.jfrog.io/atomist/api/nuget/nuget'"
  ```

* Removing is achieved like this:

  ```
  (admin) C:\ > choco uninstall rug-cli
  ```

  Files and directories in `%USERPROFILE%\.atomist` will not be removed. You can
  safely delete that directory manually if you don't intend to use the CLI
  any longer.

## Manual installation 

You can also download the CLI as a compressed archive and simply put it in
your `%PATH%` if you prefer. The archives can be found
[here](https://atomist.jfrog.io/atomist/libs-release/com/atomist/rug-cli/).

You will find the `.atomist` directory for settings and  artifacts in
`%USERPROFILE%\.atomist`.