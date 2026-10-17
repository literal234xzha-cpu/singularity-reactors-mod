# Singularity Reactors - Mod de Mindustry

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)]()
[![Mindustry](https://img.shields.io/badge/mindustry-v160.3%2B-orange.svg)]()

Mod de Mindustry que agrega reactores de singularidad extremadamente potentes con sistema eléctrico realista basado en matemáticas de física aplicada.

---

## 🌟 Características Principales

### ⚡ Sistema de Energía Realista
- **Pérdida de resistencia**: Los cables pierden energía por distancia
- **Amplificadores**: Compensan pérdidas en redes largas
- **Matemáticas reales**: Basado en Ley de Ohm y resistencia eléctrica

### ☢️ Reactores de Singularidad (4 Variantes)
Reactores masivos que generan cantidades brutales de energía, pero requieren recursos constantes y pueden explotar catastróficamente si fallan.

---

## 📊 Bloques Disponibles

### Nodos de Energía

#### 1. **Resistive Power Node** (Nodo Resistivo)
- **Tamaño**: 1×1
- **Conexiones**: 6 máximo
- **Rango**: 8 tiles
- **Costo**: 3 Cobre, 2 Plomo
- 📝 Nodo básico con pérdida moderada de energía

#### 2. **Heavy Power Node** (Nodo Pesado)
- **Tamaño**: 2×2
- **Conexiones**: 15 máximo
- **Rango**: 20 tiles
- **Costo**: 5 Titanio, 3 Silicio, 8 Cobre
- 📝 Nodo mejorado con menos pérdidas

#### 3. **Superconductor Node** (Nodo Superconductor)
- **Tamaño**: 2×2
- **Conexiones**: 25 máximo
- **Rango**: 40 tiles
- **Costo**: 2 Phase Fabric, 4 Surge Alloy, 10 Silicio
- **Consumo**: 500 energía/s (para superconductividad)
- 📝 Casi sin pérdidas, requiere energía para funcionar

---

### ☢️ Reactores de Singularidad

#### Reactor 1: **Hiper-Impacto** ⭐
![Estable](https://img.shields.io/badge/estabilidad-ALTA-green.svg)

- **Tamaño**: 5×5
- **Producción Neta**: 50,000 energía/s
- **Vida**: 3000 HP

**Consumo por segundo:**
- 💧 2.5 Agua
- ❄️ 1.5 Criofluido
- 🔩 8 Plastanio
- ☢️ 6 Torio

**Explosión:**
- Radio: 35 tiles
- Daño: 8,000

**Costo de Construcción:**
```
800 Plastanio
600 Torio
1000 Titanio
400 Surge Alloy
600 Silicio
```

✅ **Recomendado para**: Primeras bases de energía masiva. Más estable y predecible.

---

#### Reactor 2: **Impacto Singularidad** ⭐⭐
![Balanceado](https://img.shields.io/badge/estabilidad-MEDIA-yellow.svg)

- **Tamaño**: 6×6
- **Producción Neta**: 80,000 energía/s
- **Vida**: 4500 HP

**Consumo por segundo:**
- 💧 4 Agua
- ❄️ 2.5 Criofluido
- 🔩 12 Plastanio
- ☢️ 10 Torio

**Explosión:**
- Radio: 50 tiles
- Daño: 15,000

**Costo de Construcción:**
```
1200 Plastanio
900 Torio
1500 Titanio
700 Surge Alloy
800 Silicio
200 Phase Fabric
```

⚖️ **Recomendado para**: Balance entre producción y riesgo.

---

#### Reactor 3: **Neodimio** ⭐⭐⭐
![Peligroso](https://img.shields.io/badge/estabilidad-BAJA-orange.svg)

- **Tamaño**: 7×7
- **Producción Neta**: 150,000 energía/s
- **Vida**: 6000 HP

**Consumo por segundo (MUY COMILÓN):**
- 💧 8 Agua
- ❄️ 5 Criofluido
- 🔥 2 Slag
- 🔩 20 Plastanio
- ☢️ 18 Torio

**Explosión:**
- Radio: 70 tiles
- Daño: 25,000

**Costo de Construcción:**
```
1800 Plastanio
1400 Torio
2000 Titanio
1200 Surge Alloy
1000 Silicio
500 Phase Fabric
```

⚠️ **Recomendado para**: Bases establecidas con cadenas de suministro sólidas.

---

#### Reactor 4: **Impactador** ⭐⭐⭐⭐
![EXTREMO](https://img.shields.io/badge/estabilidad-CR%C3%8DTICA-red.svg)

- **Tamaño**: 8×8
- **Producción Neta**: 250,000 energía/s
- **Vida**: 8000 HP

**Consumo por segundo (BRUTALMENTE COMILÓN):**
- 💧 15 Agua
- ❄️ 10 Criofluido
- 🔥 5 Slag
- 🛢️ 3 Aceite
- 🔩 35 Plastanio
- ☢️ 30 Torio

**Explosión MASIVA:**
- Radio: 120 tiles (¡ARRASA CON TODO!)
- Daño: 50,000
- Efectos: Charcos de slag en área enorme

**Costo de Construcción:**
```
3000 Plastanio
2500 Torio
3500 Titanio
2000 Surge Alloy
1500 Silicio
1000 Phase Fabric
```

💀 **ADVERTENCIA**: Este reactor es extremadamente peligroso. Su explosión puede destruir bases enteras. Solo para jugadores experimentados con infraestructura masiva.

---

## ⚙️ Mecánicas del Reactor

### Sistema de 3 Fases (Basado en ImpactReactor)

Los reactores funcionan como el Impact Reactor vanilla pero con stats mejorados:

1. **Fase de Calentamiento (Warmup)**
   - El reactor aumenta gradualmente su producción
   - Requiere todos los recursos constantemente
   - Tiempo de calentamiento varía según el reactor

2. **Operación Normal**
   - Producción al 100% cuando todos los recursos están disponibles
   - Consumo constante de líquidos e items
   - Si falta algún recurso, la producción cae

3. **Sobrecarga/Explosión**
   - Si los recursos faltan por mucho tiempo, el reactor se vuelve inestable
   - La explosión es proporcional al tamaño y potencia del reactor
   - **NO HAY ADVERTENCIA** - Asegura suministro constante

---

## 📐 Matemáticas Reales

### Producción de Energía
```
Energía Neta = Producción Total - Consumo Interno
```

| Reactor | Producción Bruta | Consumo Interno | Neto |
|---------|------------------|-----------------|------|
| Hiper-Impacto | 50,000/s | ~0 | 50,000/s |
| Impacto Singularidad | 80,000/s | ~0 | 80,000/s |
| Neodimio | 150,000/s | ~0 | 150,000/s |
| Impactador | 250,000/s | ~0 | 250,000/s |

### Comparación con Reactores Vanilla

| Reactor | Energía/s | vs Impact Reactor (6200/s) |
|---------|-----------|----------------------------|
| Hiper-Impacto | 50,000 | **8× más potente** |
| Impacto Singularidad | 80,000 | **13× más potente** |
| Neodimio | 150,000 | **24× más potente** |
| Impactador | 250,000 | **40× más potente** |

---

## 🎮 Guía de Uso

### Para Principiantes: Hiper-Impacto

1. **Construye infraestructura de agua**:
   - 2-3 extractores de agua
   - Tuberías al reactor

2. **Construye infraestructura de criofluido**:
   - Mezclador de cryo
   - Suministro constante

3. **Cadena de materiales**:
   - Minas de torio
   - Fábrica de plastanio
   - Transportadores al reactor

4. **Red eléctrica**:
   - Usa Heavy Power Nodes para distribuir
   - Superconductor Nodes para largas distancias

### Para Expertos: Impactador

⚠️ **REQUISITOS PREVIOS**:
- Base completamente establecida
- Múltiples fuentes de cada recurso
- Sistema de respaldo de energía
- **Búnkers de protección** alrededor del reactor

**Consumo por minuto:**
- 900 Agua
- 600 Criofluido
- 300 Slag
- 180 Aceite
- 2,100 Plastanio
- 1,800 Torio

💡 **Tip**: Construye el Impactador lejos de tu base principal. Si explota, la zona de 120 tiles de radio será devastada.

---

## 📦 Instalación

### Método 1: Desde JAR

1. Descarga `mindustry-singularity-modDesktop.jar`
2. Copia el archivo a:
   - **Windows**: `%APPDATA%/Mindustry/mods/`
   - **Linux**: `~/.local/share/Mindustry/mods/`
   - **MacOS**: `~/Library/Application Support/Mindustry/mods/`
3. Reinicia Mindustry
4. Habilita el mod en `Mods > Singularity Reactors`

### Método 2: Desde Código Fuente

```bash
git clone [repositorio]
cd mindustry-singularity-mod
./gradlew jar
cp build/libs/mindustry-singularity-modDesktop.jar ~/.local/share/Mindustry/mods/
```

---

## ⚠️ Advertencias de Seguridad

### 🔥 PELIGRO DE EXPLOSIÓN

Todos los reactores pueden explotar si:
- Se quedan sin agua o criofluido
- Se interrumpe el suministro de materiales
- Son destruidos por enemigos

### 💥 Radios de Destrucción

```
Hiper-Impacto:    35 tiles  [███░░░░░░░]
Impacto Sing.:    50 tiles  [█████░░░░░]
Neodimio:         70 tiles  [███████░░░]
Impactador:      120 tiles  [██████████] TOTAL
```

### 🛡️ Consejos de Seguridad

1. **Siempre construye muros de protección** alrededor del reactor
2. **Usa múltiples fuentes de recursos** para redundancia
3. **Monitorea constantemente** el suministro
4. **Ten baterías de respaldo** para emergencias
5. **NUNCA construyas el Impactador cerca de tu base principal**

---

## 🔧 Compatibilidad

- **Mindustry**: v160.3+
- **Java**: 17+
- **Plataformas**: Desktop (Windows, Linux, MacOS), Android

### Mods Compatibles
- ✅ Todos los mods de contenido estándar
- ✅ Mods de turrets y defensa
- ✅ Mods de logística

---

## 📝 Changelog

### v1.0.0 (2026-10-17)
- ✨ Lanzamiento inicial
- ⚡ 4 reactores de singularidad
- 🔌 3 nodos de energía mejorados
- 🎨 Sprites generados proceduralmente
- 📚 Documentación completa

---

## 👨‍💻 Autor

**LOLOCORD**

---

## 📜 Licencia

Este mod es software libre. Puedes modificarlo y redistribuirlo.

---

## 🙏 Agradecimientos

- **Anuke** - Creador de Mindustry y la API de modding
- **Comunidad de Mindustry** - Por el soporte y feedback
- **MindustryJavaModTemplate** - Template base usado para este mod

---

## 🐛 Reportar Bugs

Si encuentras bugs o tienes sugerencias:
1. Describe el problema detalladamente
2. Incluye la versión del mod y de Mindustry
3. Adjunta screenshots si es posible

---

## 💡 Ideas Futuras

- [ ] Sistema de cables con pérdida de resistencia progresiva
- [ ] Amplificadores de energía
- [ ] Tuberías con presión y despresurización
- [ ] Bombas presurizadoras
- [ ] Sistema de 3 fases de encendido para reactores
- [ ] Más variantes de reactores
- [ ] Efectos visuales mejorados

---

**¡Disfruta generando energía masiva y trata de no explotar tu base! ⚡💥**
