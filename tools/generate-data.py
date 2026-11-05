"""Reproducible offline facts: HKO lunar/solar dates + Python holidays 0.87.
CN rest/work overrides are transcribed ONLY from the cited official notices.
Nothing in the worldwide dataset changes China's rest/work badges.
"""
from pathlib import Path
import csv,io,json,re,urllib.request,datetime as dt,warnings,importlib.metadata as md
import holidays
from babel import Locale
ROOT=Path(__file__).resolve().parents[1]
def dump(p,x):
    p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(x,ensure_ascii=False,separators=(',',':')),encoding='utf8')
def fetch(name,url):
    p=ROOT/'data/sources'/name
    if not p.exists():
        req=urllib.request.Request(url,headers={'User-Agent':'BandCalendar/0.1 (offline calendar build)'})
        p.write_bytes(urllib.request.urlopen(req,timeout=60).read())
    return p.read_text('utf-8-sig')
months=['正月','二月','三月','四月','五月','六月','七月','八月','九月','十月','十一月','十二月']
days=['初一','初二','初三','初四','初五','初六','初七','初八','初九','初十','十一','十二','十三','十四','十五','十六','十七','十八','十九','二十','廿一','廿二','廿三','廿四','廿五','廿六','廿七','廿八','廿九','三十']
starts=[];terms={};check=[]
for year in range(2024,2029):
    url=f'https://data.weather.gov.hk/weatherAPI/hko_data/calendar/nongli_calendar_{year}.csv'
    raw=fetch(f'hko-{year}.csv',url)
    for row in csv.DictReader(io.StringIO(raw)):
        date=dt.datetime.strptime(row['Gregorian Date'],'%d-%b-%y').date().isoformat()
        month=row['Lunar month'];day=row['Lunar Date'];leap='閏' in month;month_num=months.index(month.replace('閏',''))+1
        if day=='初一':starts.append([date,month_num,leap,row['Chinese year (Gan-Zhi)'],row['Chinese year (Zodiac)']])
        if year>=2025:check.append([date,month_num,leap,days.index(day)+1,row['Chinese year (Gan-Zhi)'],row['Chinese year (Zodiac)']])
    text=fetch(f'hko-terms-{year}.txt',f'https://www.hko.gov.hk/tc/gts/time/calendar/text/files/T{year}c.txt')
    for line in text.splitlines():
        m=re.match(r'(\d{4})年(\d+)月(\d+)日\s+\S+\s+星期\S\s+(\S+)',line)
        if m and year>=2025:terms[f'{m[1]}-{int(m[2]):02}-{int(m[3]):02}']=m[4]
    if year>=2025:assert len([x for x in terms if x.startswith(str(year))])==24
cn={}
def rest(start,end,title):
    day=dt.date.fromisoformat(start)
    while day<=dt.date.fromisoformat(end):cn[day.isoformat()]=['rest',title];day+=dt.timedelta(days=1)
def work(dates,title):
    for d in dates.split():cn[d]=['work',title]
