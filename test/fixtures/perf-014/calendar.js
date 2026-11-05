const data=require('./data')
const world=require('./world')
const countries=require('./countries')
const DAY=86400000
const lunarDays=['初一','初二','初三','初四','初五','初六','初七','初八','初九','初十','十一','十二','十三','十四','十五','十六','十七','十八','十九','二十','廿一','廿二','廿三','廿四','廿五','廿六','廿七','廿八','廿九','三十']
const lunarMonths=['正月','二月','三月','四月','五月','六月','七月','八月','九月','十月','十一月','臘月']
const lunarFestivals={'1-1':'春節','1-15':'元宵節','2-2':'龍抬頭','5-5':'端午節','7-7':'七夕','7-15':'中元節','8-15':'中秋節','9-9':'重陽節','12-8':'臘八節'}
const translations={"New Year's Day":'元旦',"Christmas Day":'聖誕節',"Christmas":'聖誕節',"Easter Sunday":'復活節',"Easter Monday":'復活節星期一',"Good Friday":'耶穌受難日',"Labor Day":'勞動節',"Labour Day":'勞動節',"National Day":'國慶日',"Independence Day":'獨立日',"Thanksgiving Day":'感恩節',"New Year's Eve":'除夕／跨年夜'}
function pad(n){return ('0'+n).slice(-2)}
function key(d){return d.getUTCFullYear()+'-'+pad(d.getUTCMonth()+1)+'-'+pad(d.getUTCDate())}
function epoch(s){return Date.UTC(Number(s.slice(0,4)),Number(s.slice(5,7))-1,Number(s.slice(8,10)))/DAY}
const startDays=data.lunarStarts.map(s=>epoch(s[0]))
const holidayRuns=[],holidayByDate={},holidayStartDays={}
Object.keys(data.cn).sort().forEach(date=>{
  const entry=data.cn[date];if(entry[0]!=='rest')return
  let run=holidayRuns.length?holidayRuns[holidayRuns.length-1]:null
  if(!run||epoch(date)!==epoch(run.end)+1){run={name:entry[1],start:date,end:date,days:0};holidayRuns.push(run);holidayStartDays[date]=epoch(date)}
  run.end=date;run.days++;holidayByDate[date]=run
})
// Fixed-size FIFO rings: constant-time lookup/eviction, with no growing map or shift.
function createCache(){return {keys:[],values:Object.create(null),next:0,size:0}}
let basicCache=createCache(),monthCache=createCache(),worldCache=createCache(),worldDecodes=0
function remember(cache,k,v,max){
  if(Object.prototype.hasOwnProperty.call(cache.values,k)){cache.values[k]=v;return v}
  if(cache.size===max){delete cache.values[cache.keys[cache.next]]}else cache.size++
  cache.keys[cache.next]=k;cache.values[k]=v;cache.next=(cache.next+1)%max;return v
}
function cached(cache,k){return cache.values[k]||null}
function valid(s){if(typeof s!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(s)||s<data.minDate||s>data.maxDate)return false;return key(new Date(epoch(s)*DAY))===s}
function today(){const d=new Date();return d.getFullYear()+'-'+pad(d.getMonth()+1)+'-'+pad(d.getDate())}
function findStart(day){let lo=0,hi=startDays.length-1,found=-1;while(lo<=hi){const mid=(lo+hi)>>1;if(startDays[mid]<=day){found=mid;lo=mid+1}else hi=mid-1}return found}
function lunarAt(day,index){if(typeof index!=='number')index=findStart(day);if(index<0)return null;const found=data.lunarStarts[index],n=day-startDays[index]+1;if(n<1||n>30)return null;const prefix=(found[2]?'閏':'')+lunarMonths[found[1]-1];return {month:found[1],leap:found[2],day:n,year:found[3],zodiac:found[4],text:prefix+lunarDays[n-1],short:n===1?prefix:lunarDays[n-1]}}
function lunar(s){return valid(s)?lunarAt(epoch(s)):null}
function defaults(){return {country:'ALL',monday:true,showGlobal:true,showTerms:true}}
function validSettings(s){return s&&typeof s==='object'&&(s.country==='ALL'||s.country==='CN'||Object.prototype.hasOwnProperty.call(countries,s.country))&&typeof s.monday==='boolean'&&typeof s.showGlobal==='boolean'&&typeof s.showTerms==='boolean'}
function sameSettings(a,b){return a&&b&&a.country===b.country&&a.monday===b.monday&&a.showGlobal===b.showGlobal&&a.showTerms===b.showTerms}
function settingsKey(s){return s.country+'|'+(s.monday?1:0)+(s.showGlobal?1:0)+(s.showTerms?1:0)}
function countryName(code){return code==='ALL'?'全球':code==='CN'?'中國大陸':countries[code]||code}
function basic(s,knownDay){
  const hit=cached(basicCache,s);if(hit)return hit
  // Only month() supplies knownDay, from a canonical date constructed here.
  if(typeof knownDay==='number'){if(s<data.minDate||s>data.maxDate)return null}else if(!valid(s))return null
  const day=typeof knownDay==='number'?knownDay:epoch(s),weekday=(day+4)%7,index=findStart(day),l=lunarAt(day,index),schedule=data.cn[s],covered=data.cnYears.indexOf(Number(s.slice(0,4)))>=0
  const next=data.lunarStarts[index+1],eve=next&&!next[2]&&next[1]===1&&startDays[index+1]===day+1
  const restKind=schedule?(schedule[0]==='rest'?'holiday':'work'):!covered?'unknown':weekday===0||weekday===6?'weekend':'weekday'
  return remember(basicCache,s,{date:s,day:Number(s.slice(8)),epoch:day,weekday:weekday,lunar:l,term:data.terms[s]||'',badge:schedule?(schedule[0]==='rest'?'休':'班'):'',status:schedule?(schedule[0]==='rest'?'大陸放假 · ':'大陸補班 · ')+schedule[1]:covered?(weekday===0||weekday===6?'大陸一般週末':'大陸一般平日'):'大陸調休安排未收錄',covered:covered,eve:!!eve,restKind:restKind},64)
}
// Official holiday blocks only: never extend across an unknown year or infer leave.
function restInfo(s){
  const b=basic(s);if(!b)return null
  const entry=data.cn[s],run=holidayByDate[s]
  return {kind:b.restKind,covered:b.covered,name:run?run.name:entry&&entry[0]==='work'?entry[1]:'',start:run?run.start:'',end:run?run.end:'',days:run?run.days:0,index:run?b.epoch-holidayStartDays[run.start]+1:0}
}
function monthBreaks(s){const first=s.slice(0,7)+'-01',last=s.slice(0,7)+'-31';return holidayRuns.filter(r=>r.end>=first&&r.start<=last)}
function commonEvents(s,b,o){
  const events=[],schedule=data.cn[s],l=b.lunar
  if(schedule)events.push({n:schedule[1],c:schedule[0]==='rest'?'中國大陸 · 官方放假調休':'中國大陸 · 官方補班',kind:schedule[0]})
  if(l&&!l.leap){const name=lunarFestivals[l.month+'-'+l.day];if(name)events.push({n:name,c:'傳統節日 · 中華文化圈',kind:'traditional'})}
  if(b.eve)events.push({n:'除夕',c:'傳統節日 · 中華文化圈',kind:'traditional'})
  if(data.cnFixed[s.slice(5)])events.push({n:data.cnFixed[s.slice(5)],c:'中國大陸節日 · 不表示放假',kind:'regional'})
  if(o.showGlobal!==false&&data.fixed[s.slice(5)])events.push({n:data.fixed[s.slice(5)],c:'國際紀念日／民俗節日 · 不代表大陸放假',kind:'global'})
  if(o.showTerms!==false&&b.term)events.push({n:b.term,c:'二十四節氣 · UTC+8 曆日',kind:'solar'})
  return events
}
function translate(n){return translations[n]||n}
function codeHas(codes,country){let i=codes.indexOf(country);while(i>=0){if(i%2===0)return true;i=codes.indexOf(country,i+1)}return false}
// Month markers scan integer-name/fixed-width-country indexes without JSON.parse.
function firstWorldName(s,country){const raw=world.days[s];if(!raw||country==='CN')return '';let start=0;while(start<raw.length){const comma=raw.indexOf(',',start),end=raw.indexOf(';',comma),stop=end<0?raw.length:end;if(country==='ALL'||codeHas(raw.slice(comma+1,stop),country))return translate(world.names[Number(raw.slice(start,comma))]);start=stop+1}return ''}
function shortName(name,kind){if(kind==='work')return '補班';if(kind==='rest')return name.replace('節','').slice(0,2);const n=name.replace(/^(世界|國際|聯合國)/,'').replace('／','');return /[^\x00-\x7F]/.test(n)?n.slice(0,2):n.slice(0,3)}
// A calendar cell needs only the first title, not an array of full event objects.
function markerFromBasic(s,b,o){
  const schedule=data.cn[s],l=b.lunar,md=s.slice(5)
  let name='',kind=''
  if(schedule){name=schedule[1];kind=schedule[0]}
  else if(l&&!l.leap&&lunarFestivals[l.month+'-'+l.day]){name=lunarFestivals[l.month+'-'+l.day];kind='traditional'}
  else if(b.eve){name='除夕';kind='traditional'}
  else if(data.cnFixed[md]){name=data.cnFixed[md];kind='regional'}
  else if(o.showGlobal!==false&&data.fixed[md]){name=data.fixed[md];kind='global'}
  else if(o.showTerms!==false&&b.term){name=b.term;kind='solar'}
  else{name=firstWorldName(s,o.country);if(name)kind='national'}
  return {name:name,short:name?shortName(name,kind):'',kind:kind}
}
function marker(s,options){const b=basic(s);return b?markerFromBasic(s,b,options||defaults()):null}
function preview(s,options){
  const b=basic(s);if(!b)return null;const o=options||defaults(),l=b.lunar,md=s.slice(5)
  let count=0
  if(data.cn[s])count++
  if(l&&!l.leap&&lunarFestivals[l.month+'-'+l.day])count++
  if(b.eve)count++
  if(data.cnFixed[md])count++
  if(o.showGlobal!==false&&data.fixed[md])count++
  if(o.showTerms!==false&&b.term)count++
  const raw=world.days[s]
  if(raw&&o.country!=='CN'){
    let start=0
    while(start<raw.length){const comma=raw.indexOf(',',start),end=raw.indexOf(';',comma),stop=end<0?raw.length:end;if(o.country==='ALL'||codeHas(raw.slice(comma+1,stop),o.country))count++;start=stop+1}
  }
  const mark=markerFromBasic(s,b,o)
  return {date:b.date,weekday:b.weekday,lunar:b.lunar,status:b.status,firstEvent:mark.name,eventCount:count}
}
function decodedWorld(s){let value=cached(worldCache,s);if(value)return value;const raw=world.days[s];value=[];if(raw){worldDecodes++;const groups=raw.split(';');for(let i=0;i<groups.length;i++){const comma=groups[i].indexOf(',');value.push([world.names[Number(groups[i].slice(0,comma))],groups[i].slice(comma+1).match(/.{2}/g)||[]])}}return remember(worldCache,s,value,8)}
function info(s,options,compact){
  const opts=options||defaults(),b=basic(s);if(!b)return null
  const events=commonEvents(s,b,opts),groups=opts.country==='CN'?[]:decodedWorld(s)
  for(let i=0;i<groups.length;i++){const g=groups[i],codes=opts.country==='ALL'?g[1]:g[1].filter(c=>c===opts.country);if(codes.length){const visible=compact?codes.slice(0,2):codes;events.push({n:translate(g[0]),original:g[0],codes:codes,c:visible.map(countryName).join('、')+(compact&&codes.length>2?' 等'+codes.length+'地區':''),kind:'national'})}}
  return {date:b.date,day:b.day,weekday:b.weekday,lunar:b.lunar,events:events,term:b.term,badge:b.badge,status:b.status,covered:b.covered}
}
function month(s,monday,options){
  const o=options||defaults(),cacheKey=s.slice(0,7)+'|'+(monday?1:0)+'|'+settingsKey(o),hit=cached(monthCache,cacheKey);if(hit)return hit
  const first=s.slice(0,7)+'-01',firstDay=epoch(first),offset=((firstDay+4)%7+(monday?6:0))%7,result=[]
  for(let i=0;i<42;i++){const day=firstDay+i-offset,date=key(new Date(day*DAY)),b=basic(date,day),m=b?markerFromBasic(date,b,o):null;result.push({key:date,day:Number(date.slice(8)),inside:date.slice(0,7)===s.slice(0,7),valid:!!b,lunar:b&&b.lunar?b.lunar.short:'',badge:b?b.badge:'',weekday:b?b.weekday:0,restKind:b?b.restKind:'unknown',hasEvent:!!(m&&m.name),markerName:m?m.name:'',markerShort:m?m.short:'',markerKind:m?m.kind:''})}
  return remember(monthCache,cacheKey,result,3)
}
function shiftMonth(s,delta){const d=new Date(epoch(s.slice(0,7)+'-01')*DAY);d.setUTCMonth(d.getUTCMonth()+delta);const v=key(d);return v>=data.minDate&&v<=data.maxDate?v:null}
function resetCaches(){basicCache=createCache();monthCache=createCache();worldCache=createCache();worldDecodes=0}
function cacheInfo(){return {basic:basicCache.size,months:monthCache.size,worldDays:worldCache.size,worldDecodes:worldDecodes}}
module.exports={data:data,defaults:defaults,validSettings:validSettings,sameSettings:sameSettings,settingsKey:settingsKey,countryName:countryName,valid:valid,today:today,lunar:lunar,info:info,preview:preview,month:month,shiftMonth:shiftMonth,marker:marker,shortName:shortName,restInfo:restInfo,monthBreaks:monthBreaks,resetCaches:resetCaches,cacheInfo:cacheInfo}
