"""Build NV Sans (OFL Manrope derivative) and original NV outline glyphs.

Build dependencies: fonttools, pillow. Runtime uses only prebuilt MTSDF atlases.
The source TTF and OFL license come from google/fonts/ofl/manrope.
"""
from pathlib import Path
import json, math, subprocess, shutil
from concurrent.futures import ThreadPoolExecutor
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.subset import Subsetter, Options
from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / 'build/identity'
ASSETS = ROOT / 'src/main/resources/assets/nv'
GEN = ROOT / 'tools/msdf-atlas-gen/msdf-atlas-gen/msdf-atlas-gen.exe'

def atlas(ttf, target, chars):
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([str(GEN), '-font', str(ttf), '-chars', chars, '-type', 'mtsdf',
                    '-size', '48', '-pxrange', '4', '-potr', '-overlap', '-threads', '2',
                    '-imageout', str(target.with_suffix('.png')), '-json', str(target.with_suffix('.json'))], check=True)

def build_text(weight):
    name, value = weight
    font = instantiateVariableFont(TTFont(WORK/'font-source/Manrope.ttf'), {'wght':value}, inplace=True)
    options = Options(); options.layout_features = ['kern','liga']; options.name_IDs = ['*']
    subset = Subsetter(options=options)
    subset.populate(unicodes=list(range(32,256))+list(range(0x400,0x530))+list(range(0x2000,0x2070))+list(range(0x20A0,0x20D0)))
    subset.subset(font)
    for key, text in [(1,'NV Sans'),(2,name.title()),(4,f'NV Sans {name.title()}'),(6,f'NVSans-{name.title()}')]:
        font['name'].setName(text,key,3,1,0x409)
        font['name'].setName(text,key,1,0,0)
    target = ASSETS / f'fonts/nv-sans/nv-sans-{name}'
    ttf = target.with_suffix('.ttf'); ttf.parent.mkdir(parents=True, exist_ok=True); font.save(ttf)
    atlas(ttf, target, '[0x20,0xff],[0x400,0x52f],[0x2000,0x206f],[0x20a0,0x20cf]')
    print('NV Sans', name, 'built')

