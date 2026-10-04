import os, re

emoji_pattern = re.compile(r'[\U00010000-\U0010ffff]|[\u2600-\u27bf]|[\u2300-\u23ff]|[\u2b50-\u2b55]|[\u203c-\u2049]|[\u25aa-\u25fe]|[\u2713-\u2718]')

found_emojis = []
for root, dirs, files in os.walk('src'):
    for f in files:
        if f.endswith('.java'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8', errors='ignore') as fp:
                for line_no, line in enumerate(fp, 1):
                    emojis = emoji_pattern.findall(line)
                    if emojis:
                        found_emojis.append((path, line_no, emojis, line.strip()))

print('Found emoji occurrences:', len(found_emojis))
for p, l, em, s in found_emojis:
    cps = [f'U+{ord(c):04X}' for c in em]
    print(f'{p}:{l} -> {cps}')
