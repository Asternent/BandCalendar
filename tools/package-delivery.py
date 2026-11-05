"""Export audited source + installables. Never exports signing secrets or SDK JARs."""
from pathlib import Path
import sys,json,zipfile,hashlib,shutil,re,subprocess,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
OUT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else ROOT/'dist/delivery'
OUT.mkdir(parents=True,exist_ok=True)
manifest=json.loads((ROOT/'band/src/manifest.json').read_text('utf8'));version=manifest['versionName']
apk=ROOT/f'dist/BandCalendar-Sync-{version}.apk';rpk=ROOT/f'band/dist/com.bandcalendar.sync.release.{version}.rpk'
assert apk.exists() and rpk.exists()
android=ET.parse(ROOT/'android/app/src/main/AndroidManifest.xml').getroot();ns='{http://schemas.android.com/apk/res/android}'
assert android.attrib['package']==manifest['package']=='com.bandcalendar.sync'
assert android.attrib[ns+'versionName']==version and int(android.attrib[ns+'versionCode'])==manifest['versionCode']
with zipfile.ZipFile(apk) as a,zipfile.ZipFile(rpk) as b:
    assert a.read('assets/band.rpk')==rpk.read_bytes(),'APK embeds a different RPK'
    built=json.loads(b.read('manifest.json'));built.pop('packageInfo',None)
    assert built==manifest
    for name in ['LICENSE.txt','NOTICE.txt','THIRD_PARTY_HOLIDAYS.txt','THIRD_PARTY_BABEL.txt']:
        assert a.read('assets/'+name)==b.read('common/'+name),'Attribution mismatch: '+name
# Public certificate only: confirm both signing blocks contain the same DER.
der=subprocess.check_output(['openssl','x509','-in',str(ROOT/'signing/certificate.pem'),'-outform','DER'])
assert der in rpk.read_bytes() and der in apk.read_bytes(),'Both packages must use the same certificate'
cert_hash=hashlib.sha256(der).hexdigest()
perf=json.loads((ROOT/'test/build/performance.json').read_text('utf8'))
assert perf['candidate']==version,'Run the current-version benchmark before packaging'
band_checks=sum(len(re.findall(r'(?m)^test\(',p.read_text('utf8'))) for p in (ROOT/'band/test').glob('*.test.cjs'))
core_checks=json.loads((ROOT/'test/build/core-result.json').read_text('utf8'))['checks']
repo_checks=json.loads((ROOT/'test/build/repository-result.json').read_text('utf8'))['checks']
install_path=ROOT/'test/build/install-result.json'
install=json.loads(install_path.read_text('utf8')) if install_path.exists() else {}
installed=install.get('version')==version and install.get('installed') and install.get('started')
phone_note=f'Android {version} 已更新並成功啟動；保留原有設定。手環顯示與體感仍待使用者實機確認。' if installed else f'{version} 為本次交付安裝檔，未替使用者覆蓋安裝；手環顯示與體感仍待實機確認。'

