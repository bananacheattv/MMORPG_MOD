param(
    [Parameter(Mandatory = $true)][string]$PanelRoot,
    [string]$ModJar = (Join-Path $PSScriptRoot '../build/libs/mmorpg-1.0.0.jar')
)
$ErrorActionPreference = 'Stop'
$panelPath = (Resolve-Path -LiteralPath $PanelRoot).Path
$jarPath = (Resolve-Path -LiteralPath $ModJar).Path
$configPath = Join-Path $panelPath 'config.json'
$panelConfig = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
$roles = @{ hub = 'HUB'; serveur = 'MAIN'; mines = 'MINES'; plaines = 'PLAINS' }
foreach ($service in $panelConfig.services) {
    if (-not $roles.ContainsKey($service.id)) { continue }
    $client = [Net.Sockets.TcpClient]::new()
    try {
        $connection = $client.ConnectAsync('127.0.0.1', [int]$service.port)
        try { $null = $connection.Wait(500) } catch { }
        if ($client.Connected) { throw "Le serveur $($service.id) tourne encore. Arretez les serveurs de jeu depuis le panel avant l'installation." }
    } finally { $client.Dispose() }
    if (-not (Test-Path -LiteralPath (Join-Path $panelPath $service.dir))) { throw "Dossier absent : $($service.dir)" }
}
$backupPath = Join-Path $panelPath ('backups/network-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backupPath | Out-Null
Copy-Item -LiteralPath $configPath -Destination (Join-Path $backupPath 'config.json')
foreach ($service in $panelConfig.services) {
    if (-not $roles.ContainsKey($service.id)) { continue }
    $serverPath = Join-Path $panelPath $service.dir
    $savedPath = Join-Path $backupPath $service.id
    New-Item -ItemType Directory -Path $savedPath | Out-Null
    # Worlds, configurations and mods are recoverable together; do not replace any worlds.
    foreach ($entry in @('world', 'config', 'mods', 'server.properties', 'user_jvm_args.txt')) {
        $sourcePath = Join-Path $serverPath $entry
        if (Test-Path -LiteralPath $sourcePath) { Copy-Item -LiteralPath $sourcePath -Destination $savedPath -Recurse }
    }
}
$sharedPath = Join-Path $panelPath 'shared/players'
New-Item -ItemType Directory -Path $sharedPath -Force | Out-Null
foreach ($service in $panelConfig.services) {
    if (-not $roles.ContainsKey($service.id)) { continue }
    $serverPath = Join-Path $panelPath $service.dir
    $modConfig = Join-Path $serverPath 'config/mmorpg'
    New-Item -ItemType Directory -Path $modConfig -Force | Out-Null
    $network = [ordered]@{
        enabled = $true; role = $roles[$service.id]; sharedDirectory = $sharedPath.Replace('\', '/')
        hub = 'hub'; main = 'serveur'; mines = 'mines'; plains = 'plaines'
    }
    $network | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $modConfig 'network.json') -Encoding utf8
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $serverPath 'mods/mmorpg-1.0.0.jar')
}
Copy-Item -LiteralPath (Join-Path $PSScriptRoot '../RESEAU_AELORIA.md') -Destination (Join-Path $panelPath 'RESEAU_AELORIA.md')
Write-Output "Installation terminee : quatre serveurs configures. Sauvegarde : $backupPath"
Write-Output 'Rechargez le panel puis demarrez les serveurs. Installez aussi le nouveau JAR sur les clients.'
