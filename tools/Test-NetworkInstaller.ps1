$ErrorActionPreference = 'Stop'
function Assert-Installer([bool]$Condition, [string]$Message) { if (-not $Condition) { throw "Installer check failed: $Message" } }
$fixture = Join-Path $PSScriptRoot ('../build/installer-check-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $fixture | Out-Null
$fixture = (Resolve-Path -LiteralPath $fixture).Path
$source = Join-Path $fixture 'servers/serveur'
$proxy = Join-Path $fixture 'servers/proxy'
foreach ($folder in @('libraries', 'mods', 'config/mmorpg', 'world/dimensions/mmorpg/donjons/region', 'world/dimensions/minecraft/overworld/data/mmorpg', 'world/players', 'world/data/mmorpg')) {
    New-Item -ItemType Directory -Path (Join-Path $source $folder) -Force | Out-Null
}
New-Item -ItemType Directory -Path $proxy -Force | Out-Null
@{ services = @(
    @{ id = 'serveur'; dir = 'servers/serveur'; port = 0; order = 2; command = 'java @user_jvm_args.txt @libraries/example/win_args.txt nogui' },
    @{ id = 'proxy'; dir = 'servers/proxy'; port = 0; order = 6; command = 'java -jar velocity.jar' }
) } | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $fixture 'config.json') -Encoding utf8
Set-Content -LiteralPath (Join-Path $proxy 'velocity.toml') -Value @('[servers]', 'serveur = "127.0.0.1:25570"', 'try = ["serveur"]') -Encoding utf8
Set-Content -LiteralPath (Join-Path $source 'server.properties') -Value @('server-port=0', 'server-ip=127.0.0.1', 'level-name=world') -Encoding utf8
@{ hubCinematic = $true; openClassSelectionOnJoin = $true; customSpawning = $true; scaleVanillaMobs = $true } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $source 'config/mmorpg/general.json') -Encoding utf8
Set-Content -LiteralPath (Join-Path $source 'world/level.dat') -Value 'fixture-level'
Set-Content -LiteralPath (Join-Path $source 'world/players/private.dat') -Value 'player-must-not-be-cloned'
Set-Content -LiteralPath (Join-Path $source 'world/data/mmorpg/hdv.dat') -Value 'auction-must-not-be-cloned'
Set-Content -LiteralPath (Join-Path $source 'world/dimensions/minecraft/overworld/data/mmorpg/banque.dat') -Value 'bank-must-not-be-cloned'
Set-Content -LiteralPath (Join-Path $source 'world/dimensions/minecraft/overworld/data/mmorpg/instances_v2.dat') -Value 'instance-metadata'
Set-Content -LiteralPath (Join-Path $source 'world/dimensions/mmorpg/donjons/region/test.mca') -Value 'dungeon-terrain'
& (Join-Path $PSScriptRoot 'Install-AeloriaNetwork.ps1') -PanelRoot $fixture -AddDungeons
$created = Join-Path $fixture 'servers/donjons'
$panel = Get-Content -LiteralPath (Join-Path $fixture 'config.json') -Raw | ConvertFrom-Json
$service = $panel.services | Where-Object { $_.id -eq 'donjons' }
Assert-Installer ($service.port -eq 25574 -and $service.order -lt ($panel.services | Where-Object { $_.id -eq 'proxy' }).order) 'service and startup order'
$network = Get-Content -LiteralPath (Join-Path $created 'config/mmorpg/network.json') -Raw | ConvertFrom-Json
Assert-Installer ($network.role -eq 'DUNGEONS' -and $network.dungeonMaxInstancesPerPool -eq 8) 'role and capacity'
Assert-Installer (Test-Path -LiteralPath (Join-Path $created 'world/dimensions/mmorpg/donjons/region/test.mca')) 'dungeon terrain copied'
Assert-Installer (Test-Path -LiteralPath (Join-Path $created 'world/dimensions/minecraft/overworld/data/mmorpg/instances_v2.dat')) 'instance metadata copied'
Assert-Installer (-not (Test-Path -LiteralPath (Join-Path $created 'world/players/private.dat'))) 'players not cloned'
Assert-Installer (-not (Test-Path -LiteralPath (Join-Path $created 'world/data/mmorpg/hdv.dat'))) 'auctions not cloned'
Assert-Installer (-not (Test-Path -LiteralPath (Join-Path $created 'world/dimensions/minecraft/overworld/data/mmorpg/banque.dat'))) 'bank not cloned'
Assert-Installer (Test-Path -LiteralPath (Join-Path $source 'world/players/private.dat')) 'original player data retained'
$velocity = Get-Content -LiteralPath (Join-Path $proxy 'velocity.toml') -Raw
Assert-Installer ($velocity -match 'donjons = "127.0.0.1:25574"') 'Velocity address'
Assert-Installer ((Get-Content -LiteralPath (Join-Path $created 'server.properties') -Raw) -match 'server-port=25574') 'backend port'
Write-Output 'INSTALLER PASS: service, proxy, roles, selective world migration, player/bank/auction preservation'
