import com.bandcalendar.sync.*;
import android.content.*;
import android.content.res.AssetManager;
import org.json.*;
import java.util.*;
public class RepositoryTest {
  static int checks=0;static void check(boolean c,String m){checks++;if(!c)throw new AssertionError(m);}
  static class Prefs implements SharedPreferences,SharedPreferences.Editor{Map<String,Object> v=new HashMap<>();public String getString(String k,String d){return v.containsKey(k)?(String)v.get(k):d;}public boolean getBoolean(String k,boolean d){return v.containsKey(k)?(Boolean)v.get(k):d;}public Editor edit(){return this;}public Editor putString(String k,String x){v.put(k,x);return this;}public Editor putBoolean(String k,boolean x){v.put(k,x);return this;}public void apply(){}}
  static class FakeContext extends Context{Prefs prefs=new Prefs();public SharedPreferences getSharedPreferences(String n,int m){return prefs;}public AssetManager getAssets(){return new AssetManager();}}
  static boolean contains(JSONObject info,String title)throws Exception{JSONArray es=info.getJSONArray("events");for(int i=0;i<es.length();i++)if(es.getJSONObject(i).getString("n").equals(title))return true;return false;}
  public static void main(String[] args)throws Exception{
    FakeContext context=new FakeContext();CalendarRepository r=new CalendarRepository(context);check(r.countries().size()==251,"249 worldwide + China + ALL");check(r.country().equals("ALL"),"global default");
    JSONObject christmas=r.info("2026-12-25");check(contains(christmas,"聖誕節"),"global Christmas");check(christmas.getString("badge").isEmpty(),"foreign festival not rest");r.selectCountry("US");check(contains(r.info("2026-11-26"),"感恩節"),"US Thanksgiving");r.selectCountry("JP");check(!contains(r.info("2026-11-26"),"感恩節"),"Japan does not inherit US Thanksgiving");check(r.info("2026-02-14").getString("badge").equals("班"),"China badge unaffected by Japan filter");
    r.selectCountry("CN");check(contains(r.info("2027-10-01"),"國慶節"),"known China festival even without future schedule");check(r.info("2027-10-01").getString("badge").isEmpty(),"no speculative future holiday");context.prefs.putBoolean("showTerms",false).apply();check(!contains(r.info("2026-04-05"),"清明"),"term toggle");check(r.info("2026-04-05").getString("badge").equals("休"),"schedule remains with term hidden");context.prefs.putBoolean("monday",false).apply();check(!r.settings().getBoolean("monday"),"synced week origin");check(r.settings().getString("country").equals("CN"),"synced region");
    boolean threw=false;try{r.selectCountry("../../x");}catch(IllegalArgumentException e){threw=true;}check(threw,"invalid country rejected");context.prefs.putString("country","bad").apply();check(r.country().equals("ALL"),"corrupt stored country safely defaults");
    r.selectCountry("ALL");int totals=0;for(String key:r.core.month("2026-11-01",true)){JSONObject d=r.info(key);if(d!=null)totals+=d.getJSONArray("events").length();}check(totals>0,"full month repository events");
    context.prefs.putBoolean("showTerms",true).putBoolean("showGlobal",true).putBoolean("monday",true).apply();check(r.marker("2026-12-25").getString("short").equals("聖誕"),"Christmas cell marker");check(r.marker("2026-02-14").getString("short").equals("補班"),"work marker not confused with holiday");check(r.marker("2027-10-01").getString("short").equals("國慶"),"future festival without speculative rest flag");
    JSONArray markerRows=new JSONArray();for(String country:new String[]{"ALL","CN","US","JP"}){r.selectCountry(country);for(java.time.LocalDate day=java.time.LocalDate.of(2025,1,1);!day.isAfter(java.time.LocalDate.of(2028,12,31));day=day.plusDays(1)){String key=day.toString();markerRows.put(new JSONObject().put("date",key).put("country",country).put("marker",r.marker(key)));}}java.nio.file.Files.writeString(java.nio.file.Path.of("test/build/java-markers.json"),markerRows.toString());
    System.out.println("CalendarRepository: "+checks+" checks passed.");
    java.nio.file.Files.writeString(java.nio.file.Path.of("test/build/repository-result.json"),new JSONObject().put("checks",checks).toString());
  }
}
