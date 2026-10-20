# Puts a "RuneLite (RunescapeClaude)" shortcut on your desktop that runs Launch-RuneLite.bat.
# Run once from this folder:
#   powershell -ExecutionPolicy Bypass -File .\Create-Desktop-Shortcut.ps1

$repo = $PSScriptRoot
$desktop = [Environment]::GetFolderPath('Desktop')
$path = Join-Path $desktop 'RuneLite (RunescapeClaude).lnk'

$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($path)
$shortcut.TargetPath = Join-Path $repo 'Launch-RuneLite.bat'
$shortcut.WorkingDirectory = $repo
$shortcut.Description = 'Update and start RuneLite with 117 HD and the RunescapeClaude plugins'
$shortcut.Save()

Write-Host "Shortcut created: $path"
