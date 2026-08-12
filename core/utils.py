import hashlib, os
from core.config import LOG_DIR

def log_event(msg: str, level: str = "INFO"):
    LOG_DIR.mkdir(exist_ok=True)
    with open(LOG_DIR / "manager.log", "a") as f:
        f.write(f"[{level}] {msg}\n")

def sha1_id(s: str) -> str:
    return hashlib.sha1(s.encode("utf-8")).hexdigest()[:12]

def xor_bytes(data: bytes, key: bytes) -> bytes:
    return bytes(b ^ key[i % len(key)] for i, b in enumerate(data))
