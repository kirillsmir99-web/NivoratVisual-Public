import os
import sys

# Force utf-8 for stdout
sys.stdout.reconfigure(encoding='utf-8', errors='replace')

IGNORE_DIRS = {'.git', 'build', '.gradle', '.venv', 'tmp', 'tmp_mc', 'backup', 'run', '.idea', 'bin'}
IGNORE_EXTS = {'.pyc', '.exe', '.jar', '.png', '.jpg', '.ttf', '.ogg', '.wav', '.class'}

KEYWORDS = [
    'kimiko',
    'catlavan',
    'expensive',
    'neverlose',
    'celestial',
    'fluger',
    'akrien',
    'nursultan',
    'zeroday',
    'liquidbounce',
    'chatgpt',
    'openai',
    'claude',
    'gemini',
    'deepseek',
    'antigravity',
    'нейросеть',
    'нейросети',
    'llm',
    'copilot',
]

def scan():
    results = {}
    for kw in KEYWORDS:
        results[kw] = []

    for root, dirs, files in os.walk('.'):
        dirs[:] = [d for d in dirs if d not in IGNORE_DIRS]
        for f in files:
            ext = os.path.splitext(f)[1].lower()
            if ext in IGNORE_EXTS:
                continue
            path = os.path.join(root, f)
            # Skip audit_branding.py itself and docs/rules
            if 'audit_branding.py' in path:
                continue
            try:
                with open(path, 'r', encoding='utf-8', errors='ignore') as fp:
                    for lno, line in enumerate(fp, 1):
                        clean_line = (
                            line.replace('C:\\Users\\Administrator\\Desktop\\Kimiko', '')
                                .replace('C:/Users/Administrator/Desktop/Kimiko', '')
                                .replace('Desktop\\Kimiko', '')
                                .replace('Desktop/Kimiko', '')
                        )
                        lower_clean = clean_line.lower()
                        for kw in KEYWORDS:
                            if kw in lower_clean:
                                results[kw].append((path, lno, line.strip()))
            except Exception as e:
                pass

    total = 0
    for kw, matches in results.items():
        if matches:
            print(f"=== Keyword: '{kw}' ({len(matches)} matches) ===")
            for path, lno, text in matches:
                # filter out false positives if needed
                print(f"  {path}:{lno}: {text[:140]}")
            total += len(matches)
            print()
    print(f"Total keyword matches: {total}")

if __name__ == '__main__':
    scan()
