// Binary WebP handling is shared between the two native applications.
export function inspectWebP(input) {
  const b = input instanceof Uint8Array ? input : new Uint8Array(input);
  const v = new DataView(b.buffer, b.byteOffset, b.byteLength);
  const tag = p => String.fromCharCode(...b.subarray(p,p+4));
  const u24 = p => b[p] | b[p+1]<<8 | b[p+2]<<16;
  if (b.length<12 || tag(0)!=='RIFF' || tag(8)!=='WEBP' || v.getUint32(4,true)+8!==b.length) throw Error('File WebP incompleto o non valido.');
  const chunks=[]; let width=0,height=0,duration=0,frames=0,animated=false,animationHeader=false;
  for(let p=12;p<b.length;) {
    if(p+8>b.length) throw Error('Intestazione WebP incompleta.');
    const size=v.getUint32(p+4,true),end=p+8+size;
    if(end+(size&1)>b.length) throw Error('Dati WebP incompleti.');
    const type=tag(p); chunks.push({type,p,size});
    if(type==='VP8X') {if(size!==10 || width) throw Error('Canvas WebP non valido.'); animated=!!(b[p+8]&2);width=u24(p+12)+1;height=u24(p+15)+1;}
    if(type==='ANIM') {if(size!==6 || animationHeader) throw Error('Animazione non valida.');animationHeader=true;}
    if(type==='ANMF') {
      if(size<16) throw Error('Fotogramma incompleto.');
      const x=u24(p+8)*2,y=u24(p+11)*2,w=u24(p+14)+1,h=u24(p+17)+1,ms=u24(p+20);
      if(!width || x+w>width || y+h>height || ms<8) throw Error('Fotogramma non compatibile con WhatsApp.');
      duration+=ms;frames++;
    }
    p=end+(size&1);
  }
  if(animated && (!animationHeader || !frames || duration>10000)) throw Error('Animazione assente o superiore a 10 secondi.');
  if(!animated && (frames || animationHeader)) throw Error('Flag animazione non valido.');
  return {b,chunks,width,height,duration,frames,animated};
}
export function padAnimatedWebP(input) {
  const info=inspectWebP(input);
  if(!info.animated) throw Error('Questo WebP non è animato.');
  if(info.width>512 || info.height>512) throw Error('Animazione oltre 512 px: riducila prima di importarla.');
  if(info.b.length>500*1024) throw Error('Animazione oltre 500 KB.');
  const out=new Uint8Array(info.b);const put=(p,n)=>{out[p]=n&255;out[p+1]=(n>>8)&255;out[p+2]=(n>>16)&255;};
  const dx=Math.floor((512-info.width)/4),dy=Math.floor((512-info.height)/4);
  for(const c of info.chunks) {
    const p=c.p;
    if(c.type==='VP8X'){out[p+8]|=16;put(p+12,511);put(p+15,511);}
    if(c.type==='ANIM') out.fill(0,p+8,p+12);
    if(c.type==='ANMF'){put(p+8,(out[p+8]|out[p+9]<<8|out[p+10]<<16)+dx);put(p+11,(out[p+11]|out[p+12]<<8|out[p+13]<<16)+dy);}
  }
  inspectWebP(out); return out;
}
export function allowedMediaURL(value) {
  try {const u=new URL(value);return u.protocol==='https:' && !u.username && !u.password && (!u.port || u.port==='443') && ['ibyteimg.com','tiktokcdn.com','tiktokcdn-us.com','byteoversea.com'].some(h=>u.hostname===h || u.hostname.endsWith('.'+h));} catch{return false;}
}
export function validTikTokURL(value) {
  try{const u=new URL(value);return u.protocol==='https:' && !u.username && !u.password && (!u.port || u.port==='443') && (u.hostname==='tiktok.com'||u.hostname.endsWith('.tiktok.com'));}catch{return false;}
}
export function validatePack(pack) {
  if(!pack.name?.trim() || pack.name.length>64) throw Error('Scegli un nome da 1 a 64 caratteri.');
  if(pack.stickers.length<3 || pack.stickers.length>30) throw Error('WhatsApp richiede da 3 a 30 sticker per pacchetto.');
  if(pack.stickers.some(s=>s.animated!==pack.animated)) throw Error('Separa sticker statici e animati.');
  return true;
}
export const scanner = `(() => {
 const found = new Map();
 for (const image of document.querySelectorAll('img')) {
   const source=image.currentSrc||image.src, label=(image.alt||'').toLowerCase();
   if(!source || !image.complete || !image.naturalWidth || !image.getClientRects().length) continue;
   if(label.includes('sticker') || /dm-sticker|dhq7zx4c1p/.test(source)) {
     try { const u=new URL(source); if(u.protocol==='https:') found.set(source,{url:source,width:image.naturalWidth,height:image.naturalHeight}); }catch{}
   }
 }
 return Array.from(found.values()).slice(0,60);
})()`;
