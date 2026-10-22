# Singularity Reactors

Mod de Mindustry con reactores de alta potencia, nodos de energía y bloques de gestión de red.

- **Mindustry**: v160.3+
- **Java**: 17+
- **Versión**: 2.0.2

## Bloques

### Reactores (ConsumeGenerator)

| Reactor | Tamaño | Energía | Consumo principal |
|---------|--------|---------|-------------------|
| Alpha   | 5×5    | 50k/s   | Agua 12/s, Cryo 8/s, Plastanium 3/s, Thorium 2/s |
| Beta    | 6×6    | 80k/s   | Agua 20/s, Cryo 12/s, Plastanium 4/s, Thorium 3/s |
| Gamma   | 7×7    | 150k/s  | + Slag 10/s |
| Omega   | 8×8    | 250k/s  | + Slag/Oil, consumo máximo |

Si faltan recursos, la producción cae (sin explosión automática).

### Nodos

- **Nodo-R**: 1×1, rango 8, 6 conexiones
- **Nodo-P**: 2×2, rango 20, 15 conexiones
- **Nodo-SC**: 2×2, rango 40, 25 conexiones (consume 500/s)

### Gestión

- **Monitor de Energía** — producción/consumo/balance + historial
- **Controller Root** — ON/OFF y cap por generador (rango 8)
- **Controller Net** — control de toda la PowerGraph + modo emergencia
- **Gestor Universal** — toggle y config item/líquido en rango
- **Liquid Controller** — niveles, alertas y toggle de bloques con líquidos

## Build

```bash
./gradlew jar
# Salida: build/libs/singularity-reactors-modDesktop.jar
```

Copia el JAR a la carpeta `mods` de Mindustry y reinicia.

## Instalación

1. Descarga el JAR de [Releases](https://github.com/literal234xzha-cpu/singularity-reactors-mod/releases)
2. Ponlo en:
   - Windows: `%APPDATA%/Mindustry/mods/`
   - Linux: `~/.local/share/Mindustry/mods/`
   - macOS: `~/Library/Application Support/Mindustry/mods/`
3. Reinicia Mindustry y activa el mod

**Importante:** no uses JARs viejos (`mindustry-singularity-modDesktop.jar` v1). Usa solo el de releases / `build/libs`.

## Autor

LOLOCORD
