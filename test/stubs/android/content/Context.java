package android.content;
import android.content.res.AssetManager;
/** JVM-only surface; never bundled in APK. */
public abstract class Context {public abstract SharedPreferences getSharedPreferences(String name,int mode);public abstract AssetManager getAssets();}
