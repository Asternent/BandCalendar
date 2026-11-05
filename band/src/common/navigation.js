const calendar=require('./calendar')
function pad(n){return ('0'+n).slice(-2)}
function dateInMonth(month,preferred){
  if(typeof month!=='string'||!/^\d{4}-\d{2}$/.test(month)||!calendar.valid(month+'-01'))return null
  const day=Number(preferred);if(!isFinite(day)||day<1||Math.floor(day)!==day)return null
  const year=Number(month.slice(0,4)),m=Number(month.slice(5)),last=new Date(Date.UTC(year,m,0)).getUTCDate()
  const result=month+'-'+pad(Math.min(day,last));return calendar.valid(result)?result:null
}
function moveMonth(date,delta,preferred){
  if(!calendar.valid(date)||!isFinite(delta)||Math.floor(delta)!==delta)return null
  const first=calendar.shiftMonth(date,delta);return first?dateInMonth(first.slice(0,7),preferred||Number(date.slice(8))):null
}
function moveDay(date,delta){
  if(!calendar.valid(date)||!isFinite(delta)||Math.floor(delta)!==delta)return null
  const d=new Date(date+'T12:00:00Z');d.setUTCDate(d.getUTCDate()+delta)
  const result=d.getUTCFullYear()+'-'+pad(d.getUTCMonth()+1)+'-'+pad(d.getUTCDate());return calendar.valid(result)?result:null
}
module.exports={dateInMonth:dateInMonth,moveMonth:moveMonth,moveDay:moveDay}
