"""Prepare clean copy of NivoratVisual for public GitHub repository."""
import shutil
import subprocess
from pathlib import Path

SRC = Path(r"C:\Users\Administrator\Desktop\Kimiko")
DEST = Path(r"C:\Users\Administrator\Desktop\NivoratVisual-Public")

EXCLUDE_DIRS = {
    ".git",
    ".gradle",
    ".idea",
    ".pytest_cache",
    "tmp_mc",
    "backup",
    "backup_modules",
    "build",
}

EXCLUDE_EXTENSIONS = {
    ".log",
    ".tmp",
    ".class",
    ".pyc",
}

EXCLUDE_FILES = {
    "audit_results.txt",
    "rich_matches.txt",
    "tmp_class329.class",
    "user_plan.txt",
    "right_hud.png",
}

def prepare_public():
    if DEST.exists():
        print(f"Removing existing {DEST}...")
        shutil.rmtree(DEST)
    DEST.mkdir(parents=True, exist_ok=True)

    for item in SRC.iterdir():
        name = item.name
        if name in EXCLUDE_DIRS or name in EXCLUDE_FILES:
            continue
        if item.suffix in EXCLUDE_EXTENSIONS:
            continue

        if item.is_dir():
            dest_sub = DEST / name
            shutil.copytree(
                item,
                dest_sub,
                ignore=shutil.ignore_patterns(
                    "*.log", "*.tmp", "__pycache__", ".pytest_cache", ".DS_Store"
                ),
            )
        elif item.is_file():
            shutil.copy2(item, DEST / name)

    # Initialize a fresh git repository in DEST
    subprocess.run(["git", "init", "-b", "main"], cwd=str(DEST), check=True)
    subprocess.run(["git", "config", "user.name", "kirillsmir99-web"], cwd=str(DEST), check=True)
    subprocess.run(["git", "config", "user.email", "kirillsmir99@gmail.com"], cwd=str(DEST), check=True)

    print(f"Clean public repository prepared at {DEST}")

if __name__ == "__main__":
    prepare_public()
