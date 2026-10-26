param([Parameter(Mandatory=$true)][string]$PanelRoot)
$ErrorActionPreference='Stop'
$panelPath=(Resolve-Path -LiteralPath $PanelRoot).Path
$cfg=Get-Content -LiteralPath (Join-Path $panelPath 'config.json') -Raw | ConvertFrom-Json
$games=@($cfg.services | Where-Object {$_.type -eq 'game'})
foreach($service in $games) {
    $probe=[Net.Sockets.TcpClient]::new()
    try {try {$null=$probe.ConnectAsync('127.0.0.1',[int]$service.port).Wait(300)}catch{}
        if($probe.Connected){throw "Arretez les serveurs de jeu avant de reinitialiser le spawn ($($service.id))."}
    } finally {$probe.Dispose()}
}
$backup=Join-Path $panelPath ('backups/manual-spawn-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backup | Out-Null
foreach($service in $games) {
    $serverPath=[IO.Path]::GetFullPath((Join-Path $panelPath $service.dir))
    if(-not $serverPath.StartsWith($panelPath+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Dossier serveur hors du panel.'}
    $roleBackup=Join-Path $backup $service.id;New-Item -ItemType Directory -Path $roleBackup | Out-Null
    $spawnConfig=Join-Path $serverPath 'config/mmorpg/spawn.json'
    Copy-Item -LiteralPath $spawnConfig -Destination (Join-Path $roleBackup 'spawn.json')
    $settings=Get-Content -LiteralPath $spawnConfig -Raw | ConvertFrom-Json
    $settings | Add-Member -NotePropertyName manualBuild -NotePropertyValue $true -Force
    $settings | Add-Member -NotePropertyName flatPlatform -NotePropertyValue $false -Force
    $settings.autoNpcs=$false;$settings.autoStations=$false
    $settings | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $spawnConfig -Encoding utf8
    if($service.id -notin @('serveur','hub')){continue}
    $worldPath=Join-Path $serverPath 'world'
    $dimension=[IO.Path]::GetFullPath((Join-Path $worldPath 'dimensions/mmorpg/spawn'))
    $destination=[IO.Path]::GetFullPath((Join-Path $roleBackup 'dimension-spawn'))
    # Check resolved absolute source/destination before moving any directory tree.
    if(-not $dimension.StartsWith($serverPath+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase) -or
       -not $destination.StartsWith($backup+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase) -or
       [IO.Path]::GetFileName($dimension) -ne 'spawn'){throw 'Chemin de dimension inattendu.'}
    if(Test-Path -LiteralPath $dimension) {
        $folder=Get-Item -LiteralPath $dimension
        if($folder.Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'La dimension est un lien : migration manuelle requise.'}
        Move-Item -LiteralPath $dimension -Destination $destination
    }
    $data=Join-Path $worldPath 'dimensions/minecraft/overworld/data/mmorpg/spawn.dat'
    if(Test-Path -LiteralPath $data){Move-Item -LiteralPath $data -Destination (Join-Path $roleBackup 'spawn.dat')}
}
Write-Output "Ancien spawn conserve dans $backup. Le hub, les personnages et les autres dimensions sont inchanges."
