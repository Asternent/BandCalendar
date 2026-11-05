package com.bandcalendar.sync;
import android.content.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CalendarRepository {
  final Context context;
  final SharedPreferences prefs;
  public final CalendarCore core;
  public final JSONObject metadata;
  private JSONObject world=new JSONObject();
  private int loadedYear=-1;
  private final LinkedHashMap<String,String> countryNames=new LinkedHashMap<>();
  private static final Map<String,String> TRANSLATIONS=new HashMap<>();
  static{String[] k={"New Year's Day","Christmas Day","Christmas","Easter Sunday","Easter Monday","Good Friday","Labor Day","Labour Day","National Day","Independence Day","Thanksgiving Day","New Year's Eve"},v={"元旦","聖誕節","聖誕節","復活節","復活節星期一","耶穌受難日","勞動節","勞動節","國慶日","獨立日","感恩節","除夕／跨年夜"};for(int i=0;i<k.length;i++)TRANSLATIONS.put(k[i],v[i]);}
  public CalendarRepository(Context c){context=c;prefs=c.getSharedPreferences("calendar-v1",0);try{core=new CalendarCore(read("core-data.json"));metadata=read("world-metadata.json");countryNames.put("ALL","全球");countryNames.put("CN","中國大陸");JSONArray a=metadata.getJSONArray("countries");for(int i=0;i<a.length();i++){JSONArray x=a.getJSONArray(i);countryNames.put(x.getString(0),x.getString(1));}}catch(Exception e){throw new IllegalStateException("Calendar assets missing",e);}}
  private JSONObject read(String asset)throws Exception{try(InputStream in=context.getAssets().open(asset);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);return new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));}}
  public String country(){String c=prefs.getString("country","ALL");return countryNames.containsKey(c)?c:"ALL";}
  public String countryName(String c){return countryNames.containsKey(c)?countryNames.get(c):c;}
  public Map<String,String> countries(){return Collections.unmodifiableMap(countryNames);}
  public boolean monday(){return prefs.getBoolean("monday",true);}
  public boolean showGlobal(){return prefs.getBoolean("showGlobal",true);}
  public boolean showTerms(){return prefs.getBoolean("showTerms",true);}
  public void selectCountry(String c){if(!countryNames.containsKey(c))throw new IllegalArgumentException("Unknown country");prefs.edit().putString("country",c).apply();}
  public JSONObject settings()throws JSONException{return new JSONObject().put("country",country()).put("monday",monday()).put("showGlobal",showGlobal()).put("showTerms",showTerms());}
  public static String shortName(String name,String kind){if(kind.equals("work"))return "補班";String n=kind.equals("rest")?name.replaceFirst("節",""):name.replaceFirst("^(世界|國際|聯合國)","").replaceFirst("／","");int max=kind.equals("rest")||n.matches(".*[^\\x00-\\x7F].*")?2:3;return n.substring(0,Math.min(n.length(),max));}
  public JSONObject marker(String date)throws Exception{JSONObject d=info(date);JSONArray events=d==null?null:d.optJSONArray("events");if(events==null||events.length()==0)return new JSONObject().put("name","").put("short","").put("kind","");JSONObject first=events.getJSONObject(0);return new JSONObject().put("name",first.getString("n")).put("short",shortName(first.getString("n"),first.getString("kind"))).put("kind",first.getString("kind"));}
  public synchronized JSONObject info(String date)throws Exception{
    JSONObject info=core.info(date,showGlobal(),showTerms());if(info==null)return null;int year=Integer.parseInt(date.substring(0,4));if(year!=loadedYear){world=read("world-"+year+".json");loadedYear=year;}
    JSONArray raw=world.optJSONArray(date),events=info.getJSONArray("events");LinkedHashMap<String,List<String>> groups=new LinkedHashMap<>();
    if(raw!=null)for(int i=0;i<raw.length();i++){JSONArray e=raw.getJSONArray(i);String code=e.getString(0),name=e.getString(1);if(!country().equals("ALL")&&!country().equals(code))continue;if(!groups.containsKey(name))groups.put(name,new ArrayList<>());groups.get(name).add(code);}
    for(Map.Entry<String,List<String>> e:groups.entrySet()){JSONArray codes=new JSONArray();List<String> names=new ArrayList<>();for(String code:e.getValue()){codes.put(code);names.add(countryName(code));}String n=TRANSLATIONS.containsKey(e.getKey())?TRANSLATIONS.get(e.getKey()):e.getKey();events.put(CalendarCore.event(n,String.join("、",names),"national").put("original",e.getKey()).put("codes",codes));}
    return info;
  }
}
