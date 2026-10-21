package dev.stickerport.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AtomicFile;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

final class Store {
 static File root(Context c){return new File(c.getFilesDir(),"exports");}
 static JSONObject load(Context c)throws Exception{File f=new File(c.getFilesDir(),"library.json");return f.exists()?new JSONObject(new String(new AtomicFile(f).readFully(),StandardCharsets.UTF_8)):new JSONObject("{\"packs\":[]}");}
 static void atomic(File f,byte[] bytes)throws Exception{AtomicFile a=new AtomicFile(f);FileOutputStream s=null;try{s=a.startWrite();s.write(bytes);a.finishWrite(s);}catch(Exception e){if(s!=null)a.failWrite(s);throw e;}}
 static void save(Context c,JSONObject library)throws Exception{library.getJSONArray("packs");byte[] bytes=library.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>100*1024*1024)throw new IOException("Raccolta troppo grande. Elimina alcuni pacchetti.");atomic(new File(c.getFilesDir(),"library.json"),bytes);}
 static JSONObject export(Context c,JSONObject pack,String tray)throws Exception{
  JSONArray stickers=pack.getJSONArray("stickers");String name=pack.getString("name");boolean animated=pack.getBoolean("animated");
  if(stickers.length()<3||stickers.length()>30||name.trim().isEmpty()||name.length()>64)throw new IOException("Pacchetto non valido.");
  String id="sp-"+UUID.randomUUID();File dir=new File(root(c),id);if(!dir.mkdirs())throw new IOException("Impossibile creare il pacchetto.");
  byte[] icon=Base64.decode(tray,Base64.DEFAULT);Bitmap iconBitmap=BitmapFactory.decodeByteArray(icon,0,icon.length);
  if(icon.length>50*1024||iconBitmap==null||iconBitmap.getWidth()!=96||iconBitmap.getHeight()!=96)throw new IOException("Icona non valida.");
  Files.write(new File(dir,"tray.png").toPath(),icon);JSONArray names=new JSONArray();
  for(int i=0;i<stickers.length();i++){
   JSONObject sticker=stickers.getJSONObject(i);if(sticker.getBoolean("animated")!=animated)throw new IOException("Tipi di sticker diversi.");byte[] b=Base64.decode(sticker.getString("data"),Base64.DEFAULT);
   if(!sticker.getString("mime").equals("image/webp")){Bitmap bitmap=BitmapFactory.decodeByteArray(b,0,b.length);if(bitmap==null)throw new IOException("Immagine non valida.");ByteArrayOutputStream output=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.WEBP,85,output);b=output.toByteArray();bitmap.recycle();}
   BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(b,0,b.length,bounds);
   if(b.length>(animated?500:100)*1024||bounds.outWidth!=512||bounds.outHeight!=512)throw new IOException("Sticker non compatibile con WhatsApp.");
   String filename=i+".webp";Files.write(new File(dir,filename).toPath(),b);names.put(filename);
  }
  JSONObject metadata=new JSONObject().put("id",id).put("name",name).put("animated",animated).put("files",names);
  atomic(new File(dir,"pack.json"),metadata.toString().getBytes(StandardCharsets.UTF_8));return metadata;
 }
 static JSONObject metadata(Context c,String id)throws Exception{if(!id.matches("sp-[a-f0-9-]{36}"))throw new IOException("Identificatore non valido");return new JSONObject(new String(Files.readAllBytes(new File(new File(root(c),id),"pack.json").toPath()),StandardCharsets.UTF_8));}
}