# Original compact geometry on a shared 24-unit grid. All strokes use round caps.
def line(*points): return ('line', points)
def circle(x,y,r): return ('circle',(x,y,r))
def box(x,y,w,h): return line((x,y),(x+w,y),(x+w,y+h),(x,y+h),(x,y))
def arc(x,y,r,a,b): return line(*[(x+r*math.cos(t),y+r*math.sin(t)) for t in [a+(b-a)*i/24 for i in range(25)]])
SHAPES = {
 'modules':[box(4,4,6,6),box(14,4,6,6),box(4,14,6,6),line((14,17),(20,17)),line((17,14),(17,20))],
 'pinned':[line((8,4),(16,4),(15,10),(19,14),(5,14),(9,10),(8,4)),line((12,14),(12,21))],
 'visuals':[line((2,12),(7,7),(17,7),(22,12),(17,17),(7,17),(2,12)),circle(12,12,2.5)],
 'interface':[box(3,4,18,16),line((3,9),(21,9)),line((9,9),(9,20))],
 'utils':[arc(15,8,5,0,4.7),line((15,3),(15,8),(20,8)),line((11,12),(4,19),(5,21),(7,20),(14,13))],
 'themes':[line((12,3),(20,7),(20,17),(12,21),(4,17),(4,7),(12,3)),circle(9,9,1),circle(15,9,1),circle(9,15,1),circle(15,15,1)],
 'search':[circle(10,10,6),line((15,15),(21,21))],
 'settings':[line((8,3),(16,3),(21,8),(21,16),(16,21),(8,21),(3,16),(3,8),(8,3)),circle(12,12,3)],
 'language':[circle(12,12,9),line((3,12),(21,12)),arc(12,12,6,-1.5,1.5),arc(12,12,6,1.65,4.65)],
 'scale':[line((3,9),(3,3),(9,3)),line((15,21),(21,21),(21,15)),line((3,3),(10,10)),line((21,21),(14,14))],
 'aspect-ratio':[box(3,6,18,12),line((7,10),(7,14)),line((17,10),(17,14))],
 'color':[line((12,3),(19,13),(18,18),(12,21),(6,18),(5,13),(12,3)),line((9,16),(12,18),(15,16))],
 'glow':[circle(12,12,4)]+[line((12+7*math.cos(a),12+7*math.sin(a)),(12+10*math.cos(a),12+10*math.sin(a))) for a in [i*math.pi/4 for i in range(8)]],
 'glass':[line((6,3),(18,3),(20,20),(4,20),(6,3)),line((9,6),(7,15)),line((12,6),(11,10))],
 'shards':[line((4,3),(14,4),(11,12),(3,14),(4,3)),line((17,6),(21,9),(19,20),(13,21),(13,15),(17,6))],
 'sound':[line((3,9),(7,9),(12,4),(12,20),(7,15),(3,15),(3,9)),arc(12,12,5,-.8,.8),arc(12,12,9,-.8,.8)],
 'volume':[box(3,14,3,6),box(10,9,3,11),box(17,4,3,16)],
 'pitch':[line((4,18),(4,13),(9,13),(9,6),(15,6),(15,11),(20,11),(20,17))],
 'bind':[box(3,5,18,14),line((6,9),(8,9)),line((11,9),(13,9)),line((16,9),(18,9)),line((7,15),(17,15))],
 'speed':[arc(12,14,9,math.pi,2*math.pi),line((12,14),(17,7)),line((4,18),(20,18))],
 'animation':[line((4,6),(10,6),(10,12),(4,12),(4,6)),line((14,10),(20,10),(20,16),(14,16),(14,10)),line((7,16),(7,20),(17,20))],
 'jump-circle':[arc(12,17,9,0,2*math.pi),line((12,13),(12,3)),line((8,7),(12,3),(16,7))],
 'music':[line((8,17),(8,5),(19,3),(19,15)),line((8,9),(19,7)),circle(5,18,3),circle(16,16,3)],
 'cooldowns':[circle(12,13,8),line((9,2),(15,2)),line((12,2),(12,5)),line((12,8),(12,13),(16,15)),line((18,5),(20,3))],
 'hotkeys':[box(3,5,18,14),line((6,9),(9,9)),line((15,9),(18,9)),line((8,15),(16,15)),line((11,8),(13,11),(11,13))],
 'potions':[line((9,3),(15,3)),line((10,3),(10,9),(5,17),(7,21),(17,21),(19,17),(14,9),(14,3)),line((8,15),(16,15))],
 'arraylist':[line((8,y),(21,y)) for y in (6,12,18)]+[circle(3,y,.7) for y in (6,12,18)],
 'add':[line((4,12),(20,12)),line((12,4),(12,20))],
 'delete':[line((4,6),(20,6)),line((8,6),(8,3),(16,3),(16,6)),line((6,6),(7,21),(17,21),(18,6)),line((10,10),(10,17)),line((14,10),(14,17))],
 'duplicate':[box(8,8,12,12),line((16,8),(16,3),(3,3),(3,16),(8,16))],
 'import':[line((4,14),(4,20),(20,20),(20,14)),line((12,3),(12,15)),line((8,11),(12,15),(16,11))],
 'export':[line((4,14),(4,20),(20,20),(20,14)),line((12,15),(12,3)),line((8,7),(12,3),(16,7))],
 'close':[line((6,6),(18,18)),line((18,6),(6,18))],
 'back':[line((10,5),(3,12),(10,19)),line((3,12),(21,12))],
 'chevron-down':[line((5,8),(12,15),(19,8))],
 'check':[line((4,12),(9,17),(20,6))],
 'more':[circle(x,12,1.2) for x in (5,12,19)],
 'heart':[line((12,21),(3,12),(3,6),(6,3),(9,3),(12,6),(15,3),(18,3),(21,6),(21,12),(12,21))],
 'play':[line((7,3),(21,12),(7,21),(7,3))],
 'pause':[box(5,4,4,16),box(15,4,4,16)],
 'previous':[line((18,4),(6,12),(18,20),(18,4)),line((3,4),(3,20))],
 'next':[line((6,4),(18,12),(6,20),(6,4)),line((21,4),(21,20))],
 'profile':[circle(12,7,4),arc(12,21,8,math.pi,2*math.pi),line((4,21),(20,21))],
 'edge-fringe':[line(*[(x,12+3*math.sin(x*.65)) for x in range(2,23)])],
 'crown':[line((3,7),(8,11),(12,3),(16,11),(21,7),(19,20),(5,20),(3,7)),line((5,16),(19,16))],
 'pet':[line((5,10),(5,3),(10,6),(14,6),(19,3),(19,10),(21,15),(18,21),(6,21),(3,15),(5,10)),circle(8,13,1),circle(16,13,1),line((10,17),(12,18),(14,17))],
 'pickaxe':[line((5,4),(13,3),(20,9)),line((12,4),(4,21))],
}

