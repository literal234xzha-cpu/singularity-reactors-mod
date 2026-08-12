import importlib, inspect

from core.config import get_module_dir
from core.interfaces import ObfuscatorModule

_REGISTRY: dict[str, type[ObfuscatorModule]] = {}

def register(lang: str):
    def deco(cls: type):
        _REGISTRY[lang] = cls
        return cls
    return deco

def get_obfuscator(lang: str) -> ObfuscatorModule:
    if lang in _REGISTRY:
        return _REGISTRY[lang]()
    mod_dir = get_module_dir(lang)
    mod_path = f"modules.{lang}_ofuscator.compiler"
    mod = importlib.import_module(mod_path)
    cls = getattr(mod, "Compiler")
    if not issubclass(cls, ObfuscatorModule):
        raise TypeError(f"{cls} no hereda ObfuscatorModule")
    _REGISTRY[lang] = cls
    return cls()

def list_supported() -> list[str]:
    return list(_REGISTRY) or ["lua"]
