package dev.stickerport.mobile;

import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import org.json.*;
import java.io.*;
import java.util.*;

public class StickerProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
  List<String> path=uri.getPathSegments();if(path.isEmpty())throw new IllegalArgumentException("URI");
  if(path.get(0).equals("metadata")&&path.size()<=2){
   MatrixCursor cursor=new MatrixCursor(new String[]{"sticker_pack_identifier","sticker_pack_name","sticker_pack_publisher","sticker_pack_icon","android_play_store_link","ios_app_download_link","sticker_pack_publisher_email","sticker_pack_publisher_website","sticker_pack_privacy_policy_website","sticker_pack_license_agreement_website","image_data_version","whatsapp_will_not_cache_stickers","animated_sticker_pack"});
   File[] dirs=Store.root(getContext()).listFiles();if(dirs!=null)for(File dir:dirs){if(path.size()==2&&!dir.getName().equals(path.get(1)))continue;try{JSONObject p=Store.metadata(getContext(),dir.getName());cursor.addRow(new Object[]{p.getString("id"),p.getString("name"),"StickerPort","tray.png","","","","https://github.com/th1sable/stickerport","","","1",0,p.getBoolean("animated")?1:0});}catch(Exception ignored){}}
   cursor.setNotificationUri(getContext().getContentResolver(),uri);return cursor;
  }
  if(path.get(0).equals("stickers")&&path.size()==2){MatrixCursor cursor=new MatrixCursor(new String[]{"sticker_file_name","sticker_emoji","sticker_accessibility_text"});try{JSONArray files=Store.metadata(getContext(),path.get(1)).getJSONArray("files");for(int i=0;i<files.length();i++)cursor.addRow(new Object[]{files.getString(i),"","Sticker"});}catch(Exception ignored){}cursor.setNotificationUri(getContext().getContentResolver(),uri);return cursor;}
  throw new IllegalArgumentException("URI non supportato");
 }
 public AssetFileDescriptor openAssetFile(Uri uri,String mode)throws FileNotFoundException{
  List<String> p=uri.getPathSegments();if(!mode.equals("r")||p.size()!=3||!p.get(0).equals("stickers_asset"))throw new FileNotFoundException();
  try{JSONObject pack=Store.metadata(getContext(),p.get(1));boolean allowed=p.get(2).equals("tray.png");JSONArray names=pack.getJSONArray("files");for(int i=0;i<names.length();i++)allowed|=names.getString(i).equals(p.get(2));if(!allowed)throw new FileNotFoundException();File file=new File(new File(Store.root(getContext()),p.get(1)),p.get(2));return new AssetFileDescriptor(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY),0,file.length());}catch(Exception e){throw new FileNotFoundException("Sticker assente");}
 }
 public String getType(Uri uri){List<String> p=uri.getPathSegments();if(p.size()==3&&p.get(0).equals("stickers_asset"))return p.get(2).equals("tray.png")?"image/png":"image/webp";if(p.size()==1&&p.get(0).equals("metadata"))return "vnd.android.cursor.dir/vnd."+uri.getAuthority()+".metadata";if(p.size()==2&&p.get(0).equals("metadata"))return "vnd.android.cursor.item/vnd."+uri.getAuthority()+".metadata";if(p.size()==2&&p.get(0).equals("stickers"))return "vnd.android.cursor.dir/vnd."+uri.getAuthority()+".stickers";throw new IllegalArgumentException("URI");}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
 public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
 public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
