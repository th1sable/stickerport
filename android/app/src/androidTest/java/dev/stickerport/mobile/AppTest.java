package dev.stickerport.mobile;
import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.util.Base64;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import java.io.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class) public class AppTest {
 @Test public void homeLoadsJavascriptAndNativeStorage()throws Exception{
  var instrumentation=InstrumentationRegistry.getInstrumentation();Context c=instrumentation.getTargetContext();Intent intent=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);MainActivity activity=(MainActivity)instrumentation.startActivitySync(intent);
  try{boolean loaded=false;for(int i=0;i<40;i++){CountDownLatch latch=new CountDownLatch(1);String[] value={""};instrumentation.runOnMainSync(()->activity.home.evaluateJavascript("typeof window.stickerCandidates + ':' + document.getElementById('count').textContent",v->{value[0]=v;latch.countDown();}));assertTrue(latch.await(2,TimeUnit.SECONDS));if(value[0].contains("function:0")){loaded=true;break;}Thread.sleep(250);}assertTrue("Shared UI module and native library must load",loaded);}finally{instrumentation.runOnMainSync(activity::finish);}
 }
 @Test public void exportIsReadableThroughWhatsAppProvider()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();JSONArray stickers=new JSONArray();Bitmap image=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);image.eraseColor(Color.GREEN);ByteArrayOutputStream bytes=new ByteArrayOutputStream();image.compress(Bitmap.CompressFormat.WEBP,85,bytes);for(int i=0;i<3;i++)stickers.put(new JSONObject().put("animated",false).put("mime","image/webp").put("data",Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP)));
  Bitmap icon=Bitmap.createScaledBitmap(image,96,96,true);bytes.reset();icon.compress(Bitmap.CompressFormat.PNG,100,bytes);JSONObject p=Store.export(c,new JSONObject().put("name","Test").put("animated",false).put("stickers",stickers),Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP));String id=p.getString("id"),base="content://"+c.getPackageName()+".stickers/";
  try(Cursor cursor=c.getContentResolver().query(Uri.parse(base+"metadata/"+id),null,null,null,null)){assertTrue(cursor.moveToFirst());assertEquals("Test",cursor.getString(cursor.getColumnIndexOrThrow("sticker_pack_name")));}
  try(Cursor cursor=c.getContentResolver().query(Uri.parse(base+"stickers/"+id),null,null,null,null)){assertEquals(3,cursor.getCount());}
  try(InputStream in=c.getContentResolver().openInputStream(Uri.parse(base+"stickers_asset/"+id+"/0.webp"))){Bitmap result=BitmapFactory.decodeStream(in);assertEquals(512,result.getWidth());}
  assertThrows(FileNotFoundException.class,()->c.getContentResolver().openInputStream(Uri.parse(base+"stickers_asset/"+id+"/pack.json")));
 }
 @Test public void nativeNetworkPolicy(){assertTrue(MainActivity.media("https://a.ibyteimg.com/x"));assertFalse(MainActivity.media("https://ibyteimg.com.evil.test/x"));assertFalse(MainActivity.media("http://a.ibyteimg.com/x"));assertFalse(MainActivity.media("https://127.0.0.1/"));assertTrue(MainActivity.tiktok("https://vm.tiktok.com/a"));assertFalse(MainActivity.tiktok("https://tiktok.com.evil.test"));}
 @Test public void storageRoundTrip()throws Exception{Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();JSONObject original=Store.load(c);try{Store.save(c,new JSONObject("{\"packs\":[{\"name\":\"Prova italiana\"}]}"));assertEquals("Prova italiana",Store.load(c).getJSONArray("packs").getJSONObject(0).getString("name"));}finally{Store.save(c,original);}}
}
