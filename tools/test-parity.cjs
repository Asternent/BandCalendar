const fs=require('node:fs'),assert=require('node:assert/strict'),c=require('../band/src/common/calendar');
const rows=JSON.parse(fs.readFileSync('test/build/java-parity.json','utf8'));
for(const raw of rows){const j={...raw},js=c.info(j.date);delete js.events;delete j.events;assert.deepEqual(js,j,j.date);const opts={...c.defaults(),country:'CN'},a=c.info(j.date,opts).events;assert.deepEqual(a,raw.events,j.date+' events');}
console.log('Java/JS parity: all 1461 civil dates and core events agree.');
const markers=JSON.parse(fs.readFileSync('test/build/java-markers.json','utf8'));for(const row of markers)assert.deepEqual(c.marker(row.date,{...c.defaults(),country:row.country}),row.marker,row.date+' '+row.country);console.log('Calendar labels: all 5844 Android/band date-country markers agree.');
const rest=JSON.parse(fs.readFileSync('test/build/java-rest.json','utf8'));for(const row of rest.dates)assert.deepEqual(c.restInfo(row.date),row.rest,row.date+' rest');for(const row of rest.months)assert.deepEqual(c.monthBreaks(row.month),row.runs,row.month+' holiday runs');console.log('Rest colours/durations: all 1461 dates and 48 month summaries agree across Android/band.');
