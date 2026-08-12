# LOLOCORD - APK & Native Library Decompiler Toolkit

Herramienta de análisis y descompilación para APKs Android y librerías nativas (`.so`).

## Inicio rápido

```bash
# Build the native decompiler binary (no Python deps required)
make

# Analyze any .so file
./dl il2cpp --info      # Info del ELF
./dl il2cpp --patch     # Buscar parches SUB→ADD
./dl il2cpp --disasm    # Disassembly completo
```

## Características

| Feature | Descripción |
|---------|-------------|
| **Multi-arquitectura** | ARM64, ARM, x86, x86_64 |
| **Sin dependencias** | Binario C puro, compila con `make` |
| **Manejo de grandes archivos** | Usa `mmap`, soporta hasta 750MB+ |
| **Filename smart-resolve** | Escribe solo "il2cpp" o "libunity" |
| **Detección de parches** | Encuentra instrucciones ADD→SUB modificadas |

## Uso

### Información del ELF (`--info`)
```bash
./dl decompiler/output/.../libil2cpp.so --info
# [∗] Architecture: ARM64 (machine=0xb7)
# [∗] Found 3 code sections: .text, il2cpp, .plt
```

### Detección de parches --patch`
```bash
./dl il2cpp --patch
# Escanea por instrucciones ADD con inmediatos 1000-65535
# (patrón típico de hack: SUB was changed to ADD)
```

### Disassembly completo ( --disasm`
```bash
./dl libunity.so --disasm
# Usa objdump para disassembly completo
```

### Filename smart-resolve
```bash
./dl il2cpp          # Encuentra libil2cpp.so automáticamente
./dl libunity        # Encuentra libunity.so
./dl ones            # Busca *ones*.so en cwd y subdirectorios
```

## Cómo funciona el hack del oro

El modificador GameKiller parchea `libil2cpp.so` a nivel binario:

1. **Original**: método `set_Gold` ejecuta `gold -= amount` → instrucción `SUB`
2. **Hack**: cambia `SUB` a `ADD` → `gold += amount` (ganas oro al gastar)
3. **Detección**: `--patch` escanea por instructuras `ADD` con inmediates sospechosos

## Estructura del proyecto

```
LOLOCORD/
├── decompiler/
│   ├── decompile_lib.c     # Código fuente C del decompilador nativo
│   ├── decompile_lib.py    # Versión Python (con capstone)
│   ├── decompile.py        # Wrapper JADX + Apktool
│   ├── ANALYSIS_REPORT.md  # Reporte del análisis del APK
│   ├── README.md           # Esta guía
│   ├── tools/              # Herramientas externas (jadx, apktool)
│   ├── apks/               # APKs a analizar
│   └── output/             # Resultados de descompilación
├── Makefile                # Build del decompilador nativo
└── core/                   # Módulos del ofuscador Lua
```

## Análisis del APK analizado

### Epic Stickman Mod MWahyudiStar
- **Package:** `com.fansipan.epic.stickman.survival.rpg.idle.game`
- **Mod:** GameKiller (`com.game.killer`)
- **Motor:** Unity (libil2cpp.so 149MB)
- **Entry Point:** `gamesActivity` (splash) → `UnityPlayerActivity`

### Métodos de oro encontrados
- `get_Gold`, `set_Gold`, `GetGold`, `GetGoldReward`
- `SetGoldDropValue`, `OnGoldEarnEffectAdd`, `GetGoldDrop`

### SDKs integrados
AdMob, AppLovin, Unity Ads, IronSource, Vungle, Facebook, Bytedance, Mintegral

## Herramientas incluidas

| Herramienta | Uso |
|-------------|-----|
| `dl` | Decompilador nativo (.so) |
| `decompile.py` | Wrapper JADX + Apktool |
| `decompile_lib.py` | Versión Python del decompilador nativo |