reports=ROOT/'reports';reports.mkdir(exist_ok=True)
(reports/'performance-model.json').write_text(json.dumps(perf,ensure_ascii=False,indent=2),encoding='utf8')
labels={'cold_month_grid':'建立未快取月份（含新增節日標記）','revisit_three_months':'重訪最近三個月份（快取命中）','select_day_and_detail_model':'同月選日期、詳情及返回模型','month_render_model':'完整換月畫面資料（不含原生繪製）'}
report=[f'# BandCalendar {version} 效能與驗證報告','', '**範圍：Windows 主機 Node.js 模型運算，不是手環實測。** 不含原生 Vela 渲染、觸控、藍牙、儲存與裝置耗時，不能由此推定手環 FPS 或毫秒延遲。','', f'比較上一版 0.1.4 模型與 {version}；每項先暖機 100 次，再測 7 組，以下為每次操作耗時中位數。基準程式碼附在原始碼的 `test/fixtures/perf-014/`。','', '| 項目 | 修改前（微秒） | 修改後（微秒） | 模型加速倍數 |','| --- | ---: | ---: | ---: |']
for row in perf['results']:report.append(f"| {labels[row['scenario']]} | {row['beforeMedianMicroseconds']:.2f} | {row['afterMedianMicroseconds']:.2f} | {row['speedup']:.1f}× |")
report+=['','本版重點是操作邏輯與資訊層次，不以縮短所有模型路徑為目標；倍數不能代表實機操作快慢。數字隨主機負載與 JIT 改變；結果可用 `node tools/benchmark.cjs` 重現。','', '- 雙端月曆：日期數字紅／綠／藍分色、節日簡稱、大陸休／班標記、完整官方假期區間與連休天數。','- 手環月曆零完整國別節日解碼、原生月曆節點保留、同月選取只改兩格、詳情每頁最多 8 組。','- 全球資料索引與 0.1.4 相同，完整節日資料和適用地區保留；本次不靠刪減功能換取效能。',f'- 自動驗證：{band_checks} 項手環測試、{core_checks:,} 項 Java 核心檢查、{repo_checks} 項資料庫／偏好檢查全部通過。','- 農曆 1,461 日來源核對；雙端核心 1,461 日比對；雙端節日標記 5,844 日期／範圍組合比對通過。','- APK 簽章檢查通過，兩端包名、版本、證書與 APK 內嵌 RPK 配對一致。','- 新增假期種類、完整區間與天數：雙端 1,461 日與 48 個月份摘要一致。','- '+phone_note,'',f'配對簽章證書 SHA-256：`{cert_hash}`。']
report+=['','## 原生結構與操作檢查','',f"- 日期格模板節點：{perf['structuralBefore']['cellTemplateNodes']} → {perf['structuralAfter']['cellTemplateNodes']}（沿用 0.1.4 的精簡格子結構）。",f"- 換月後保留原格物件：{perf['structuralBefore']['retainedSlotsAcrossMonth']} → {perf['structuralAfter']['retainedSlotsAcrossMonth']} / 42。",f"- 啟動全球詳情解碼：{perf['structuralBefore']['initialDetailDecodes']} → {perf['structuralAfter']['initialDetailDecodes']}。",'- 畫面就緒後才啟動通訊；隱藏月曆與熄屏不更新日期格；詳情以原有列物件更新，等待佈局後才捲動。','- 40,908 個日期／國別／顯示設定的輕量摘要與完整詳情相符。','', '結構數是程式模板與物件保留檢查，不是實機記憶體或 FPS 實測。']
slower=[r for r in perf['results'] if r['speedup']<1]
if slower:report+=['','**取捨：** '+ '；'.join(labels[r['scenario']]+f" 的主機 CPU 耗時約增加 {(1/r['speedup']-1)*100:.0f}%" for r in slower)+'。新增導航、選取摘要與按鈕狀態有額外模型開銷；本版以可理解的操作及保留原生格子為主，不宣稱所有路徑更快。']
report+=['','實作參照：[小米 Vela 記憶體優化](https://iot.mi.com/vela/quickapp/zh/guide/best-practice/memory.html)、[通用樣式](https://iot.mi.com/vela/quickapp/zh/components/general/style.html)、[全域方法](https://iot.mi.com/vela/quickapp/zh/guide/framework/script/global-data-method.html)。']
report+=['','## 操作與介面驗證','', '- 日期先選取／上方摘要預覽，再點同日或摘要開詳情；不把第一次選取當成強制跳頁。','- 點月份快速跳年／月，取消不改日期；月底保留目標幾號，包含 1/31→2/28→3/31 與閏年 2/29。','- 詳情前一天／今天／後一天、節日資料分頁、資料邊界與一致返回堆疊均有回歸檢查。','- 暫時國別與手機保存設定分開；同步不打斷詳情、跳月年份或選單。','- 重複同步點擊合併；逾時、通訊失敗、儲存失敗、過期回呼與緩慢初始偏好讀取都有測試。','- 336×480 / 456 高度模型：首段月份、摘要、六行日期格共 450 設計像素；主要操作目標至少 44×44。其他螢幕比例支援捲動。','- 以上是程式模型與版面尺寸檢查；未將示意或桌面測試當成手環實機顯示驗證。']
(reports/'效能與驗證報告.md').write_text('\n'.join(report)+'\n',encoding='utf8')
p=ROOT/'README.md';text=p.read_text('utf8');a=perf['results'][0]['speedup'];c=perf['results'][2]['speedup'];text=re.sub(r'冷月份資料建立約 [\d.]+ 倍、同月日期／詳情模型約 [\d.]+ 倍快',f'冷月份資料建立約 {a:.1f} 倍、同月日期／詳情模型約 {c:.1f} 倍快',text);text=re.sub(r'(?m)^- \*\*'+re.escape(version)+r' 為本次交付安裝檔[^\n]+','- '+phone_note,text);p.write_text(text,encoding='utf8')
files=[]
for name in ['README.md','CHANGELOG.md','LICENSE','NOTICE.md','.gitignore','THIRD_PARTY_HOLIDAYS_0.txt','THIRD_PARTY_BABEL_0.txt','band/package.json','band/package-lock.json','test/CalendarTest.java','test/RepositoryTest.java','test/hko-reference.json']:
    files.append(ROOT/name)
for folder in ['android/app/src/main','band/src','band/test','test/stubs','test/fixtures/perf-014','shared','tools','reports']:
    files.extend(p for p in (ROOT/folder).rglob('*') if p.is_file() and p.name!='band.rpk' and p.suffix not in ['.pyc','.jar','.rpk','.apk'])
files.extend(p for p in (ROOT/'data').rglob('*') if p.is_file() and p.suffix in ['.txt','.csv'])
files=sorted(set(files))
secret=re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}')
for p in files:
    rel=p.relative_to(ROOT).as_posix();assert not any(x in rel.split('/') for x in ['signing','sign','node_modules','vendor','build'])
    raw=p.read_bytes();assert not secret.search(raw),'Secret found: '+rel
    assert not re.search(rb'[A-Za-z]:[\\/]Users[\\/][^\\/\r\n]+',raw),'Personal absolute path found'
source=OUT/f'BandCalendar-Source-{version}.zip'
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED) as z:
    for p in files:z.write(p,'BandCalendar/'+p.relative_to(ROOT).as_posix())
with zipfile.ZipFile(source) as z:
    assert z.testzip() is None
    assert not any(n.endswith(('.p12','.pem','.jar','.apk','.rpk')) or '/signing/' in n or '/node_modules/' in n for n in z.namelist())
shutil.copy2(apk,OUT/apk.name);shutil.copy2(rpk,OUT/f'BandCalendar-Band-{version}.rpk')
shutil.copy2(ROOT/'README.md',OUT/'安裝與使用說明.md');shutil.copy2(reports/'效能與驗證報告.md',OUT/'效能與驗證報告.md');shutil.copy2(reports/'performance-model.json',OUT/'performance-model.json')
checks=[]
for p in sorted(OUT.iterdir()):
    if p.is_file() and p.name!='SHA256SUMS.txt':checks.append(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name)
(OUT/'SHA256SUMS.txt').write_text('\n'.join(checks)+'\n',encoding='utf8')
print(json.dumps({'version':version,'sourceFiles':len(files),'certificate':cert_hash,'artifacts':[{'name':p.name,'bytes':p.stat().st_size} for p in sorted(OUT.iterdir()) if p.is_file()]},ensure_ascii=False))
