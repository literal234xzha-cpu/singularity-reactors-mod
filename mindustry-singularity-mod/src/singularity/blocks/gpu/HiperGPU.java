package singularity.blocks.gpu;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.type.Category;
import singularity.blocks.gpu.functions.*;

/**
 * HiperGPU — Procesador GPU de gama ultra (tier 2).
 * 200 ipt, rango 20 tiles, 35 funciones totales.
 */
public class HiperGPU extends GPU {

    public static final int HIPER_IPT = 200;
    public TextureRegion rotatorRegion;

    public HiperGPU(String name) {
        super(name);
        size = 3;
        instructionsPerTick = HIPER_IPT;
        range = 8 * 20f;
        health = 700;
        category = Category.logic;
        buildType = HiperGPUBuild::new;
    }

    @Override
    public void load() {
        super.load();
        rotatorRegion = Core.atlas.find(name + "-rotator");
    }

    @Override
    protected void registerFunctions() {
        super.registerFunctions(); // hereda 1-25
        availableFunctions.add(new AutoScram());
        availableFunctions.add(new SmartGrid());
        availableFunctions.add(new BaseDashboard());
        availableFunctions.add(new PIDController());
        availableFunctions.add(new WaveDefender());
        availableFunctions.add(new ResourceOptimizer());
        availableFunctions.add(new MultiReactorSync());
        availableFunctions.add(new EconomyMonitor());
        availableFunctions.add(new CombatAI());
        availableFunctions.add(new FullBaseAutopilot());
    }

    public class HiperGPUBuild extends GPUBuild {
        private float spinAngle = 0f;
        private float glowTimer = 0f;

        @Override
        public void updateTile() {
            super.updateTile();
            glowTimer += Time.delta;
            spinAngle += 60f * Time.delta;
        }

        @Override
        public void draw() {
            super.draw();
            if (rotatorRegion != null && rotatorRegion.found()) {
                float pulse = Mathf.sin(glowTimer, 45f, 0.2f) + 0.8f;
                Draw.color(Color.valueOf("AA00FF"), pulse * 0.5f);
                Draw.rect(rotatorRegion, x, y, spinAngle);
                Draw.color();
            }
        }
    }
}
