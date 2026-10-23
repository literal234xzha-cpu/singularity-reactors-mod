package singularity.blocks.gpu;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
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
 * GPU — Procesador GPU de gama media.
 *
 * Equivalente a una RTX 4060 Ti.
 * - 50 instrucciones/tick (vs 2 del micro-procesador base)
 * - Tamaño 2x2
 * - Rango de enlace: 14 tiles
 * - Ideal para lógica de control compleja: PID, gestión de reactores,
 *   monitoreo de toda la red de energía, control de unidades, etc.
 *
 * Un contador de red completo (producción + consumo + alerta):
 *   sensor prod @dspread @powerNetProduced
 *   sensor cons @dspread @powerNetConsumed
 *   op sub balance prod cons
 *   print "Balance: " balance
 *   printflush msg
 */
public class GPU extends LogicBlock {

    public static final int IPT = 50;

    public TextureRegion glowRegion;
    public TextureRegion topRegion;

    public GPU(String name) {
        super(name);
        size = 2;
        instructionsPerTick = IPT;
        range = 8 * 14f; // 14 tiles
        health = 320;
        category = Category.logic;
    }

    @Override
    public void load() {
        super.load();
        glowRegion = Core.atlas.find(name + "-glow");
        topRegion = Core.atlas.find(name + "-top");
    }

    public class GPUBuild extends LogicBuild {

        private float glow = 0f;
        private float glowTimer = 0f;
        private float coreHeat = 0f;

        @Override
        public void updateTile() {
            super.updateTile();
            glowTimer += Time.delta;
            float activity = (executor != null && executor.instructions.length > 0) ? 1f : 0f;
            glow = Mathf.lerp(glow, activity, 0.06f * Time.delta);
            coreHeat = Mathf.lerp(coreHeat, activity * 0.8f + 0.2f, 0.04f * Time.delta);
        }

        @Override
        public void draw() {
            super.draw();

            // Resplandor base de la GPU
            if (glow > 0.01f && glowRegion != null && glowRegion.found()) {
                float pulse = Mathf.sin(glowTimer, 60f, 0.25f) + 0.75f;
                Draw.color(Color.valueOf("22FF88"), glow * pulse * 0.8f);
                Draw.rect(glowRegion, x, y);
                Draw.color();
            }

            // Capa superior (ventilador / disipador)
            if (topRegion != null && topRegion.found()) {
                Draw.rect(topRegion, x, y, glowTimer * 1.5f);
            }
        }
    }
}
