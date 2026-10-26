'use strict';
const { execFile } = require('node:child_process');
const os = require('node:os');

function createProcessMetrics({ services, run = execFile, cores = os.cpus().length, now = () => performance.now() }) {
  const previous = new Map(); let busy = false;
  function apply(rows, time) {
    const byPid = new Map(rows.map(row => [Number(row.Id), row]));
    const keep = new Set();
    for (const service of services()) {
      if (!service.pid) { service.cpu = 0; service.mem = 0; service.metricsAt = null; continue; }
      const row = byPid.get(service.pid);keep.add(service.pid);
      if (!row || row.CPU == null || !Number.isFinite(Number(row.CPU))) { service.cpu = null; service.mem = null; service.metricsAt = null; continue; }
      const cpu = Number(row.CPU), old = previous.get(service.pid);
      service.cpu = old && old.start === row.StartTime && time > old.time && cpu >= old.cpu
        ? Math.min(100, Math.max(0, (cpu - old.cpu) * 100000 / (time - old.time) / Math.max(1, cores))) : null;
      service.mem = Math.max(0, Number(row.WorkingSet64) || 0);
      service.metricsAt = Date.now();
      previous.set(service.pid, { cpu, time, start: row.StartTime });
    }
    for (const pid of previous.keys()) if (!keep.has(pid)) previous.delete(pid);
  }
  async function sample() {
    if (busy) return;busy = true;
    try {
      const ids = services().map(s => s.pid).filter(pid => Number.isSafeInteger(pid) && pid > 0);
      if (!ids.length) { apply([], now());return; }
      // CPU is sampled as a delta, normalized to the complete machine (100% = all cores).
      // One native query for all services, without dependence on Windows performance counters.
      const command = `Get-Process -Id ${ids.join(',')} -ErrorAction SilentlyContinue | Select-Object Id,CPU,WorkingSet64,@{Name='StartTime';Expression={$_.StartTime.ToUniversalTime().Ticks.ToString()}} | ConvertTo-Json -Compress`;
      const out = await new Promise((resolve,reject) => run('powershell.exe', ['-NoProfile','-NonInteractive','-Command',command],
        { windowsHide:true, timeout:3500, maxBuffer:1024*1024 }, (error,stdout) => error ? reject(error) : resolve(stdout)));
      const parsed = JSON.parse(String(out).trim() || '[]');apply(Array.isArray(parsed) ? parsed : [parsed],now());
    } catch {
      for (const service of services()) {service.cpu=service.pid?null:0;service.mem=service.pid?null:0;service.metricsAt=null;}
    } finally { busy=false; }
  }
  return { sample, apply };
}
module.exports = { createProcessMetrics };
