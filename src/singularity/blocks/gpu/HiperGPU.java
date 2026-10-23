package singularity.blocks.gpu;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.world.blocks.logic.LogicBlock;

/**
 * HiperGPU — Procesador GPU de gama ultra.
 *
 * Equivalente a una RTX 4090 extrema / "HiperGPU".
 * - 200 instrucciones/tick (100x el micro-procesador base)
 * - Tamaño 3x3
 * - Rango de enlace: 20 tiles
 * - Capaz de ejecutar simulaciones completas, control de ejércitos de
 *   unidades, procesamiento paralelo masivo y toda la lógica de la base
 *   en un solo bloque.
 *
 * Nota: Con 200 ipt, toda la lógica de un mapa mediano corre cómoda y
 * sin lag. Equivale a tener 100 micro-procesadores coordinados.
 */
public class HiperGPU extends LogicBlock {

    public static final int IPT = 200;

    public TextureRegion glowRegion;
    public TextureRegion rotatorRegion;
    public TextureRegion topRegion;

    public HiperGPU(String name) {
        super(name);
        size = 3;
        instructionsPerTick = IPT;
        range = 8 * 20f; // 20 tiles
        health = 700;
        category = Category.logic;
    }

    @Override
    public void load() {
        super.load();
        glowRegion = Core.atlas.find(name + "-glow");
        rotatorRegion = Core.atlas.find(name + "-rotator");
        topRegion = Core.atlas.find(name + "-top");
    }

    public class HiperGPUBuild extends LogicBuild {

        private float glow = 0f;
        private float spinAngle = 0f;
        private float glowTimer = 0f;

        @Override
        public void updateTile() {
            super.updateTile();
            glowTimer += Time.delta;
            float activity = (executor != null && executor.instructions.length > 0) ? 1f : 0f;
            glow = Mathf.lerp(glow, activity, 0.05f * Time.delta);
            // La rueda del disipador gira más rápido según la carga
            spinAngle += (80f + glow * 220f) * Time.delta;
        }

        @Override
        public void draw() {
            super.draw();

            // Resplandor base ultra intenso morado/cian
            if (glow > 0.01f && glowRegion != null && glowRegion.found()) {
                float pulse = Mathf.sin(glowTimer, 45f, 0.2f) + 0.8f;
                Draw.color(Color.valueOf("AA00FF"), glow * pulse * 0.9f);
                Draw.rect(glowRegion, x, y);
                // Segunda capa cian superpuesta para efecto RGB
                float pulse2 = Mathf.sin(glowTimer + 30f, 55f, 0.15f) + 0.85f;
                Draw.color(Color.valueOf("00FFFF"), glow * pulse2 * 0.5f);
                Draw.rect(glowRegion, x, y);
                Draw.color();
            }

            // Ventilador / rotor giratorio
            if (rotatorRegion != null && rotatorRegion.found()) {
                Draw.rect(rotatorRegion, x, y, spinAngle);
            }

            // Cubierta superior
            if (topRegion != null && topRegion.found()) {
                Draw.rect(topRegion, x, y);
            }
        }
    }
}
