$packageName = 'rug-cli'
Remove-Item "$env:ChocolateyInstall\lib\${packageName}" -recurse
Remove-Item "$env:ChocolateyInstall\bin\rug.exe"