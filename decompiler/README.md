# LOLOCORD - APK & Native Library Decompiler Toolkit

Herramienta de análisis y descompilación para APKs Android y librerías nativas (`.so`).

## Instalación

```bash
pip3 install capstone==5.0.9
```

## Estructura

```
decompiler/
├── tools/                    # Herramientas instaladas
│   ├── apktool.jar           # Apktool 3.0.3
│   └── bin/jadx              # JADX 1.5.6 CLI
├── apks/                     # APKs objetivo
├── output/                   # Resultados de descompilación
├── decompile_lib.py          # Decompilador de librerías nativas .so
├── decompile.py              # Wrapper para JADX + Apktool
└── ANALYSIS_REPORT.md        # Reporte de análisis
```

## Uso del decorador nativo (decompile_lib.py)

```bash
# Disassembly completo de una librería .so
python3 decompiler/decompile_lib.py <archivo.so> -o salida.asm

# Buscar parches SUB→ADD sospechosos
python3 decompiler/decompile_lib.py <archivo.so> --mode find-patch
```

### Arquitecturas soportadas
- **ARM64** (arm64-v8a)
- **ARM** (armeabi-v7a)
- **x86**
- **x86_64**

### Cómo funciona
1. Lee el header ELF y detecta arquitectura + endianness
2. Parsea secciones de encabezado para localizar código ejecutable
3. Usa Capstone para desensamblar instrucciones
4. Output: direcciones, mnemónicos, operandos y opcodes en hex

### find-patch mode
Busca instrucciones `ADD`/`SUB` con inmediatos en range 1000-65535. Útil para detectar parches de tipo:
```
Original:  SUB x0, x1, #0x3E8    (resta 1000)
Parcheado: ADD x0, x1, #0x3E8    (suma 1000)
```

## Cómo decompilar un APK

```bash
# Apktool: resources + smali
python3 decompiler/decompile.py apktool <archivo.apk>

# JADX: Java source
python3 decompiler/decompile.py jadx <archivo.apk>
```

## Resultados de prueba

| Archivo | Tamaño | Instrucciones | Modo |
|---------|--------|--------------|------|
| libapplovin...so | 845KB | 107,935 | ARM64 ✅ |
| libunity.so | 20MB | — | find-patch ✅ |
| libil2cpp.so | 149MB | — | detección ✅ |

## Créditos
- [Capstone](https://www.capstone-engine.org/) - motor de desensamblado
- [JADX](https://github.com/skylot/jadx) - decompilador dex-to-Java
- [Apktool](https://ibotpeaches.github.io/Apktool/) - extractor de recursos
- [Androguard](https://github.com/androguard/androguard) - análisis estático
