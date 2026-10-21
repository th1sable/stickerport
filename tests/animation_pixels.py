"""Decode before/after with libwebp and compare every composited frame."""
from pathlib import Path
import tempfile, subprocess
from PIL import Image, ImageDraw, ImageChops
root=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as temp:
    source=Path(temp)/'original.webp'; destination=Path(temp)/'padded.webp'
    frames=[]
    for i in range(12):
        frame=Image.new('RGBA',(498,280),(0,0,0,0))
        draw=ImageDraw.Draw(frame)
        draw.rectangle((i*18,20,i*18+80,180),fill=(255,i*15,90,255))
        draw.ellipse((350,70,470,190),fill=(50,200,100,255))
        frames.append(frame)
    frames[0].save(source,save_all=True,append_images=frames[1:],duration=42,loop=0,lossless=True)
    script="import fs from 'node:fs';import {pathToFileURL} from 'node:url';const {padAnimatedWebP}=await import(pathToFileURL(process.argv[1]));fs.writeFileSync(process.argv[3],padAnimatedWebP(fs.readFileSync(process.argv[2])));"
    subprocess.run(['node','--input-type=module','-e',script,str(root/'web/core.js'),str(source),str(destination)],check=True)
    a=Image.open(source);b=Image.open(destination)
    assert b.size==(512,512) and a.n_frames==b.n_frames
    for i in range(a.n_frames):
        a.seek(i);b.seek(i);original=a.convert('RGBA');actual=b.convert('RGBA')
        expected=Image.new('RGBA',(512,512),(0,0,0,0));expected.paste(original,(6,116))
        # Compare alpha, then visible RGB after compositing onto the same background.
        assert ImageChops.difference(expected.getchannel('A'),actual.getchannel('A')).getbbox() is None, i
        background=Image.new('RGBA',(512,512),(20,30,40,255))
        assert ImageChops.difference(Image.alpha_composite(background,expected),Image.alpha_composite(background,actual)).convert('RGB').getbbox() is None, i
    print(f'PASS: {a.n_frames} decoded frames, alpha, placement and visible pixels preserved')
