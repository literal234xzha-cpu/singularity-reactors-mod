# Singularity Reactors

[![Version](https://img.shields.io/badge/version-2.0.7-blue.svg)](https://github.com/literal234xzha-cpu/singularity-reactors-mod/releases/latest)
[![Mindustry](https://img.shields.io/badge/mindustry-v160.3%2B-orange.svg)](https://github.com/Anuken/Mindustry)
[![Download](https://img.shields.io/github/downloads/literal234xzha-cpu/singularity-reactors-mod/total)](https://github.com/literal234xzha-cpu/singularity-reactors-mod/releases/latest)

Reactores de singularidad extremadamente potentes con un sistema completo de gestion energetica y logistica.

---

## Reactores (4 variantes)

| Bloque | Produccion | Tamano |
|--------|-----------|--------|
| Reactor Alpha | 50,000/s | 5x5 |
| Reactor Beta  | 80,000/s | 6x6 |
| Reactor Gamma | 150,000/s | 7x7 |
| Reactor Omega | 250,000/s | 8x8 |

Hasta **40x mas potentes** que el Impact Reactor vanilla.

---

## Sistema de Gestion

### Monitores
- **Monitor Local** — monitorea solo su red conectada en tiempo real
- **Monitor Global** — escanea todas las redes en 40 tiles y suma produccion/consumo total
- **Monitor de Baterias** — nivel de carga, alertas y control por bateria

### Controladores de Energia
- **Controlador Root** — control directo de generadores: toggle ON/OFF y cap de produccion %
- **Controlador Net** — gestor de red completa: cap global, modo emergencia, control por categoria

### Controladores de Logistica
- **Controlador de Items** — cintas, sorters y routers con cambio de item desde el panel
- **Controlador de Liquidos** — tuberias y tanques con alertas de nivel bajo
- **Gestor Universal** — controla CUALQUIER bloque configurable en rango

---

## Nodos de Energia

| Bloque | Rango | Conexiones |
|--------|-------|-----------|
| Nodo-R | 8 tiles | 6 |
| Nodo-P | 20 tiles | 15 |
| Nodo-SC | 40 tiles | 25 |

---

## Instalacion

Descarga `singularity-reactors-mod.jar` desde [Releases](https://github.com/literal234xzha-cpu/singularity-reactors-mod/releases/latest) y copialo a tu carpeta de mods.

- **Android**: usar importar mod desde el juego
- **Windows**: `%APPDATA%\Mindustry\mods\`
- **Linux**: `~/.local/share/Mindustry/mods/`

---

## Compatibilidad

- Mindustry v160.3+
- Compatible con Omaloon, Erekir Liquefied y cualquier mod que use la API estandar
- Java 8+, Android compatible
