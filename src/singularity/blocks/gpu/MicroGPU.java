package singularity.blocks.gpu;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.type.Category;
import mindustry.world.blocks.logic.LogicBlock;
import singularity.blocks.gpu.functions.*;

/**
 * MicroGPU — tier 0. 12 ipt, 10 funciones precargadas.
 */
public class MicroGPU extends LogicBlock {

    public static final int IPT = 12;
    public TextureRegion glowRegion;
    public final Seq<GPUFunction> availableFunctions = new Seq<>();

    public MicroGPU(String name) {
        super(name);
        size = 1;
        instructionsPerTick = IPT;
        range = 8 * 10f;
        health = 80;
        category = Category.logic;
        buildType = MicroGPUBuild::new;
        registerFunctions();
    }

    protected void registerFunctions() {
        availableFunctions.add(new EnergyCounter());
        availableFunctions.add(new LowPowerAlert());
        availableFunctions.add(new ItemCounter());
        availableFunctions.add(new LiquidLevel());
        availableFunctions.add(new UnitCounter());
        availableFunctions.add(new HealthMonitor());
        availableFunctions.add(new TimerDisplay());
        availableFunctions.add(new ToggleSwitch());
        availableFunctions.add(new ItemAlert());
        availableFunctions.add(new TemperatureDisplay());
    }

    @Override
    public void load() {
        super.load();
        glowRegion = Core.atlas.find(name + "-glow");
    }

    public class MicroGPUBuild extends LogicBuild {

        public boolean codeMode       = false;
        public int     selectedFunction = -1;
        private float  glow           = 0f;
        private float  glowTimer      = 0f;

        @Override
        public void updateTile() {
            super.updateTile();
            glowTimer += Time.delta;
            float activity = (!codeMode && selectedFunction >= 0) ? 1f :
                             (executor != null && executor.instructions.length > 0) ? 1f : 0f;
            glow = Mathf.lerp(glow, activity, 0.08f * Time.delta);

            if (!codeMode && selectedFunction >= 0 &&
                    selectedFunction < availableFunctions.size) {
                availableFunctions.get(selectedFunction).update(this);
            }
        }

        @Override
        public void draw() {
            super.draw();
            if (glow > 0.01f && glowRegion != null && glowRegion.found()) {
                float pulse = Mathf.sin(glowTimer, 40f, 0.3f) + 0.7f;
                Draw.color(Color.valueOf("00C8FF"), glow * pulse);
                Draw.rect(glowRegion, x, y);
                Draw.color();
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            table.defaults().pad(3f);

            // Toggle modo
            table.table(h -> {
                h.add("[cyan]MicroGPU[]").padRight(6f);
                h.button(codeMode ? "[cyan]< Funciones[]" : "[yellow]Código >[]",
                    () -> { codeMode = !codeMode; deselect(); configure(null); }
                ).size(110f, 28f);
            }).row();

            table.image().color(Color.gray).fillX().height(1f).pad(2f).row();

            if (codeMode) {
                table.add("[yellow]Modo Código[]").row();
                table.add("[lightgray]" + IPT + " instrucciones/tick[]").row();
                table.row();
                super.buildConfiguration(table);
                return;
            }

            // Lista de funciones
            if (selectedFunction < 0) {
                table.add("[white]Selecciona una función:[]").row();
                for (int i = 0; i < availableFunctions.size; i++) {
                    final int idx = i;
                    GPUFunction fn = availableFunctions.get(i);
                    table.button(fn.icon + " [white]" + fn.name + "[]",
                        () -> { selectedFunction = idx; deselect(); configure(null); }
                    ).width(200f).height(34f).row();
                }
            } else {
                GPUFunction fn = availableFunctions.get(selectedFunction);
                table.button("← Volver", () -> {
                    selectedFunction = -1; deselect(); configure(null);
                }).size(80f, 26f).left().row();
                table.image().color(Color.darkGray).fillX().height(1f).pad(2f).row();
                fn.buildConfig(table, this);
            }
        }
    }
}