rest('2025-01-01','2025-01-01','元旦');rest('2025-01-28','2025-02-04','春節');rest('2025-04-04','2025-04-06','清明節');rest('2025-05-01','2025-05-05','勞動節');rest('2025-05-31','2025-06-02','端午節');rest('2025-10-01','2025-10-08','國慶／中秋')
work('2025-01-26 2025-02-08','春節補班');work('2025-04-27','勞動節補班');work('2025-09-28 2025-10-11','國慶／中秋補班')
rest('2026-01-01','2026-01-03','元旦');rest('2026-02-15','2026-02-23','春節');rest('2026-04-04','2026-04-06','清明節');rest('2026-05-01','2026-05-05','勞動節');rest('2026-06-19','2026-06-21','端午節');rest('2026-09-25','2026-09-27','中秋節');rest('2026-10-01','2026-10-07','國慶節')
work('2026-01-04','元旦補班');work('2026-02-14 2026-02-28','春節補班');work('2026-05-09','勞動節補班');work('2026-09-20 2026-10-10','國慶節補班')
# Selected internationally recognised observances; not every local festival.
fixed={'01-01':'元旦','01-04':'世界盲文日','01-24':'國際教育日','02-04':'世界癌症日','02-14':'情人節','02-21':'國際母語日','03-08':'國際婦女節','03-20':'國際幸福日','03-21':'國際森林日','03-22':'世界水日','03-23':'世界氣象日','04-02':'世界自閉症日','04-07':'世界衛生日','04-22':'世界地球日','04-23':'世界圖書與版權日','04-30':'國際爵士樂日','05-01':'勞動節','05-03':'世界新聞自由日','05-15':'國際家庭日','05-17':'世界電信和資訊社會日','05-21':'世界文化多樣性促進對話和發展日','05-22':'國際生物多樣性日','05-31':'世界無菸日','06-01':'國際兒童節（部分地區）','06-05':'世界環境日','06-08':'世界海洋日','06-14':'世界捐血者日','06-20':'世界難民日','06-21':'國際瑜伽日','07-11':'世界人口日','07-18':'納爾遜·曼德拉國際日','07-30':'國際友誼日','08-09':'世界原住民族日','08-12':'國際青年日','08-19':'世界人道主義日','09-05':'國際慈善日','09-08':'國際掃盲日','09-16':'國際保護臭氧層日','09-21':'國際和平日','09-27':'世界旅遊日','09-30':'國際翻譯日','10-01':'國際老年人日','10-02':'國際非暴力日','10-05':'世界教師日','10-10':'世界精神衛生日','10-11':'國際女童日','10-16':'世界糧食日','10-17':'國際消除貧困日','10-24':'聯合國日','10-31':'萬聖節前夜','11-05':'世界海嘯意識日','11-10':'世界科學促進和平與發展日','11-14':'世界糖尿病日','11-16':'國際寬容日','11-19':'世界廁所日','11-20':'世界兒童日','11-21':'世界電視日','11-25':'國際消除對婦女暴力日','12-01':'世界愛滋病日','12-03':'國際身心障礙者日','12-05':'國際志願人員日','12-10':'人權日','12-18':'國際移民日','12-24':'平安夜','12-25':'聖誕節','12-31':'跨年夜'}
core={'minDate':'2025-01-01','maxDate':'2028-12-31','revision':'2026-11-03','lunarStarts':starts,'terms':terms,'fixed':fixed,'cnFixed':{'03-12':'植樹節','05-04':'青年節','06-01':'兒童節','09-10':'教師節','10-01':'國慶節'},'cn':cn,'cnYears':[2025,2026]}
dump(ROOT/'shared/core-data.json',core)
(ROOT/'band/src/common/data.js').write_text('module.exports='+json.dumps(core,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf8')
dump(ROOT/'android/app/src/main/assets/core-data.json',core)
dump(ROOT/'test/hko-reference.json',check)
territories=Locale.parse('zh_Hant').territories
countries=[];all_data={str(y):{} for y in range(2025,2029)};failures=[];counts={}
with warnings.catch_warnings(record=True) as logged:
    warnings.simplefilter('always')
    for code in sorted(holidays.list_supported_countries()):
        if len(code)!=2:continue
        if code=='CN':continue # China rest/work is the official dataset, never estimated by this library.
        try:
            h=holidays.country_holidays(code,years=range(2025,2029),language='en_US',observed=True)
            country_name=territories.get(code,code)
            countries.append([code,str(country_name)])
            for date,name in sorted(h.items()):
                if date.year not in range(2025,2029):continue
                all_data[str(date.year)].setdefault(date.isoformat(),[]).append([code,str(name)])
        except Exception as e:failures.append([code,type(e).__name__,str(e)])
    (ROOT/'data/generation-warnings.txt').write_text('\n'.join(sorted(set(str(w.message) for w in logged))),encoding='utf8')
assert not failures,failures
for year,data in all_data.items():
    for events in data.values():events.sort(key=lambda e:(e[1].lower(),e[0]))
    dump(ROOT/f'android/app/src/main/assets/world-{year}.json',data)
    counts[year]=sum(map(len,data.values()))
# Keep world days as packed strings on the band. Only the opened date is parsed,
# avoiding thousands of live event objects on the small wearable heap.
packed={};names=[];name_ids={}
for data in all_data.values():
    for date,events in data.items():
        groups={}
        for code,name in events:groups.setdefault(name,[]).append(code)
        for name in groups:
            if name not in name_ids:name_ids[name]=len(names);names.append(name)
        packed[date]=';'.join(str(name_ids[name])+','+''.join(codes) for name,codes in groups.items())
(ROOT/'band/src/common/world.js').write_text('module.exports='+json.dumps({'names':names,'days':packed},ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf8')
(ROOT/'band/src/common/countries.js').write_text('module.exports='+json.dumps(dict(countries),ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf8')
metadata={'library':'python-holidays','version':holidays.__version__,'countries':countries,'coverage':'2025–2028；國家／地區層級公眾假期，不含所有地方或宗教節日。未來日期可能調整。','counts':counts,'sources':{'world':'https://holidays.readthedocs.io/en/latest/','observances':'https://www.un.org/en/observances/list-days-weeks','lunar':'https://www.hko.gov.hk/tc/gts/time/conversion.htm','cn2025':'https://www.gov.cn/zhengce/zhengceku/202411/content_6986383.htm','cn2026':'https://www.gov.cn/zhengce/content/202511/content_7047090.htm'}}
dump(ROOT/'android/app/src/main/assets/world-metadata.json',metadata)
dump(ROOT/'shared/world-metadata.json',metadata)
for package in ['holidays','babel']:
    dist=md.distribution(package)
    files=[p for p in dist.files if 'license' in str(p).lower() and str(p).endswith(('LICENSE','LICENSE.txt'))]
    for i,p in enumerate(files):
        dest=ROOT/f'THIRD_PARTY_{package.upper()}_{i}.txt';dest.write_bytes(Path(dist.locate_file(p)).read_bytes())
print(json.dumps({'countries':len(countries),'events':counts,'lunarDaysChecked':len(check),'coreBytes':(ROOT/'shared/core-data.json').stat().st_size},ensure_ascii=False))
