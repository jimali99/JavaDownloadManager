$WScriptShell = New-Object -ComObject WScript.Shell
$DesktopPath = [System.Environment]::GetFolderPath('Desktop')
$Shortcut = $WScriptShell.CreateShortcut("$DesktopPath\Internet Download Manager (Java).lnk")
$Shortcut.TargetPath = "$PSScriptRoot\run.bat"
$Shortcut.WorkingDirectory = "$PSScriptRoot"
$Shortcut.Description = "Internet Download Manager (IDM Pro - Java Accelerated)"
$Shortcut.Save()
Write-Host "Desktop shortcut created successfully at: $DesktopPath\Internet Download Manager (Java).lnk"