def polygon(pen, pts):
    # SVG y-down becomes font y-up. Reverse order to keep the same winding.
    pts = [(x*40,(24-y)*40) for x,y in reversed(pts)]
    pen.moveTo(pts[0]); [pen.lineTo(p) for p in pts[1:]]; pen.closePath()

def disk(pen,x,y,r): polygon(pen,[(x+r*math.cos(i*math.pi/12),y+r*math.sin(i*math.pi/12)) for i in range(24)])

def build_icons():
    if (ROOT/'nivorat_nv_icons_pack/nivoratclient-original').is_dir():
        # Keep imported artwork authoritative during subsequent identity builds.
        import sys
        subprocess.run(['node', str(ROOT/'tools/build_imported_icons.js')], cwd=ROOT, check=True)
        subprocess.run(['node', str(ROOT/'tools/build_font.js')], cwd=ROOT, check=True)
        codepoints=json.loads((ROOT/'nivorat_nv_icons_pack/codepoints.json').read_text())
        last=max(int(value[2:],16) for value in codepoints.values())
        subprocess.run([str(GEN), '-font', str(ROOT/'build/icon_font/nv.ttf'), '-chars', f'[0xe001,0x{last:x}]',
                        '-type','mtsdf','-size','64','-pxrange','6','-potr','-overlap','-threads','2',
                        '-imageout',str(ROOT/'build/icon_font/nv_atlas.png'),'-json',str(ROOT/'build/icon_font/nv_atlas.json')],check=True)
        subprocess.run([sys.executable,str(ROOT/'tools/publish_imported_icons.py')],cwd=ROOT,check=True)
        return
    codepoints=json.loads((ROOT/'nivorat_nv_icons_pack/codepoints.json').read_text())
    for i,name in enumerate(('crown','pet','pickaxe'),0xE02D): codepoints[name]=f'U+{i:04X}'
    glyphs={}; cp={}; metrics={}; atlas_json=ASSETS/'fonts/nv/nv.json'
    texture_alias={'arrow_down':'chevron-down','gear':'settings','pin':'pinned'}
    for name,items in SHAPES.items():
        pen=TTGlyphPen(None); image=Image.new('RGBA',(192,192)); draw=ImageDraw.Draw(image)
        svg=[]
        for kind,data in items:
            if kind=='circle':
                x,y,r=data
                disk(pen,x,y,r+.8); disk(pen,x,y,max(0,r-.8)) if r>.8 else None
                # Reverse inner circle winding to cut the centre out.
                if r>.8:
                    # Replace the two same-winding disks with a proper annular contour.
                    # The inner disk above is cancelled by two reverse contours.
                    for _ in range(2): polygon(pen,[(x+(r-.8)*math.cos(-i*math.pi/12),y+(r-.8)*math.sin(-i*math.pi/12)) for i in range(24)])
                draw.ellipse(((x-r)*8,(y-r)*8,(x+r)*8,(y+r)*8),outline='white',width=13)
                svg.append(f'<circle cx="{x}" cy="{y}" r="{r}"/>')
            else:
                for (x,y),(xx,yy) in zip(data,data[1:]):
                    length=math.hypot(xx-x,yy-y)
                    if length:
                        dx=(yy-y)/length*.8; dy=-(xx-x)/length*.8
                        polygon(pen,[(x+dx,y+dy),(xx+dx,yy+dy),(xx-dx,yy-dy),(x-dx,y-dy)])
                for x,y in data: disk(pen,x,y,.8)
                draw.line([(x*8,y*8) for x,y in data],fill='white',width=13,joint='curve')
                for x,y in data: draw.ellipse((x*8-6,y*8-6,x*8+6,y*8+6),fill='white')
                svg.append('<polyline points="'+' '.join(f'{x},{y}' for x,y in data)+'"/>')
        glyphs[name]=pen.glyph(); cp[int(codepoints[name][2:],16)]=name; metrics[name]=(960,0)
        (ROOT/f'nivorat_nv_icons_pack/svg/{name}.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">'+''.join(svg)+'</svg>',encoding='utf-8')
        image.resize((96,96),Image.Resampling.LANCZOS).save(ASSETS/f'textures/icons/{name}.png')
        for alias,target in texture_alias.items():
            if target==name: image.resize((96,96),Image.Resampling.LANCZOS).save(ASSETS/f'textures/icons/{alias}.png')
    pen=TTGlyphPen(None); glyphs['.notdef']=pen.glyph(); metrics['.notdef']=(960,0)
    builder=FontBuilder(960,isTTF=True); builder.setupGlyphOrder(['.notdef']+list(SHAPES)); builder.setupCharacterMap(cp)
    builder.setupGlyf(glyphs); builder.setupHorizontalMetrics(metrics); builder.setupHorizontalHeader(ascent=960,descent=0)
    builder.setupNameTable({'familyName':'NV Symbols','styleName':'Regular','uniqueFontIdentifier':'NV Symbols 2','fullName':'NV Symbols','psName':'NVSymbols','version':'Version 2.0'})
    builder.setupOS2(sTypoAscender=960,sTypoDescender=0,usWinAscent=960,usWinDescent=0); builder.setupPost(); builder.setupMaxp()
    ttf=WORK/'NVSymbols.ttf'; builder.save(ttf)
    shutil.copyfile(ttf, ASSETS/'fonts/nv/nv.ttf')
    atlas(ttf,atlas_json.with_suffix(''),'[0xe001,0xe02f]')
    meta=json.loads(atlas_json.read_text()); by_cp={g['unicode']:g for g in meta['glyphs']}
    aliases={'p':'visuals','j':'interface','r':'utils','B':'themes','h':'modules','f':'settings','q':'search','s':'scale','L':'pinned','x':'close','v':'chevron-down','c':'check','k':'bind','g':'speed','A':'heart','i':'pickaxe','⊹':'pinned','\ue104':'import','\ue105':'export','\ue106':'close'}
    for alias,name in aliases.items():
        entry=dict(by_cp[int(codepoints[name][2:],16)]); entry['unicode']=ord(alias); meta['glyphs'].append(entry)
    atlas_json.write_text(json.dumps(meta,separators=(',',':')),encoding='utf-8')
    (ROOT/'nivorat_nv_icons_pack/codepoints.json').write_text(json.dumps(codepoints,indent=2)+'\n',encoding='utf-8')
    manifest = {'pack':'Nivorat NV Icons','version':'2.0','grid':'24x24','stroke_width':1.6,
                'linecap':'round','linejoin':'round','format':'SVG outline','count':len(SHAPES),
                'icons':[{'name':name,'file':f'svg/{name}.svg','codepoint':codepoints[name]} for name in SHAPES]}
    (ROOT/'nivorat_nv_icons_pack/manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    print('NV Symbols:',len(SHAPES),'original icons')

if __name__=='__main__':
    with ThreadPoolExecutor(max_workers=2) as pool: list(pool.map(build_text,[('regular',400),('medium',500),('semibold',600),('bold',700)]))
    build_icons()
