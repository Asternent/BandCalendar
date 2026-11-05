package android.content.res;
import java.io.*;
public class AssetManager {public InputStream open(String name)throws IOException{return new FileInputStream("android/app/src/main/assets/"+name);}}
