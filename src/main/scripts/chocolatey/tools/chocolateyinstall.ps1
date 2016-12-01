$packageName = 'rug-cli'
$32BitUrl = 'https://github.com/atomist/rug-cli/releases/download/__VERSION__/rug-cli-__VERSION__-bin.zip'
$64BitUrl = $32BitUrl
$global:installLocation = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$checksum = '__CHECKSUM__'
$checksumType = 'sha256'
$checksum64 = '__CHECKSUM__'
$checksumType64 = 'sha256'

Install-ChocolateyZipPackage "$packageName" "$32BitUrl" "$global:installLocation" "$64BitUrl" -checksum "$checksum" -checksumType "$checksumType" -checksum64 "$checksum64" -checksumType64 "$checksumType64"

# create an executable shim for the batch script that runs rug
Install-BinFile "rug" "$global:installLocation\rug-cli-__VERSION__\bin\rug.bat"

# rug expects those directories to exist
New-Item -ItemType Directory -Force -Path "$ENV:UserProfile\.atomist" | Out-Null
New-Item -ItemType Directory -Force -Path "$ENV:UserProfile\.atomist\repository" | Out-Null

# store the Java preferences in the registry
# these keys don't seem to exist by default
function JavaPrefsRegKey([String] $JavaSoftPath) {
    $PrefsPath="$JavaSoftPath\Prefs"
    # we check its existence because otherwise
    # we would destroy whatever is in there already
    if((Test-Path $JavaSoftPath) -and !(Test-Path $PrefsPath)) {
        New-Item -Path $PrefsPath -Force | Out-Null
    }
}
JavaPrefsRegKey 'HKLM:\Software\JavaSoft'
JavaPrefsRegKey 'HKLM:\Software\WOW6432Node\JavaSoft'