package com.bandcalendar.sync;

import android.content.*;
import android.os.*;
import com.xiaomi.xms.wearable.Wearable;
import com.xiaomi.xms.wearable.node.*;
import com.xiaomi.xms.wearable.message.*;
import com.xiaomi.xms.wearable.auth.Permission;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SyncController {
  private static SyncController instance;
  public static synchronized SyncController get(Context c){if(instance==null)instance=new SyncController(c.getApplicationContext());return instance;}
  final Context context;
  final CalendarRepository repo;
  final Handler main=new Handler(Looper.getMainLooper());
  private final NodeApi nodes;
  private final MessageApi messages;
  private String nodeId="", listenerNode="", revision="";
  private JSONArray deviceList=new JSONArray();
  private boolean connecting=false, sending=false;
  private int connectGeneration=0, transferGeneration=0, sendAttempt=0;
  private long lastRequest=0;
  private String bridgeStatus="尚未連接手環";
  private byte[] pending;
  private String sentSettings="";
  private SyncController(Context c) {
    context=c;repo=new CalendarRepository(c);nodes=Wearable.getNodeApi(c);messages=Wearable.getMessageApi(c);
  }
  public void connectAndSync() {main.post(()->{
    if(connecting||sending)return;
    connecting=true;bridgeStatus="正在尋找已配對手環…";final int generation=++connectGeneration;
    main.postDelayed(()->{if(connecting&&generation==connectGeneration){connecting=false;connectGeneration++;bridgeStatus="連接逾時；請開啟小米運動健康並確認手環已連線";}},15000);
    try {nodes.getConnectedNodes().addOnSuccessListener(list->main.post(()->{
      if(generation!=connectGeneration)return;
      connecting=false;deviceList=new JSONArray();for(Node n:list){try{deviceList.put(new JSONObject().put("id",n.id).put("name",n.name));}catch(Exception ignored){}}
      if(list.isEmpty()){bridgeStatus="找不到已連線手環；請先在小米運動健康連線";return;}
      String preferred=repo.prefs.getString("device","");Node chosen=null;
      for(Node n:list)if(n.id.equals(preferred))chosen=n;
      if(chosen==null&&list.size()==1)chosen=list.get(0);
      if(chosen==null){bridgeStatus="請選擇要同步的手環";return;}
      final Node device=chosen;nodeId=device.id;
      bridgeStatus="已找到 "+device.name+"；正在建立通道";
      registerAndSend(generation);
    })).addOnFailureListener(e->main.post(()->{if(generation!=connectGeneration)return;connecting=false;bridgeStatus=reason(e);}));}
    catch(Exception e){connecting=false;bridgeStatus=reason(e);}
  });}
  private void registerAndSend(int generation){
    Wearable.getAuthApi(context).checkPermission(nodeId,Permission.DEVICE_MANAGER)
      .addOnSuccessListener(allowed->main.post(()->{if(generation!=connectGeneration)return;if(allowed)openChannel(generation);else bridgeStatus="需要手環通訊授權；請點下方「授權手環通訊」";}))
      .addOnFailureListener(e->main.post(()->{if(generation==connectGeneration)bridgeStatus="請先點「授權手環通訊」完成小米授權";}));
  }
  public void authorize(android.app.Activity activity){main.post(()->{
    if(nodeId.isEmpty()){connectAndSync();bridgeStatus="請先連線取得手環，再點授權手環通訊";return;}
    bridgeStatus="正在向小米申請手環通訊授權…";
    Wearable.getAuthApi(activity).requestPermission(nodeId,Permission.DEVICE_MANAGER)
      .addOnSuccessListener(granted->main.post(()->{
        // Permission is parcelled by the SDK. Re-check the actual grant; object identity and an empty newly-granted list are not authoritative.
        Wearable.getAuthApi(context).checkPermission(nodeId,Permission.DEVICE_MANAGER)
          .addOnSuccessListener(allowed->main.post(()->{if(allowed){bridgeStatus="通訊授權完成";openChannel(++connectGeneration);}else bridgeStatus="小米尚未授予手環通訊權限";}))
          .addOnFailureListener(e->main.post(()->bridgeStatus=reason(e)));
      }))
      .addOnFailureListener(e->main.post(()->bridgeStatus=reason(e)));
  });}
  private void openChannel(int generation){
    if(nodeId.equals(listenerNode)){sendSnapshot();return;}
    final String target=nodeId;
    messages.addListener(target,(id,bytes)->main.post(()->receive(id,bytes)))
      .addOnSuccessListener(v->main.post(()->{if(generation!=connectGeneration)return;listenerNode=target;sendSnapshot();}))
      .addOnFailureListener(e->main.post(()->{if(generation==connectGeneration)bridgeStatus=reason(e);}));
    main.postDelayed(()->{if(generation==connectGeneration&&!target.equals(listenerNode))bridgeStatus="通道建立逾時；此版小米運動健康可能未開放第三方互通";},12000);
  }
  private String reason(Exception e){
    android.util.Log.w("BandCalendar", "Xiaomi interconnect: "+e.getClass().getSimpleName()+" "+e.getMessage());
    String name=e.getClass().getSimpleName();
    if(name.contains("Signature"))return "小米拒絕簽章驗證；請確認安裝的是同一組 APK 與 RPK";
    if(name.contains("Permission"))return "小米拒絕存取；目前小米運動健康可能需要開發者接入授權";
    if(name.contains("NotInstalled"))return "請先安裝中國版小米運動健康及手環端腕上日曆";
    return "手環連線失敗（"+name+"）；請確認小米運動健康與手環端都已開啟";
  }
  private void sendSnapshot(){
    if(sending)return;
    try {
      long seq=Math.max(System.currentTimeMillis(),repo.prefs.getLong("sequence",0)+1);repo.prefs.edit().putLong("sequence",seq).apply();revision=String.valueOf(seq);
      JSONObject settings=repo.settings();sentSettings=settings.toString();pending=new JSONObject().put("type","settings").put("v",1).put("id",revision).put("settings",settings).toString().getBytes(StandardCharsets.UTF_8);
      sending=true;sendAttempt=0;sendPending();
    }catch(Exception e){sending=false;bridgeStatus="無法準備日曆設定";}
  }
  private void sendPending(){
    final int generation=++transferGeneration;bridgeStatus="正在傳送日曆設定…";
    main.postDelayed(()->{if(sending&&generation==transferGeneration){if(sendAttempt++<1)sendPending();else{sending=false;transferGeneration++;bridgeStatus="未收到手環確認；請開啟腕上日曆後重試";}}},12000);
    try{messages.sendMessage(nodeId,pending).addOnSuccessListener(v->main.post(()->{if(sending&&generation==transferGeneration)bridgeStatus="已傳送；等待手環儲存確認…";})).addOnFailureListener(e->main.post(()->{if(generation!=transferGeneration)return;sending=false;transferGeneration++;bridgeStatus=reason(e);}));}
    catch(Exception e){sending=false;transferGeneration++;bridgeStatus=reason(e);}
  }
  private void receive(String id,byte[] bytes){
    if(!id.equals(nodeId)||bytes.length>2048)return;
    try{JSONObject p=new JSONObject(new String(bytes,StandardCharsets.UTF_8));if(p.optInt("v")!=1)return;String type=p.optString("type");
      if(type.equals("ack")&&sending&&revision.equals(p.optString("id"))){sending=false;transferGeneration++;repo.prefs.edit().putLong("lastAck",System.currentTimeMillis()).putString("ackSettings",sentSettings).apply();bridgeStatus="手環已確認收到並儲存設定";if(!repo.settings().toString().equals(sentSettings))sendSnapshot();}
      else if(type.equals("hello")||type.equals("refresh")){long now=System.currentTimeMillis();if(now-lastRequest<1000)return;lastRequest=now;sendSnapshot();}
      else if(type.equals("nack")&&revision.equals(p.optString("id"))){sending=false;transferGeneration++;bridgeStatus="手環儲存失敗；請重試，原設定仍保留";}
    }catch(Exception ignored){}
  }
  public void selectDevice(String id){main.post(()->{if(sending){bridgeStatus="請等目前同步完成再切換手環";return;}if(!id.equals(nodeId)&&!listenerNode.isEmpty()){messages.removeListener(listenerNode);listenerNode="";}repo.prefs.edit().putString("device",id).apply();nodeId=id;connectAndSync();});}
  public JSONObject state(){JSONObject out=new JSONObject();try{out.put("bridgeStatus",bridgeStatus).put("sending",sending).put("connecting",connecting).put("devices",deviceList).put("device",nodeId).put("lastAck",repo.prefs.getLong("lastAck",0)).put("dirty",!repo.settings().toString().equals(repo.prefs.getString("ackSettings","")));}catch(Exception ignored){}return out;}
}
