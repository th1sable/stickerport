package dev.stickerport.mobile;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.webkit.*;
import android.widget.*;
import android.view.*;
import androidx.webkit.WebViewAssetLoader;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 WebView home,browser;LinearLayout layout;String scanner="";ValueCallback<Uri[]> chooser;final ExecutorService worker=Executors.newSingleThreadExecutor();
 static final String HOME="https://appassets.androidplatform.net/assets/";
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(Color.rgb(16,17,24));getWindow().setNavigationBarColor(Color.rgb(16,17,24));layout=new LinearLayout(this);layout.setOrientation(1);layout.setBackgroundColor(Color.rgb(16,17,24));layout.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});setContentView(layout);
  home=new WebView(this);home.setBackgroundColor(Color.rgb(16,17,24));home.getSettings().setJavaScriptEnabled(true);home.getSettings().setDomStorageEnabled(true);home.getSettings().setAllowFileAccess(false);home.getSettings().setAllowContentAccess(false);home.addJavascriptInterface(new Bridge(),"StickerNative");
  WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
  home.setWebViewClient(new WebViewClient(){public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){return loader.shouldInterceptRequest(request.getUrl());}public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !request.getUrl().toString().startsWith(HOME);}});
  home.setWebChromeClient(new WebChromeClient(){public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> callback,FileChooserParams params){if(chooser!=null)chooser.onReceiveValue(null);chooser=callback;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);try{startActivityForResult(i,40);}catch(Exception e){chooser.onReceiveValue(null);chooser=null;}return true;}});
  layout.addView(home,new LinearLayout.LayoutParams(-1,-1));home.loadUrl(HOME+"index.html");
 }
 void reply(int id,Object result,String error){String value=result==null?"null":result instanceof String?JSONObject.quote((String)result):result.toString();runOnUiThread(()->home.evaluateJavascript("window.nativeReply("+id+","+value+","+(error==null?"null":JSONObject.quote(error))+")",null));}
 class Bridge {@JavascriptInterface public void post(String raw){if(raw.length()>110*1024*1024)return;worker.execute(()->{int id=0;try{JSONObject request=new JSONObject(raw);id=request.getInt("id");String action=request.getString("action");JSONObject data=request.getJSONObject("data");final int requestId=id;
   switch(action){case "load":reply(id,Store.load(MainActivity.this),null);break;case "save":Store.save(MainActivity.this,data.getJSONObject("library"));reply(id,true,null);break;case "download":reply(id,download(data.getString("url")),null);break;
    case "browse":String url=data.getString("url");if(!tiktok(url))throw new IOException("Link TikTok non valido.");scanner=data.getString("scanner");runOnUiThread(()->openBrowser(url));reply(id,true,null);break;
    case "export":JSONObject p=Store.export(MainActivity.this,data.getJSONObject("pack"),data.getString("tray"));runOnUiThread(()->sendPack(requestId,p));break;
    default:throw new IOException("Comando sconosciuto.");}
  }catch(Exception e){reply(id,null,e.getMessage()==null?"Operazione non riuscita.":e.getMessage());}});}}
 static boolean tiktok(String value){try{URI u=URI.create(value);return "https".equals(u.getScheme())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&(u.getHost().equals("tiktok.com")||u.getHost().endsWith(".tiktok.com"));}catch(Exception e){return false;}}
 static boolean media(String value){try{URI u=URI.create(value);if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return false;for(String h:new String[]{"ibyteimg.com","tiktokcdn.com","tiktokcdn-us.com","byteoversea.com"})if(u.getHost().equals(h)||u.getHost().endsWith("."+h))return true;}catch(Exception ignored){}return false;}
 static String download(String source)throws Exception{String target=source;for(int redirects=0;redirects<5;redirects++){if(!media(target))throw new IOException("Origine immagine non supportata.");HttpURLConnection c=(HttpURLConnection)new URL(target).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);try{int status=c.getResponseCode();if(status>=300&&status<400){target=new URL(new URL(target),c.getHeaderField("Location")).toString();continue;}if(status!=200)throw new IOException("Sticker non disponibile. Riapri TikTok e raccoglilo di nuovo.");if(c.getContentLengthLong()>8*1024*1024)throw new IOException("File troppo grande.");ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>8*1024*1024)throw new IOException("File troppo grande.");out.write(b,0,n);}}return android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);}finally{c.disconnect();}}throw new IOException("Troppi reindirizzamenti.");}
 void openBrowser(String url){if(browser!=null){browser.loadUrl(url);return;}home.setVisibility(View.GONE);LinearLayout toolbar=new LinearLayout(this);toolbar.setTag("toolbar");Button close=new Button(this);close.setText("← Raccolta");close.setOnClickListener(v->closeBrowser());Button scan=new Button(this);scan.setText("Raccogli sticker");scan.setOnClickListener(v->{if(!tiktok(browser.getUrl())){Toast.makeText(this,"Apri una pagina TikTok.",Toast.LENGTH_LONG).show();return;}browser.evaluateJavascript(scanner,result->{try{JSONArray items=new JSONArray(result);home.evaluateJavascript("window.stickerCandidates("+items+")",null);closeBrowser();}catch(Exception e){Toast.makeText(this,"Pagina non pronta. Riprova.",Toast.LENGTH_LONG).show();}});});toolbar.addView(close,new LinearLayout.LayoutParams(0,-2,1));toolbar.addView(scan,new LinearLayout.LayoutParams(0,-2,1));layout.addView(toolbar);
  browser=new WebView(this);browser.getSettings().setJavaScriptEnabled(true);browser.getSettings().setDomStorageEnabled(true);browser.getSettings().setAllowFileAccess(false);browser.getSettings().setAllowContentAccess(false);browser.getSettings().setMediaPlaybackRequiresUserGesture(true);browser.getSettings().setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36");browser.getSettings().setUseWideViewPort(true);browser.getSettings().setLoadWithOverviewMode(true);
  browser.setWebViewClient(new WebViewClient(){public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){return !"https".equals(r.getUrl().getScheme());}});layout.addView(browser,new LinearLayout.LayoutParams(-1,0,1));browser.loadUrl(url);
 }
 void closeBrowser(){if(browser==null)return;layout.removeView(browser);browser.destroy();browser=null;View toolbar=layout.findViewWithTag("toolbar");layout.removeView(toolbar);home.setVisibility(View.VISIBLE);}
 void sendPack(int id,JSONObject p){try{Intent intent=new Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK");intent.putExtra("sticker_pack_id",p.getString("id"));intent.putExtra("sticker_pack_authority",getPackageName()+".stickers");intent.putExtra("sticker_pack_name",p.getString("name"));intent.setPackage("com.whatsapp");if(intent.resolveActivity(getPackageManager())==null)intent.setPackage("com.whatsapp.w4b");if(intent.resolveActivity(getPackageManager())==null)throw new IOException("Installa WhatsApp prima di aggiungere il pacchetto.");startActivity(intent);reply(id,true,null);}catch(Exception e){reply(id,null,e.getMessage());}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==40&&chooser!=null){Uri[] uris=null;if(result==RESULT_OK&&data!=null){if(data.getClipData()!=null){uris=new Uri[data.getClipData().getItemCount()];for(int i=0;i<uris.length;i++)uris[i]=data.getClipData().getItemAt(i).getUri();}else if(data.getData()!=null)uris=new Uri[]{data.getData()};}chooser.onReceiveValue(uris);chooser=null;}}
 @Override public void onBackPressed(){if(browser!=null){if(browser.canGoBack())browser.goBack();else closeBrowser();}else super.onBackPressed();}
 @Override protected void onDestroy(){if(chooser!=null)chooser.onReceiveValue(null);if(browser!=null)browser.destroy();home.destroy();worker.shutdownNow();super.onDestroy();}
}
