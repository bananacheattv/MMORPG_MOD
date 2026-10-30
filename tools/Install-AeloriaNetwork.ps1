param(
    [Parameter(Mandatory = $true)][string]$PanelRoot,
    [string]$ModJar = '',
    [switch]$AddDungeons
)
$ErrorActionPreference = 'Stop'
$panelPath = (Resolve-Path -LiteralPath $PanelRoot).Path
if (-not $ModJar) {
    # jar produit par ./gradlew build (aeloria-<version>.jar), hors sources/javadoc
    $built = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '../build/libs') -Filter 'aeloria-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $built) { throw 'Aucun jar du mod dans build/libs : lancez ./gradlew build ou passez -ModJar.' }
    $ModJar = $built.FullName
}
$jarPath = (Resolve-Path -LiteralPath $ModJar).Path
$configPath = Join-Path $panelPath 'config.json'
$panelConfig = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
$roles = @{ hub = 'HUB'; serveur = 'MAIN'; mines = 'MINES'; plaines = 'PLAINS'; donjons = 'DUNGEONS' }
$createDungeons = $AddDungeons -and -not ($panelConfig.services | Where-Object { $_.id -eq 'donjons' })
if ($createDungeons -and (Test-Path -LiteralPath (Join-Path $panelPath 'servers/donjons'))) { throw 'Le dossier donjons existe deja sans service enregistre. Installation interrompue pour le preserver.' }
if ($createDungeons) {
    if ($panelConfig.services | Where-Object { $_.port -eq 25574 }) { throw 'Le port 25574 est deja affecte dans le panel.' }
    $probe = [Net.Sockets.TcpClient]::new()
    try {
        try { $null = $probe.ConnectAsync('127.0.0.1', 25574).Wait(500) } catch { }
        if ($probe.Connected) { throw 'Le port 25574 est deja utilise.' }
    } finally { $probe.Dispose() }
}
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
if (Test-Path -LiteralPath (Join-Path $panelPath 'shared')) { Copy-Item -LiteralPath (Join-Path $panelPath 'shared') -Destination $backupPath -Recurse }
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
if ($createDungeons) {
    $sourceService = $panelConfig.services | Where-Object { $_.id -eq 'serveur' } | Select-Object -First 1
    if (-not $sourceService) { throw 'Serveur principal introuvable pour la migration des instances.' }
    $sourcePath = Join-Path $panelPath $sourceService.dir
    $dungeonPath = Join-Path $panelPath 'servers/donjons'
    New-Item -ItemType Directory -Path $dungeonPath | Out-Null
    foreach ($entry in @('libraries', 'config', 'mods', 'eula.txt', 'server.properties', 'user_jvm_args.txt', 'run.bat')) {
        $sourceEntry = Join-Path $sourcePath $entry
        if (Test-Path -LiteralPath $sourceEntry) { Copy-Item -LiteralPath $sourceEntry -Destination $dungeonPath -Recurse }
    }
    # Whitelist dungeon terrain/metadata. Do not duplicate player files, banks, auctions or the MAIN town.
    $sourceWorld = Join-Path $sourcePath 'world'
    $targetWorld = Join-Path $dungeonPath 'world'
    New-Item -ItemType Directory -Path $targetWorld | Out-Null
    $worldEntries = @('level.dat', 'level.dat_old', 'datapacks', 'data/minecraft', 'dimensions/mmorpg/donjons')
    foreach ($dataRoot in @('data/mmorpg', 'dimensions/minecraft/overworld/data/mmorpg')) {
        foreach ($metadata in @('instances_v2.dat', 'trials.dat', 'zones.dat')) { $worldEntries += "$dataRoot/$metadata" }
    }
    foreach ($entry in $worldEntries) {
        $from = Join-Path $sourceWorld $entry
        if (-not (Test-Path -LiteralPath $from)) { continue }
        $to = Join-Path $targetWorld $entry
        New-Item -ItemType Directory -Path (Split-Path -Parent $to) -Force | Out-Null
        Copy-Item -LiteralPath $from -Destination $to -Recurse
    }
    $propertiesPath = Join-Path $dungeonPath 'server.properties'
    $properties = Get-Content -LiteralPath $propertiesPath -Raw
    $settings = @{ 'server-port' = '25574'; 'query.port' = '25574'; 'server-ip' = '127.0.0.1'; 'motd' = 'Aeloria - Donjons'; 'view-distance' = '6'; 'simulation-distance' = '4'; 'pause-when-empty-seconds' = '0' }
    foreach ($key in $settings.Keys) {
        $pattern = '(?m)^' + [regex]::Escape($key) + '=.*$'
        if ($properties -match $pattern) { $properties = [regex]::Replace($properties, $pattern, "$key=$($settings[$key])") }
        else { $properties += "`n$key=$($settings[$key])`n" }
    }
    Set-Content -LiteralPath $propertiesPath -Value $properties -Encoding utf8
    Set-Content -LiteralPath (Join-Path $dungeonPath 'user_jvm_args.txt') -Value @('-Xms1G', '-Xmx4G') -Encoding ascii
    $generalPath = Join-Path $dungeonPath 'config/mmorpg/general.json'
    $general = Get-Content -LiteralPath $generalPath -Raw | ConvertFrom-Json
    $general.hubCinematic = $false; $general.openClassSelectionOnJoin = $false
    $general.customSpawning = $false; $general.scaleVanillaMobs = $false
    $general | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $generalPath -Encoding utf8
    $panelConfig.services += [pscustomobject]@{
        id = 'donjons'; name = 'Donjons (instances)'; type = 'game'; order = 2; dir = 'servers/donjons'
        command = $sourceService.command; port = 25574; stopCommand = 'stop'; stopExec = ''
        stopTimeout = 120; autoRestart = $true; dailyRestart = $false
    }
    $order = @{ db = 1; donjons = 2; serveur = 3; hub = 4; mines = 5; plaines = 6; proxy = 7 }
    foreach ($service in $panelConfig.services) { if ($order.ContainsKey($service.id)) { $service.order = $order[$service.id] } }
    $proxyConfigPath = Join-Path $panelPath 'servers/proxy/velocity.toml'
    Copy-Item -LiteralPath $proxyConfigPath -Destination (Join-Path $backupPath 'velocity.toml')
    $proxyConfig = Get-Content -LiteralPath $proxyConfigPath -Raw
    if ($proxyConfig -notmatch '(?m)^\s*donjons\s*=') {
        if ($proxyConfig -notmatch '(?m)^\[servers\][ \t]*\r?$') { throw 'Section servers absente dans Velocity.' }
        $proxyConfig = [regex]::Replace($proxyConfig, '(?m)^\[servers\][ \t]*\r?$', "[servers]`n`tdonjons = `"127.0.0.1:25574`"")
        Set-Content -LiteralPath $proxyConfigPath -Value $proxyConfig -Encoding utf8
    }
    $panelConfig | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $configPath -Encoding utf8
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
        hub = 'hub'; main = 'serveur'; mines = 'mines'; plains = 'plaines'; dungeons = 'donjons'
        dungeonMaxInstancesPerPool = 8; dungeonBuildBudgetMs = 8
    }
    $network | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $modConfig 'network.json') -Encoding utf8
    $modsPath = Join-Path $serverPath 'mods'
    New-Item -ItemType Directory -Path $modsPath -Force | Out-Null
    # un seul jar du mod : une ancienne version laissee a cote ferait refuser le demarrage (mod en double)
    Get-ChildItem -LiteralPath $modsPath -File | Where-Object { $_.Name -match '^(mmorpg|aeloria)-.*\.jar$' -and $_.FullName -ne $jarPath } | Remove-Item -Force
    $modTarget = Join-Path $modsPath (Split-Path -Leaf $jarPath)
    if ($modTarget -ne $jarPath) { Copy-Item -LiteralPath $jarPath -Destination $modTarget }
}
foreach ($guide in @('RESEAU_AELORIA.md', 'PORTAILS_ET_UNDO.md')) {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot "../$guide") -Destination (Join-Path $panelPath $guide)
}
Write-Output "Installation terminee : serveurs configures. Sauvegarde : $backupPath"
Write-Output 'Rechargez le panel puis demarrez les serveurs. Installez aussi le nouveau JAR sur les clients.'
