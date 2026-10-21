"""Generate the original geometric StickerPort icon, without external dependencies."""
from pathlib import Path
import struct, zlib, json
root=Path(__file__).resolve().parents[1]/'ios/StickerPort/Assets.xcassets/AppIcon.appiconset'
root.mkdir(parents=True,exist_ok=True)
size=1024
raw=bytearray()
for y in range(size):
    raw.append(0)
    for x in range(size):
        nx,ny=x*108/size,y*108/size
        active=(30<=nx<78 and (24<=ny<38 or 47<=ny<61 or 70<=ny<84)) or (30<=nx<44 and 38<=ny<47) or (64<=nx<78 and 61<=ny<70)
        raw.extend((197,247,122) if active else (16,17,24))
def chunk(tag,data):
    return struct.pack('>I',len(data))+tag+data+struct.pack('>I',zlib.crc32(tag+data)&0xffffffff)
(root/'icon.png').write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',size,size,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b''))
(root/'Contents.json').write_text(json.dumps({'images':[{'filename':'icon.png','idiom':'universal','platform':'ios','size':'1024x1024'}],'info':{'author':'xcode','version':1}}))
