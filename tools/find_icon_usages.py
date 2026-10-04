import os, re

patterns = [
    r'Fonts\.NV',
    r'NvIcons',
    r'textures/icons/',
    r'AccentGradient\.msdfIcon',
    r'iconChar',
    r'forCategory',
    r'MusicHudComp',
    r'ScoreboardComp',
    r'CooldownHUD',
    r'Hotkeys',
    r'Potions',
    r'ArrayList',
    r'ThemesRenderer',
    r'InspectorRenderer',
    r'SelectSetting',
    r'SearchField'
]

combined = re.compile('|'.join(patterns))

matches = []
for root, dirs, files in os.walk('src'):
    for f in files:
        if f.endswith('.java'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8', errors='ignore') as fp:
                for line_no, line in enumerate(fp, 1):
                    if combined.search(line):
                        matches.append((path, line_no, line.strip()))

print('Total matching lines:', len(matches))
by_file = {}
for p, l, s in matches:
    by_file.setdefault(p, []).append((l, s))

for f, lines in sorted(by_file.items()):
    print(f'\n=== {f} ({len(lines)} occurrences) ===')
    for l, s in lines[:8]:
        print(f'  L{l}: {s}')
    if len(lines) > 8:
        print(f'  ... and {len(lines) - 8} more')
