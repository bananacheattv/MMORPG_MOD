# Sauvegarde d'un serveur Aeloria (Windows) : le monde et les reglages du mod, en une archive datee, avec rotation.
#
# usage : .\backup.ps1 -Serveur C:\aeloria -Destination D:\sauvegardes [-Garder 14]
#
# A lancer serveur ARRETE (ex. : juste apres le redemarrage quotidien), ou apres « save-off » puis « save-all flush » dans la console
# (et « save-on » ensuite) : copier un monde en cours d'ecriture peut donner une sauvegarde inutilisable.
# Sauvegarde : world\ (monde et donnees des joueurs), config\mmorpg\ (reglages, admin.json compris), server.properties, ops, whitelist, bans.
param(
    [Parameter(Mandatory = $true)][string]$Serveur,
    [Parameter(Mandatory = $true)][string]$Destination,
    [int]$Garder = 14
)
$ErrorActionPreference = 'Stop'

if (-not (Test-Path (Join-Path $Serveur 'world'))) { throw "Pas de dossier world dans $Serveur" }
New-Item -ItemType Directory -Force $Destination | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$tmp = Join-Path ([System.IO.Path]::GetTempPath()) "aeloria-backup-$stamp"
New-Item -ItemType Directory -Force $tmp | Out-Null
try {
    # robocopy gere les fichiers verrouilles mieux que Compress-Archive et permet d'exclure session.lock (codes de retour < 8 = succes)
    robocopy (Join-Path $Serveur 'world') (Join-Path $tmp 'world') /E /XF session.lock /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Copie du monde en echec (robocopy $LASTEXITCODE)" }
    robocopy (Join-Path $Serveur 'config\mmorpg') (Join-Path $tmp 'config\mmorpg') /E /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Copie des reglages en echec (robocopy $LASTEXITCODE)" }
    foreach ($f in 'server.properties', 'ops.json', 'whitelist.json', 'banned-players.json', 'banned-ips.json') {
        $p = Join-Path $Serveur $f
        if (Test-Path $p) { Copy-Item $p $tmp }
    }
    $zip = Join-Path $Destination "aeloria-$stamp.zip"
    Compress-Archive -Path (Join-Path $tmp '*') -DestinationPath $zip -CompressionLevel Optimal
    # verification : l'archive doit se rouvrir
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::OpenRead($zip).Dispose()
    Write-Host ("Sauvegarde : {0} ({1:N1} Mo)" -f $zip, ((Get-Item $zip).Length / 1MB))
}
finally {
    Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
}

# rotation : on ne garde que les plus recentes
Get-ChildItem $Destination -Filter 'aeloria-*.zip' | Sort-Object LastWriteTime -Descending | Select-Object -Skip $Garder | ForEach-Object {
    Remove-Item $_.FullName -Force
    Write-Host "Ancienne sauvegarde supprimee : $($_.FullName)"
}
