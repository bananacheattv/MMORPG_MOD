const {test}=require('node:test'),assert=require('node:assert/strict');
const {createProcessMetrics}=require('./process-metrics');
test('CPU deltas use machine capacity and restart baselines; RAM follows each PID',()=>{
  const services=[{pid:10},{pid:20},{pid:null}],metrics=createProcessMetrics({services:()=>services,cores:4});
  metrics.apply([{Id:10,CPU:1,WorkingSet64:1024,StartTime:'a'},{Id:20,CPU:3,WorkingSet64:2048,StartTime:'b'}],1000);
  assert.equal(services[0].cpu,null);assert.equal(services[1].mem,2048);
  metrics.apply([{Id:10,CPU:3,WorkingSet64:4096,StartTime:'a'},{Id:20,CPU:7,WorkingSet64:8192,StartTime:'b'}],5000);
  assert.equal(services[0].cpu,12.5);assert.equal(services[1].cpu,25);assert.equal(services[0].mem,4096);assert.equal(services[2].cpu,0);
  metrics.apply([{Id:10,CPU:100,WorkingSet64:64,StartTime:'new-process'}],9000);
  assert.equal(services[0].cpu,null);assert.equal(services[1].cpu,null);
  services[0].pid=null;metrics.apply([],13000);assert.equal(services[0].mem,0);assert.equal(services[0].cpu,0);
});
test('sampling failure shows unavailable instead of inventing zero consumption',async()=>{
  const services=[{pid:10}],metrics=createProcessMetrics({services:()=>services,run:(exe,args,opts,callback)=>callback(Error('denied'))});
  await metrics.sample();assert.equal(services[0].cpu,null);assert.equal(services[0].mem,null);
});
