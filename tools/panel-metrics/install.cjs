const fs=require('node:fs'),path=require('node:path');
function install(root) {
  root=fs.realpathSync(root);const server=path.join(root,'server.js'),app=path.join(root,'public/app.js');
  let backend=fs.readFileSync(server,'utf8'),frontend=fs.readFileSync(app,'utf8');
  if(!backend.includes('const serviceMetrics =')) {
    const start=backend.indexOf('function sampleMem() {'),end=backend.indexOf('setInterval(() => { sampleCpu(); sampleMem();',start);
    if(start<0||end<0)throw Error('Version du panel incompatible avec les métriques.');
    backend=backend.slice(0,start)+"const serviceMetrics = require('./process-metrics').createProcessMetrics({ services: ordered });\nfunction sampleMem() { serviceMetrics.sample(); }\n"+backend.slice(end);
    const field='mem: s.mem, restarts: s.restarts';
    if(!backend.includes(field))throw Error('Snapshot du panel incompatible.');
    backend=backend.replace(field,'mem: s.pid ? (s.metricsAt ? s.mem : null) : 0, cpu: s.pid ? (s.cpu ?? null) : 0, metricsAt: s.metricsAt || null, restarts: s.restarts');
  }
  if(!frontend.includes('CPU du serveur')) {
    const memory="<div><span>RAM</span><b>${x.mem ? fmtBytes(x.mem) : '—'}</b></div>";
    if(!frontend.includes(memory))throw Error('Carte serveur incompatible.');
    frontend=frontend.replace('<div class="meta">','<div class="meta" style="grid-template-columns:repeat(2,minmax(0,1fr))">');
    frontend=frontend.replace(memory,`<div title="Part du processeur total de la machine, tous les cœurs confondus"><span>CPU du serveur</span><b>\${x.pid ? (x.cpu == null ? 'Mesure…' : x.cpu.toFixed(1) + '%') : '0%'}</b></div>
        <div><span>RAM du serveur</span><b>\${x.pid ? (x.mem == null ? 'Indisponible' : fmtBytes(x.mem)) : '0 Mo'}</b></div>`);
    const anchor='<div class="row"><button class="btn sm pri" data-a="start"';
    if(!frontend.includes(anchor))throw Error('Actions serveur introuvables.');
    frontend=frontend.replace(anchor,`<div style="margin:0 0 12px" aria-label="Consommation du serveur">
          <div class="status-txt">CPU · \${x.cpu == null && x.pid ? 'en attente de mesure' : (x.cpu || 0).toFixed(1) + '% de la machine'}</div>
          <div class="bar"><i style="width:\${Math.min(100,x.cpu || 0)}%"></i></div>
          <div class="status-txt">RAM · \${x.mem == null && x.pid ? 'mesure indisponible' : ((x.mem || 0)/sys.memTotal*100).toFixed(1) + '% de la machine'}</div>
          <div class="bar"><i style="width:\${Math.min(100,(x.mem || 0)/sys.memTotal*100)}%"></i></div>
          <div class="status-txt">Actualisation automatique · 4 secondes</div></div>
        `+anchor);
  }
  const backup=path.join(root,'backups','panel-metrics-'+Date.now());fs.mkdirSync(backup,{recursive:true});
  fs.copyFileSync(server,path.join(backup,'server.js'));fs.copyFileSync(app,path.join(backup,'app.js'));
  fs.copyFileSync(path.join(__dirname,'process-metrics.js'),path.join(root,'process-metrics.js'));
  fs.writeFileSync(server,backend);fs.writeFileSync(app,frontend);return {backup};
}
if(require.main===module)console.log(JSON.stringify(install(process.argv[2])));
module.exports={install};
