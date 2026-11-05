const calendar=require('./calendar')
function parse(raw){try{if(typeof raw==='string'){if(raw.length>2048)return null;raw=JSON.parse(raw)}return raw&&typeof raw==='object'?raw:null}catch(e){return null}}
function validPacket(p){return p&&p.type==='settings'&&p.v===1&&typeof p.id==='string'&&/^\d{1,16}$/.test(p.id)&&Number(p.id)>0&&calendar.validSettings(p.settings)}
function validSaved(s){return s&&validPacket(s)}
function ack(p){return {type:'ack',v:1,id:p.id}}
module.exports={parse:parse,validPacket:validPacket,validSaved:validSaved,ack:ack}
