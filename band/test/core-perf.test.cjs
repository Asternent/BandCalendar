const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const c=require('../src/common/calendar');
const dates=JSON.parse(fs.readFileSync(path.join(__dirname,'../../test/hko-reference.json'),'utf8')).map(row=>row[0]);
test('lightweight previews exactly match full events without decoding global records',()=>{
  c.resetCaches();
  for(const date of dates)for(const country of ['ALL','CN','US','JP','IN','KR','TW'])for(const flags of [[true,true],[false,false],[false,true],[true,false]]){
    const settings={...c.defaults(),country,showGlobal:flags[0],showTerms:flags[1]},before=c.cacheInfo().worldDecodes,p=c.preview(date,settings);
    assert.equal(c.cacheInfo().worldDecodes,before,'preview decoded '+date);
    const d=c.info(date,settings);
    assert.deepEqual(p,{date:d.date,weekday:d.weekday,lunar:d.lunar,status:d.status,firstEvent:d.events[0]?.n||'',eventCount:d.events.length},date+' '+country+' '+flags);
  }
  assert.equal(c.preview('2026-02-30'),null);
});
test('cold month fast path constructs only its 42 dates and performs one UTC conversion',()=>{
  let dateObjects=0,utcCalls=0;
  class CountedDate extends Date{constructor(...args){super(...args);dateObjects++}static UTC(...args){utcCalls++;return Date.UTC(...args)}}
  const box={module:{exports:{}},Date:CountedDate,require:name=>require(path.join(__dirname,'../src/common',name))};
  vm.runInNewContext(fs.readFileSync(path.join(__dirname,'../src/common/calendar.js'),'utf8'),box);
  dateObjects=0;utcCalls=0;const cells=box.module.exports.month('2026-02-01',true);
  assert.equal(cells.length,42);assert.equal(dateObjects,42);assert.equal(utcCalls,1);assert.equal(box.module.exports.cacheInfo().worldDecodes,0);
  const before=dateObjects;box.module.exports.month('2026-02-01',true);assert.equal(dateObjects,before,'cache hit rebuilt dates');
});
test('ring caches remain bounded through eviction and reset without stale month results',()=>{
  c.resetCaches();const first=c.month('2025-01-01',true);
  for(let y=2025;y<=2028;y++)for(let m=1;m<=12;m++){const key=y+'-'+String(m).padStart(2,'0')+'-01';c.month(key,true);c.info(key)}
  const last=c.cacheInfo();assert.equal(last.months,3);assert.equal(last.basic,64);assert.equal(last.worldDays,8);assert.deepEqual(c.month('2025-01-01',true),first);
  c.resetCaches();assert.deepEqual(c.cacheInfo(),{basic:0,months:0,worldDays:0,worldDecodes:0});
});
