// Runs the actual NeoForge server in an isolated folder; never targets a production service.
// Usage: node native_import.cjs RUNTIME MAP_OUTPUT JAVA
const fs=require('node:fs'),path=require('node:path'),{spawn}=require('node:child_process');
const [runtime,out,java]=process.argv.slice(2);
if(!runtime||!out||!java)throw Error('runtime, map output and java executable required');
if(!fs.existsSync(path.join(out,'export-report.json'))||!fs.existsSync(path.join(runtime,'server.properties')))throw Error('Preparation missing');
const properties=fs.readFileSync(path.join(runtime,'server.properties'),'utf8');
if(!properties.includes('server-port=25686')||!properties.includes('server-ip=127.0.0.1'))throw Error('Refusing non-isolated runtime');
const log=fs.createWriteStream(path.join(out,'native-import.log'),{flags:'a'});
const child=spawn(java,['-Xms512M','-Xmx4G','-Dmmorpg.preloadRadius=0','-XX:ActiveProcessorCount=4','@libraries/net/neoforged/neoforge/26.3.0.39-beta/win_args.txt','nogui'],{cwd:runtime,windowsHide:true,stdio:['pipe','pipe','pipe']});
fs.writeFileSync(path.join(runtime,'builder.pid'),String(child.pid));
const send=s=>child.stdin.write(s+'\n');
let ready=false,verification=false,finished=false,buffer='',report={started:new Date().toISOString(),runtime,imported:false,verified:false};
function onLine(line){
  log.write(line+'\n');
  if(/\[aeloria\]|Done \(|ERROR|Exception|Unknown|Unknown or incomplete|Incorrect|Cannot|Invalid/.test(line))console.log(line);
  if(!ready&&/Done \(/.test(line)){
    ready=true;
    ['gamerule random_tick_speed 0','gamerule fire_spread_radius_around_player 0','gamerule advance_time false','gamerule advance_weather false','gamerule mob_griefing false',
      'gamerule spawn_mobs false','gamerule respawn_radius 0','time set 6000','weather clear','aeloriaimport budget 40','aeloriaimport'].forEach(send);
  }
  if(/Import termin/.test(line)&&!verification){report.imported=true;verification=true;send('aeloriaimport verify');}
  if(/V.rification termin.e/.test(line)){
    report.verification=line;report.verified=/0 diff.rence\(s\) sur 100 secteur/.test(line);
    if(!report.verified){report.error='Nonzero native comparison';finish();return;}
    report.completed=new Date().toISOString();
    ['worldborder center 0 0','worldborder set 800','worldborder warning distance 12','setworldspawn 0 84 0'].forEach(send);
    const meta=JSON.parse(fs.readFileSync(path.join(out,'map.json'),'utf8'));
    // Block entities are intentionally not imported by AeloriaImport. Apply each sign after verification.
    let i=0;const signs=()=>{
      if(i>=meta.signs.length){send('save-all flush');setTimeout(finish,3000);return;}
      const s=meta.signs[i++],cx=Math.floor(s.x/16)*16,cz=Math.floor(s.z/16)*16;
      send(`forceload add ${cx} ${cz}`);
      setTimeout(()=>{
        send(`data merge block ${s.x} ${s.y} ${s.z} ${JSON.stringify({front_text:{messages:s.lines,color:'black',has_glowing_text:false}})}`);
        send(`forceload remove ${cx} ${cz}`);setTimeout(signs,40);
      },90);
    };signs();
  }
  if(/palette incompatible|contr.le pr.alable.*(chou|erreur)|import interrompu|erreur inattendue|Cannot animate/i.test(line)){report.error=line;setTimeout(finish,500);}
}
function consume(data){buffer+=data.toString('utf8');let n;while((n=buffer.indexOf('\n'))>=0){onLine(buffer.slice(0,n).replace(/\r$/,''));buffer=buffer.slice(n+1);}}
child.stdout.on('data',consume);child.stderr.on('data',consume);
function finish(){if(finished)return;finished=true;fs.writeFileSync(path.join(out,'native-report.json'),JSON.stringify(report,null,2));send('stop');}
child.on('exit',code=>{log.end();console.log(JSON.stringify({...report,exitCode:code}));process.exitCode=code||(!report.verified?1:0);});
child.on('error',e=>{console.error(e);process.exitCode=1;});
setTimeout(()=>{if(!finished){report.error='Timeout after 45 minutes';finish();}},45*60*1000).unref();
