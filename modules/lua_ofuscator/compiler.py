import struct
from pathlib import Path

from core.interfaces import ObfuscatorModule
from core.utils import sha1_id, log_event
from core.manager import register

@register("lua")
class Compiler(ObfuscatorModule):
    lang = "lua"

    def __init__(self):
        base = Path(__file__).resolve().parent
        self.vm_template = (base / "assets" / "vm_loader.lua").read_text()
        self._seed_counter = 0x9E3779B9

    @staticmethod
    def _xor(data: bytes, keys: list[int]) -> bytes:
        return bytes(b ^ keys[i % len(keys)] for i, b in enumerate(data))

    @staticmethod
    def _keystream(n: int, seed: int) -> list[int]:
        s = seed % 2147483647
        out = []
        for _ in range(n):
            s = (s * 1103515245 + 12345) % 2147483648
            out.append(s % 256)
        return out

    @staticmethod
    def _split_bytes(payload: bytes) -> bytes:
        out = bytearray()
        for b in payload:
            out.append(b // 100)
            out.append(b % 100)
        return bytes(out)

    def _chunk(self, payload: bytes) -> str:
        header = b"\x01" + struct.pack("<H", len(payload))
        raw = header + payload + b"\x03"
        split = self._split_bytes(raw)
        seed = self._seed_counter & 0x7fffffff
        self._seed_counter = (seed * 69069 + 1) & 0x7fffffff
        keys = self._keystream(len(split), seed)
        enc = self._xor(split, keys)
        data_esc = "".join(f"\\{b:03d}" for b in enc)
        return f"{{{seed},\"{data_esc}\"}}"

    def obfuscate(self, src: str, **kwargs) -> str:
        log_event(f"ofuscando lua (id={sha1_id(src)}) seed=0x{self._seed_counter:08x}")
        chunks = [self._chunk(src.encode("utf-8"))]
        arr = "{" + ",".join(chunks) + "}"
        loader = self.vm_template.replace("@@CHUNKS@@", arr)
        return loader

    @property
    def options(self) -> dict:
        return {"xor_keystream": "xorshift32", "split_bytes": "/100", "anti_debug": True}
