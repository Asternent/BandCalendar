package android.content;
public interface SharedPreferences {
  String getString(String k,String fallback);boolean getBoolean(String k,boolean fallback);Editor edit();
  interface Editor {Editor putString(String k,String v);Editor putBoolean(String k,boolean v);void apply();}
}
