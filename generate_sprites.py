#!/usr/bin/env python3
"""
Generador de sprites para Reactores de Singularidad
Crea sprites visuales para los 4 reactores con diferentes colores y diseños
"""

from PIL import Image, ImageDraw
import os

# Crear directorio de sprites
os.makedirs("mindustry-singularity-mod/assets/sprites/blocks/power", exist_ok=True)

def create_reactor_sprite(size, name, core_color, ring_color, detail_color, complexity):
    """
    Crea un sprite de reactor con anillos concéntricos
    
    Args:
        size: Tamaño en tiles (5, 6, 7, 8)
        name: Nombre del archivo
        core_color: Color del núcleo central
        ring_color: Color de los anillos
        detail_color: Color de detalles
        complexity: Nivel de complejidad visual (1-4)
    """
    # Tamaño en pixels (32px por tile)
    px_size = size * 32
    img = Image.new('RGBA', (px_size, px_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    center = px_size // 2
    
    # Base oscura (estructura)
    base_color = (40, 40, 45, 255)
    draw.rectangle([0, 0, px_size, px_size], fill=base_color, outline=(60, 60, 70, 255), width=2)
    
    # Detalles de estructura (paneles)
    panel_color = (50, 50, 60, 255)
    panel_size = 8
    for x in range(0, px_size, 16):
        for y in range(0, px_size, 16):
            if (x + y) % 32 == 0:
                draw.rectangle([x, y, x+panel_size, y+panel_size], fill=panel_color)
    
    # Anillos concéntricos (aumentan con complejidad)
    num_rings = complexity + 1
    for i in range(num_rings, 0, -1):
        radius = (center - 10) * (i / num_rings)
        
        # Anillo exterior
        draw.ellipse(
            [center-radius, center-radius, center+radius, center+radius],
            fill=None,
            outline=ring_color,
            width=3
        )
        
        # Anillo interior más brillante
        inner_radius = radius - 4
        if inner_radius > 0:
            draw.ellipse(
                [center-inner_radius, center-inner_radius, 
                 center+inner_radius, center+inner_radius],
                fill=None,
                outline=detail_color,
                width=2
            )
    
    # Núcleo central brillante (singularidad)
    core_radius = 12 + (complexity * 3)
    
    # Glow exterior
    for i in range(5, 0, -1):
        alpha = 50 * i
        glow_radius = core_radius + (i * 2)
        glow_color = (*core_color[:3], alpha)
        draw.ellipse(
            [center-glow_radius, center-glow_radius,
             center+glow_radius, center+glow_radius],
            fill=glow_color
        )
    
    # Núcleo sólido
    draw.ellipse(
        [center-core_radius, center-core_radius,
         center+core_radius, center+core_radius],
        fill=core_color,
        outline=(255, 255, 255, 255),
        width=2
    )
    
    # Detalles de energía (rayos)
    for angle in range(0, 360, 360 // (complexity * 4)):
        import math
        rad = math.radians(angle)
        start_x = center + int(math.cos(rad) * (core_radius + 5))
        start_y = center + int(math.sin(rad) * (core_radius + 5))
        end_x = center + int(math.cos(rad) * (center - 15))
        end_y = center + int(math.sin(rad) * (center - 15))
        
        draw.line([start_x, start_y, end_x, end_y], fill=detail_color, width=2)
    
    # Marcadores de esquina (indicadores de peligro)
    danger_size = 6
    danger_color = (255, 100 * (5 - complexity), 0, 255)  # Más rojo = más peligroso
    positions = [
        (5, 5),  # Esquina superior izquierda
        (px_size - danger_size - 5, 5),  # Superior derecha
        (5, px_size - danger_size - 5),  # Inferior izquierda
        (px_size - danger_size - 5, px_size - danger_size - 5)  # Inferior derecha
    ]
    
    for x, y in positions:
        draw.rectangle([x, y, x+danger_size, y+danger_size], fill=danger_color)
        draw.rectangle([x+1, y+1, x+danger_size-1, y+danger_size-1], 
                      fill=(255, 255, 0, 200))
    
    # Guardar
    output_path = f"mindustry-singularity-mod/assets/sprites/blocks/power/{name}.png"
    img.save(output_path)
    print(f"✓ Creado: {output_path} ({px_size}x{px_size}px)")

def create_power_node_sprite(size, name, color, max_connections):
    """Crea sprites de nodos de energía"""
    px_size = size * 32
    img = Image.new('RGBA', (px_size, px_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    center = px_size // 2
    
    # Base metálica
    base_color = (80, 80, 90, 255)
    draw.rectangle([0, 0, px_size, px_size], fill=base_color, outline=(100, 100, 110, 255), width=2)
    
    # Torre central
    tower_width = px_size // 3
    tower_color = (60, 60, 70, 255)
    draw.rectangle(
        [center-tower_width//2, 5, center+tower_width//2, px_size-5],
        fill=tower_color,
        outline=(120, 120, 130, 255),
        width=2
    )
    
    # Núcleo de energía
    core_radius = tower_width // 2
    draw.ellipse(
        [center-core_radius, center-core_radius,
         center+core_radius, center+core_radius],
        fill=color,
        outline=(255, 255, 255, 255),
        width=2
    )
    
    # Indicadores de conexión
    conn_size = 4
    angles = [0, 90, 180, 270]
    if max_connections > 10:
        angles += [45, 135, 225, 315]
    
    for angle in angles[:min(8, max_connections)]:
        import math
        rad = math.radians(angle)
        x = center + int(math.cos(rad) * (px_size // 2 - 8))
        y = center + int(math.sin(rad) * (px_size // 2 - 8))
        draw.ellipse([x-conn_size, y-conn_size, x+conn_size, y+conn_size], fill=color)
    
    output_path = f"mindustry-singularity-mod/assets/sprites/blocks/power/{name}.png"
    img.save(output_path)
    print(f"✓ Creado: {output_path} ({px_size}x{px_size}px)")

# ============= CREAR SPRITES DE REACTORES =============

print("Generando sprites para Reactores de Singularidad...\n")

# Reactor 1: Hiper-Impacto (5x5, Azul/Cyan - Estable)
create_reactor_sprite(
    size=5,
    name="singularity-reactor-hyper",
    core_color=(100, 200, 255, 255),  # Azul brillante
    ring_color=(50, 150, 200, 255),
    detail_color=(150, 230, 255, 255),
    complexity=1
)

# Reactor 2: Impacto Singularidad (6x6, Morado - Balanceado)
create_reactor_sprite(
    size=6,
    name="singularity-reactor-impact",
    core_color=(180, 100, 255, 255),  # Morado
    ring_color=(120, 50, 200, 255),
    detail_color=(220, 150, 255, 255),
    complexity=2
)

# Reactor 3: Neodimio (7x7, Verde/Amarillo - Eficiente pero comilón)
create_reactor_sprite(
    size=7,
    name="singularity-reactor-neodymium",
    core_color=(100, 255, 150, 255),  # Verde brillante
    ring_color=(50, 200, 100, 255),
    detail_color=(200, 255, 100, 255),
    complexity=3
)

# Reactor 4: Impactador (8x8, Rojo/Naranja - PELIGROSO)
create_reactor_sprite(
    size=8,
    name="singularity-reactor-impactor",
    core_color=(255, 50, 50, 255),  # Rojo intenso
    ring_color=(255, 100, 0, 255),  # Naranja
    detail_color=(255, 200, 0, 255),  # Amarillo
    complexity=4
)

print("\n============= NODOS DE ENERGÍA =============\n")

# Nodos de energía
create_power_node_sprite(
    size=1,
    name="resistive-power-node",
    color=(255, 200, 100, 255),  # Naranja (cobre)
    max_connections=6
)

create_power_node_sprite(
    size=2,
    name="heavy-power-node",
    color=(100, 200, 255, 255),  # Azul (titanio)
    max_connections=15
)

create_power_node_sprite(
    size=2,
    name="superconductor-node",
    color=(200, 100, 255, 255),  # Morado (phase)
    max_connections=25
)

print("\n✅ Todos los sprites generados exitosamente!")
print(f"\nUbicación: mindustry-singularity-mod/assets/sprites/blocks/power/")
