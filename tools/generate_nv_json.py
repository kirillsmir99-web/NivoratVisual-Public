import json
import shutil

# 1. Parse TTF post table to map glyph index to name
with open('build/icon_font/nv.ttf', 'rb') as f:
    data = f.read()

num_tables = int.from_bytes(data[4:6], 'big')
post_offset = None
for i in range(num_tables):
    tag = data[12+i*16:16+i*16].decode('latin1')
    if tag == 'post':
        post_offset = int.from_bytes(data[20+i*16:24+i*16], 'big')
        break

num_glyphs = int.from_bytes(data[post_offset+32:post_offset+34], 'big')
glyph_name_indices = []
for i in range(num_glyphs):
    glyph_name_indices.append(int.from_bytes(data[post_offset+34+i*2:post_offset+36+i*2], 'big'))

str_offset = post_offset + 34 + num_glyphs * 2
custom_names = []
while str_offset < len(data):
    length = data[str_offset]
    str_offset += 1
    if str_offset + length > len(data):
        break
    custom_names.append(data[str_offset:str_offset+length].decode('latin1', errors='ignore'))
    str_offset += length

glyph_index_to_name = {}
for i, gni in enumerate(glyph_name_indices):
    if gni >= 258 and (gni - 258) < len(custom_names):
        glyph_index_to_name[i] = custom_names[gni - 258]

# 2. Load Fantasticon mapping
with open('build/icon_font/nv.json', 'r', encoding='utf-8') as f:
    name_to_codepoint = json.load(f)

# 3. Load msdf-atlas-gen output
with open('build/icon_font/nv_atlas.json', 'r', encoding='utf-8') as f:
    atlas_gen = json.load(f)

# 4. Construct final nv.json
legacy_aliases = {
    'p': 'visuals',
    'j': 'interface',
    'r': 'utils',
    'B': 'themes',
    'h': 'modules',
    'f': 'settings',
    'q': 'search',
    's': 'scale',
    'L': 'pinned',
    'x': 'close',
    'v': 'chevron-down',
    'c': 'check',
    'k': 'bind',
    'g': 'speed',
    '\u22B9': 'pinned'
}

glyphs_out = []
name_to_glyph_entry = {}

for g in atlas_gen['glyphs']:
    idx = g.get('index')
    if idx == 0 or idx not in glyph_index_to_name:
        continue
    name = glyph_index_to_name[idx]
    if name not in name_to_codepoint:
        continue
    unicode_val = name_to_codepoint[name]
    entry = {
        'unicode': unicode_val,
        'advance': g.get('advance', 1.0),
        'planeBounds': g['planeBounds'],
        'atlasBounds': g['atlasBounds']
    }
    glyphs_out.append(entry)
    name_to_glyph_entry[name] = entry

# Add legacy aliases
for alias_char, target_name in legacy_aliases.items():
    if target_name in name_to_glyph_entry:
        target = name_to_glyph_entry[target_name]
        alias_entry = {
            'unicode': ord(alias_char),
            'advance': target['advance'],
            'planeBounds': target['planeBounds'],
            'atlasBounds': target['atlasBounds']
        }
        glyphs_out.append(alias_entry)

final_meta = {
    'atlas': {
        'type': 'mtsdf',
        'distanceRange': atlas_gen['atlas'].get('distanceRange', 4),
        'distanceRangeMiddle': 0,
        'size': atlas_gen['atlas'].get('size', 64),
        'width': atlas_gen['atlas']['width'],
        'height': atlas_gen['atlas']['height'],
        'yOrigin': 'bottom'
    },
    'metrics': atlas_gen['metrics'],
    'glyphs': glyphs_out,
    'kerning': []
}

with open('src/main/resources/assets/nv/fonts/nv/nv.json', 'w', encoding='utf-8') as f:
    json.dump(final_meta, f, separators=(',', ':'))

shutil.copyfile('build/icon_font/nv_atlas.png', 'src/main/resources/assets/nv/fonts/nv/nv.png')
print(f'Successfully updated nv.json ({len(glyphs_out)} glyphs) and nv.png ({atlas_gen["atlas"]["width"]}x{atlas_gen["atlas"]["height"]})')
