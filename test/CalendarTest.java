import com.bandcalendar.sync.CalendarCore;
import org.json.*;
import java.nio.file.*;
import java.time.*;
public class CalendarTest {
  static int checks=0;
  static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
  public static void main(String[] args)throws Exception{
    CalendarCore c=new CalendarCore(new JSONObject(Files.readString(Path.of("shared/core-data.json"))));
    JSONArray rows=new JSONArray(Files.readString(Path.of("test/hko-reference.json")));
    for(int i=0;i<rows.length();i++){JSONArray r=rows.getJSONArray(i);JSONObject l=c.lunar(r.getString(0));check(l.getInt("month")==r.getInt(1)&&l.getBoolean("leap")==r.getBoolean(2)&&l.getInt("day")==r.getInt(3)&&l.getString("year").equals(r.getString(4))&&l.getString("zodiac").equals(r.getString(5)),r.getString(0));}
    for(String key:new String[]{"2026-02-30","2025-02-29","2029-01-01","2024-12-31","2026-1-01"})check(!c.valid(key),"reject "+key);
    check(c.valid("2028-02-29"),"leap day");check(c.month("2026-11-03",true).get(0).equals("2026-10-26"),"Monday origin");check(c.month("2026-11-03",false).get(0).equals("2026-11-01"),"Sunday origin");check(c.month("2026-11-03",true).size()==42,"42 cells");check(c.shiftMonth("2026-12-01",1).equals("2027-01-01"),"rollover");check(c.shiftMonth("2025-01-01",-1)==null,"lower bound");
    check(c.info("2026-02-14",true,true).getString("badge").equals("班"),"work override");check(c.info("2026-02-15",true,true).getString("badge").equals("休"),"rest override");check(!c.info("2027-01-01",true,true).getBoolean("covered"),"unverified future");check(c.info("2026-12-25",true,true).getString("badge").isEmpty(),"foreign holiday not China rest");
    String[] starts={"2026-01-01","2026-02-15","2026-04-04","2026-05-01","2026-06-19","2026-09-25","2026-10-01"};int[] lengths={3,9,3,5,3,3,7};for(int i=0;i<starts.length;i++)check(c.restInfo(starts[i]).getInt("days")==lengths[i],"official duration "+starts[i]);check(c.restInfo("2026-02-23").getInt("index")==9,"last holiday day index");check(c.restInfo("2026-02-14").getString("kind").equals("work"),"compensatory Saturday is work, not weekend");check(c.restInfo("2026-02-07").getString("kind").equals("weekend"),"ordinary weekend");check(c.restInfo("2027-01-02").getString("kind").equals("unknown"),"future weekend not inferred");check(c.monthBreaks("2027-01-01").length()==0,"no future vacation length");check(c.monthBreaks("2025-05-01").length()==2,"two holidays in one month");check(c.monthBreaks("2025-06-01").getJSONObject(0).getInt("days")==3,"cross-month full length");check(c.restInfo("2026-12-25").getString("kind").equals("weekday"),"foreign festival not mainland holiday");
    JSONArray restRows=new JSONArray(),monthRows=new JSONArray();for(int i=0;i<rows.length();i++){String date=rows.getJSONArray(i).getString(0);restRows.put(new JSONObject().put("date",date).put("rest",c.restInfo(date)));if(date.endsWith("-01"))monthRows.put(new JSONObject().put("month",date).put("runs",c.monthBreaks(date)));}Files.writeString(Path.of("test/build/java-rest.json"),new JSONObject().put("dates",restRows).put("months",monthRows).toString());
    JSONArray parity=new JSONArray();for(int i=0;i<rows.length();i++){String date=rows.getJSONArray(i).getString(0);parity.put(c.info(date,true,true));}Files.writeString(Path.of("test/build/java-parity.json"),parity.toString());
    System.out.println("CalendarCore: "+checks+" checks passed; 1461-day Java/JS parity export written.");
    Files.writeString(Path.of("test/build/core-result.json"),new JSONObject().put("checks",checks).toString());
  }
}
