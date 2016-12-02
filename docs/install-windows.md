# Install the RUG CLI on Windows

## Automated installation via Chocolatey

We used [Nuget](https://docs.nuget.org/) and 
[Chocolatey](https://chocolatey.org/) to package and distribute the CLI on
Windows systems (actually wherever .NET and Powershell run).

The following steps have been tested on Windows 10, your mileage may vary.

* Install Chocolatey on your host as per the [doc](https://chocolatey.org/install)
* Install the jdk8 dependency using choclatey
  
  ```
  > choco install -y jdk8
  ```

* Then configure chocolatey so it can download from our 
  [Nuget feed](https://atomist.jfrog.io/atomist/api/nuget/nuget)

  ```
  > choco install rug-cli -s "'https://atomist.jfrog.io/atomist/api/nuget/nuget'"
  ```

The CLI will be installed in `%programdata%\Chocolatey\libs\rug-cli` and 
available to your PATH. You can now run:

```
> rug -version
```

## Manual installation 

You can also download the CLI as a compressed archive and simply put it in
your PATH if you prefer. The archives can be found 
[here](https://atomist.jfrog.io/atomist/libs-release/com/atomist/rug-cli/).

