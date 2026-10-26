
/* Shared WorldEdit schematic library. Uses the panel's existing authenticated session. */
async function worldeditPage() {
  const content = main();
  content.innerHTML = `<div class="top"><div><h2>WorldEdit</h2><p>Un dépôt de schématics partagé entre les serveurs de jeu.</p></div></div><div style="display:grid;gap:16px">
    <div class="card"><h3>Importer un schématic</h3><p>.schem, .schematic ou .nbt · 32 Mo maximum. Un nom existant est conservé.</p>
    <div class="row"><input id="we-file" type="file" accept=".schem,.schematic,.nbt"><button class="btn pri" id="we-upload">Partager aux serveurs</button></div><p id="we-progress"></p></div>
    <div class="card"><h3>Serveurs reliés</h3><div id="we-servers"></div></div>
    <div class="card"><div class="row"><h3>Bibliothèque partagée</h3><button class="btn" id="we-refresh">Actualiser</button></div><div id="we-files"></div></div>
    <div class="card"><h3>Utilisation en jeu (opérateur)</h3><p><code>//schem list</code> affiche la bibliothèque. Charge un fichier avec <code>//schem load nom.schem</code>, puis place-le avec <code>//paste</code>. Annule une opération WorldEdit avec <code>//undo</code>.</p><p>Les fichiers enregistrés en jeu avec <code>//schem save nom</code> deviennent aussi disponibles sur les autres serveurs.</p></div></div>`;
  const refresh = async () => {
    const data = await api('/api/worldedit');
    $('#we-servers').innerHTML = data.servers.map(s => `<div class="row"><strong>${esc(s.name)}</strong><span>${s.shared ? 'Dossier partagé' : 'Non relié'} · ${s.installed ? 'WorldEdit installé' : 'WorldEdit à installer'}</span></div>`).join('');
    $('#we-files').innerHTML = data.files.length ? `<table><thead><tr><th>Fichier</th><th>Taille</th><th>Modifié</th><th></th></tr></thead><tbody>${data.files.map(f => `<tr><td>${esc(f.name)}</td><td>${Math.ceil(f.size / 1024)} Ko</td><td>${esc(fmtDate(f.modified))}</td><td><a class="btn" href="/api/worldedit/download?name=${encodeURIComponent(f.name)}">Télécharger</a></td></tr>`).join('')}</tbody></table>` : '<p>Aucun schématic pour le moment. Importe le premier fichier ci-dessus.</p>';
  };
  $('#we-refresh').onclick = () => act(refresh);
  $('#we-upload').onclick = async () => {
    const file = $('#we-file').files[0]; if (!file) return toast('Choisis un fichier à importer.', true);
    if (file.size > 32 * 1024 * 1024) return toast('32 Mo maximum.', true);
    const button = $('#we-upload'); button.disabled = true; $('#we-progress').textContent = 'Import en cours…';
    try {
      const response = await fetch('/api/worldedit/upload?name=' + encodeURIComponent(file.name), { method: 'POST', body: file, headers: { 'Content-Type': 'application/octet-stream' } });
      const result = await response.json(); if (!response.ok) throw Error(result.error || 'Import refusé');
      $('#we-progress').textContent = `Disponible sur ${result.sharedWith.length} serveurs de jeu.`; $('#we-file').value = '';await refresh();
      toast('Schématic partagé.');
    } catch(e) { $('#we-progress').textContent = '';toast(e.message,true); }
    finally { button.disabled = false; }
  };
  await act(refresh);
}
