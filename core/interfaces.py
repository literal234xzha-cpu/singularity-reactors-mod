from abc import ABC, abstractmethod

class ObfuscatorModule(ABC):
    """Interfaz base para modulos de ofuscacion por lenguaje."""

    @property
    @abstractmethod
    def lang(self) -> str:
        ...

    @abstractmethod
    def obfuscate(self, src: str, **kwargs) -> str:
        """Recibe codigo fuente, devuelve codigo ofuscado."""
        ...

    @property
    def options(self) -> dict:
        return {}
