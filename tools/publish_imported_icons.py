from pathlib import Path
import json, shutil
root=Path(__file__).resolve().parents[1]
cp=json.loads((root/'nivorat_nv_icons_pack/codepoints.json').read_text())
meta=json.loads((root/'build/icon_font/nv_atlas.json').read_text())
by={g['unicode']:g for g in meta['glyphs']}
if any('planeBounds' not in g for g in by.values()): raise RuntimeError('Empty icon in generated font')
aliases={'p':'visuals','j':'interface','r':'utils','B':'themes','h':'modules','f':'settings','q':'search','s':'scale','L':'pinned','x':'close','v':'chevron-down','c':'check','k':'bind','g':'speed','A':'heart','i':'pickaxe','⊹':'pinned','\ue104':'import','\ue105':'export','\ue106':'close'}
for char,name in aliases.items():
    glyph=dict(by[int(cp[name][2:],16)]);glyph['unicode']=ord(char);meta['glyphs'].append(glyph)
out=root/'src/main/resources/assets/nv/fonts/nv'
(out/'nv.json').write_text(json.dumps(meta,separators=(',',':')),encoding='utf-8')
shutil.copyfile(root/'build/icon_font/nv_atlas.png',out/'nv.png')
shutil.copyfile(root/'build/icon_font/nv.ttf',out/'nv.ttf')
print('Published',len(by),'nonempty icons with compatibility aliases')
