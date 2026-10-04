import os
import sys
import re
import json

sys.stdout.reconfigure(encoding='utf-8')

modules_dir = 'src/main/java/rtx/nv/api/modules/impl'
all_modules = []
all_settings = set()
all_options = set()

# Pattern for super("Name", "Desc", ...)
super_pat = re.compile(r'super\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"')

# Pattern for settings constructors
setting_pat = re.compile(r'new\s+(?:[a-zA-Z0-9_.]+\.)?(Separator|Mode|Number|Boolean|Color|Select|MultiSelect)Setting\s*\(\s*"([^"]+)"')

# Pattern for ModeSetting options: ModeSetting("...", "...", "default", "opt1", "opt2", ...)
mode_pat = re.compile(r'new\s+(?:[a-zA-Z0-9_.]+\.)?(?:Mode|Select|MultiSelect)Setting\s*\(\s*"([^"]+)"\s*,\s*"([^"]*)"\s*,\s*("[^"]+"(?:\s*,\s*"[^"]+")*)')

for root, dirs, files in os.walk(modules_dir):
    for f in files:
        if f.endswith('.java'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8', errors='ignore') as jf:
                content = jf.read()
                
            m_super = super_pat.search(content)
            if m_super:
                all_modules.append((m_super.group(1), m_super.group(2)))
                
            for m in setting_pat.finditer(content):
                all_settings.add(m.group(2))

            for m in mode_pat.finditer(content):
                opts_str = m.group(3)
                opts = re.findall(r'"([^"]+)"', opts_str)
                for opt in opts:
                    all_options.add(opt)

print(f'Discovered {len(all_modules)} modules, {len(all_settings)} unique setting names, {len(all_options)} unique option names.')

with open('src/main/resources/assets/nv/lang/en_us.json', 'r', encoding='utf-8') as f:
    en_us = json.load(f)

def to_key(text):
    return re.sub(r'[^a-z0-9_\u0430-\u044f\u0451]', '', text.strip().lower().replace(' ', '_').replace('-', '_'))

missing_modules = []
for name, desc in all_modules:
    k = to_key(name)
    k_no_ = k.replace('_', '')
    has_name = f'module.{k}.name' in en_us or f'module.{k_no_}.name' in en_us
    has_desc = f'module.{k}.desc' in en_us or f'module.{k_no_}.desc' in en_us
    if not has_name or not has_desc:
        missing_modules.append((name, desc, has_name, has_desc))

missing_settings = []
for s in sorted(all_settings):
    k = to_key(s)
    k_no_ = k.replace('_', '')
    if f'setting.{k}.name' not in en_us and f'setting.{k_no_}.name' not in en_us:
        missing_settings.append(s)

missing_options = []
for o in sorted(all_options):
    k = to_key(o)
    k_no_ = k.replace('_', '')
    if f'option.{k}' not in en_us and f'option.{k_no_}' not in en_us:
        missing_options.append(o)

print(f'Missing module translations: {len(missing_modules)}')
for m in missing_modules:
    print(f'  Module: {m[0]} (has_name={m[2]}, has_desc={m[3]})')

print(f'Missing settings translations: {len(missing_settings)}')
for s in missing_settings[:30]:
    print(f'  Setting: {s}')

print(f'Missing options translations: {len(missing_options)}')
for o in missing_options[:30]:
    print(f'  Option: {o}')
