package com.bandcalendar.sync;
import org.json.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Pure date model: all arithmetic uses civil dates, never 24h local timestamps. */
public final class CalendarCore {
  public final JSONObject data;
  private final JSONArray starts;
  private final List<JSONObject> holidayRuns=new ArrayList<>();
  private final Map<String,JSONObject> holidayByDate=new HashMap<>();
  private static final String[] MONTHS={"正月","二月","三月","四月","五月","六月","七月","八月","九月","十月","十一月","臘月"};
  private static final String[] DAYS={"初一","初二","初三","初四","初五","初六","初七","初八","初九","初十","十一","十二","十三","十四","十五","十六","十七","十八","十九","二十","廿一","廿二","廿三","廿四","廿五","廿六","廿七","廿八","廿九","三十"};
  private static final Map<String,String> LUNAR=new HashMap<>();
  static {String[] keys={"1-1","1-15","2-2","5-5","7-7","7-15","8-15","9-9","12-8"};String[] values={"春節","元宵節","龍抬頭","端午節","七夕","中元節","中秋節","重陽節","臘八節"};for(int i=0;i<keys.length;i++)LUNAR.put(keys[i],values[i]);}
  public CalendarCore(JSONObject d){data=d;starts=d.optJSONArray("lunarStarts");try{JSONObject cn=d.getJSONObject("cn");List<String> dates=new ArrayList<>();for(Iterator<String> i=cn.keys();i.hasNext();)dates.add(i.next());Collections.sort(dates);JSONObject run=null;for(String date:dates){JSONArray entry=cn.getJSONArray(date);if(!entry.getString(0).equals("rest"))continue;if(run==null||!LocalDate.parse(run.getString("end")).plusDays(1).toString().equals(date)){run=new JSONObject().put("name",entry.getString(1)).put("start",date).put("end",date).put("days",0);holidayRuns.add(run);}run.put("end",date).put("days",run.getInt("days")+1);holidayByDate.put(date,run);}}catch(JSONException e){throw new IllegalArgumentException("Invalid holiday dataset",e);}}
  public boolean valid(String s){try{return s!=null&&s.matches("\\d{4}-\\d{2}-\\d{2}")&&LocalDate.parse(s).toString().equals(s)&&s.compareTo(data.optString("minDate"))>=0&&s.compareTo(data.optString("maxDate"))<=0;}catch(Exception e){return false;}}
  public JSONObject lunar(String s)throws JSONException{
    if(!valid(s))return null;JSONArray found=null;
    for(int i=0;i<starts.length();i++){JSONArray a=starts.getJSONArray(i);if(a.getString(0).compareTo(s)<=0)found=a;else break;}
    if(found==null)return null;int day=(int)ChronoUnit.DAYS.between(LocalDate.parse(found.getString(0)),LocalDate.parse(s))+1;if(day<1||day>30)return null;
    int month=found.getInt(1);boolean leap=found.getBoolean(2);String prefix=(leap?"閏":"")+MONTHS[month-1];
    return new JSONObject().put("month",month).put("leap",leap).put("day",day).put("year",found.getString(3)).put("zodiac",found.getString(4)).put("text",prefix+DAYS[day-1]).put("short",day==1?prefix:DAYS[day-1]);
  }
  public JSONObject info(String s,boolean global,boolean terms)throws JSONException{
    if(!valid(s))return null;LocalDate date=LocalDate.parse(s);int weekday=date.getDayOfWeek().getValue()%7;JSONObject l=lunar(s);JSONArray events=new JSONArray();
    if(l!=null&&!l.getBoolean("leap")){String name=LUNAR.get(l.getInt("month")+"-"+l.getInt("day"));if(name!=null)events.put(event(name,"傳統節日 · 中華文化圈","traditional"));}
    JSONObject tomorrow=lunar(date.plusDays(1).toString());if(tomorrow!=null&&!tomorrow.getBoolean("leap")&&tomorrow.getInt("month")==1&&tomorrow.getInt("day")==1)events.put(event("除夕","傳統節日 · 中華文化圈","traditional"));
    String regional=data.getJSONObject("cnFixed").optString(s.substring(5));if(!regional.isEmpty())events.put(event(regional,"中國大陸節日 · 不表示放假","regional"));
    String fixed=data.getJSONObject("fixed").optString(s.substring(5)),term=data.getJSONObject("terms").optString(s);
    if(global&&!fixed.isEmpty())events.put(event(fixed,"國際紀念日／民俗節日 · 不代表大陸放假","global"));
    if(terms&&!term.isEmpty())events.put(event(term,"二十四節氣 · UTC+8 曆日","solar"));
    JSONArray schedule=data.getJSONObject("cn").optJSONArray(s);boolean covered=false;JSONArray years=data.getJSONArray("cnYears");for(int i=0;i<years.length();i++)if(years.getInt(i)==date.getYear())covered=true;
    String badge="",status=covered?(weekday==0||weekday==6?"大陸一般週末":"大陸一般平日"):"大陸調休安排未收錄";
    if(schedule!=null){boolean rest=schedule.getString(0).equals("rest");badge=rest?"休":"班";status=(rest?"大陸放假 · ":"大陸補班 · ")+schedule.getString(1);JSONArray combined=new JSONArray();combined.put(event(schedule.getString(1),rest?"中國大陸 · 官方放假調休":"中國大陸 · 官方補班",rest?"rest":"work"));for(int i=0;i<events.length();i++)combined.put(events.get(i));events=combined;}
    return new JSONObject().put("date",s).put("day",date.getDayOfMonth()).put("weekday",weekday).put("lunar",l).put("events",events).put("term",term).put("badge",badge).put("status",status).put("covered",covered);
  }
  public static JSONObject event(String n,String c,String kind)throws JSONException{return new JSONObject().put("n",n).put("c",c).put("kind",kind);}
  public JSONObject restInfo(String s)throws JSONException{
    if(!valid(s))return null;LocalDate date=LocalDate.parse(s);int weekday=date.getDayOfWeek().getValue()%7;boolean covered=false;JSONArray years=data.getJSONArray("cnYears");for(int i=0;i<years.length();i++)if(years.getInt(i)==date.getYear())covered=true;
    JSONObject run=holidayByDate.get(s);JSONArray entry=data.getJSONObject("cn").optJSONArray(s);boolean work=entry!=null&&entry.getString(0).equals("work");String kind=run!=null?"holiday":work?"work":!covered?"unknown":weekday==0||weekday==6?"weekend":"weekday";
    return new JSONObject().put("kind",kind).put("covered",covered).put("name",run!=null?run.getString("name"):work?entry.getString(1):"").put("start",run!=null?run.getString("start"):"").put("end",run!=null?run.getString("end"):"").put("days",run!=null?run.getInt("days"):0).put("index",run!=null?(int)ChronoUnit.DAYS.between(LocalDate.parse(run.getString("start")),date)+1:0);
  }
  public JSONArray monthBreaks(String s)throws JSONException{String first=s.substring(0,7)+"-01",last=s.substring(0,7)+"-31";JSONArray out=new JSONArray();for(JSONObject run:holidayRuns)if(run.getString("end").compareTo(first)>=0&&run.getString("start").compareTo(last)<=0)out.put(run);return out;}
  public List<String> month(String s,boolean monday){LocalDate date=LocalDate.parse(s).withDayOfMonth(1);int offset=(date.getDayOfWeek().getValue()%7+(monday?6:0))%7;List<String> keys=new ArrayList<>();for(int i=0;i<42;i++)keys.add(date.plusDays(i-offset).toString());return keys;}
  public String shiftMonth(String s,int delta){String key=LocalDate.parse(s).withDayOfMonth(1).plusMonths(delta).toString();return valid(key)?key:null;}
}
