from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
MODULES_DIR = BASE_DIR / "modules"
OUTPUT_DIR = BASE_DIR / "output"
LOG_DIR = BASE_DIR / "logs"

DEFAULT_LANG = "lua"
SUPPORTED_LANGS = ["lua"]

def get_module_dir(lang: str):
    d = MODULES_DIR / f"{lang}_ofuscator"
    if not d.exists():
        raise NotImplementedError(f"modulo no encontrado: {lang}")
    return d

def ensure_dirs():
    OUTPUT_DIR.mkdir(exist_ok=True)
    LOG_DIR.mkdir(exist_ok=True)
