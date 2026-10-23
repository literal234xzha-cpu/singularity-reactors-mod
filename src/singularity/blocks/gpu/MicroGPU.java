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
import singularity.blocks.gpu.functions.EnergyCounter;
import singularity.blocks.gpu.functions.HealthMonitor;
import singularity.blocks.gpu.functions.ItemAlert;
import singularity.blocks.gpu.functions.ItemCounter;
import singularity.blocks.gpu.functions.LiquidLevel;
import singularity.blocks.gpu.functions.LowPowerAlert;
import singularity.blocks.gpu.functions.TemperatureDisplay;
import singularity.blocks.gpu.functions.TimerDisplay;
import singularity.blocks.gpu.functions.ToggleSwitch;
import singularity.blocks.gpu.functions.UnitCounter;

/**
 * MicroGPU — Procesador GPU de gama base con funciones precargadas.
 *
 * DOS MODOS:
 * ─────────────────────────────────────────────────────────────
 * 1. MODO SIMPLE (Funciones precargadas)
 *    Seleccionas una función de la lista, configuras parámetros
 *    con sliders/checkboxes en la UI y funciona automáticamente.
 *    No necesitas escribir código.
 *
 * 2. MODO CÓDIGO (Avanzado)
 *    Igual que un procesador normal pero con 12 ipt (6x más rápido).
 *    Soporta instrucciones especiales gpu.* para mayor simplicidad.
 *
 * Funciones disponibles en MicroGPU (tier 0):
 *   1. Energy Counter  — producción/consumo/balance en display
 *   2. Low Power Alert — alerta cuando energía baja de X%
 *   3. Item Counter    — items en contenedores conectados
 */
public class MicroGPU extends LogicBlock {

    public static final int IPT = 12;

    public TextureRegion glowRegion;

    /** Todas las funciones disponibles para este tier */
    protected final Seq<GPUFunction> availableFunctions = new Seq<>();

    public MicroGPU(String name) {
        super(name);
        size = 1;
        instructionsPerTick = IPT;
        range = 8 * 10f;
        health = 80;
        category = Category.logic;
        buildType = MicroGPUBuild::new;

        // Registrar funciones de tier 0 (1-10)
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

    // =========================================================================
    public class MicroGPUBuild extends LogicBuild {

        /** Modo actual: false = funciones precargadas, true = código manual */
        public boolean codeMode = false;

        /** Función actualmente seleccionada (-1 = ninguna) */
        public int selectedFunction = -1;

        // Visual
        private float glow      = 0f;
        private float glowTimer = 0f;

        // ── Update ────────────────────────────────────────────────────────────
        @Override
        public void updateTile() {
            super.updateTile();
            glowTimer += Time.delta;

            float activity = (!codeMode && selectedFunction >= 0) ? 1f :
                             (executor != null && executor.instructions.length > 0) ? 1f : 0f;
            glow = Mathf.lerp(glow, activity, 0.08f * Time.delta);

            // Ejecutar función precargada
            if (!codeMode && selectedFunction >= 0
                    && selectedFunction < availableFunctions.size) {
                availableFunctions.get(selectedFunction).update(this);
            }
        }

        // ── Draw ──────────────────────────────────────────────────────────────
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

        // ── UI ────────────────────────────────────────────────────────────────
        @Override
        public void buildConfiguration(Table table) {
            table.defaults().pad(4f);

            // ── Header con toggle de modo ──────────────────────────────────
            table.table(header -> {
                header.add("[cyan]GPU MODE[]").padRight(8f);
                header.button(codeMode ? "[cyan]< Funciones[]" : "[yellow]Código >[]",
                    () -> {
                        codeMode = !codeMode;
                        Core.scene.setKeyboardFocus(null);
                        deselect();
                        configure(null);
                    }).size(120f, 32f);
            }).row();

            table.image().color(Color.gray).fillX().height(1f).pad(2f).row();

            if (!codeMode) {
                buildFunctionPanel(table);
            } else {
                buildCodePanel(table);
            }
        }

        /** Panel de funciones precargadas */
        private void buildFunctionPanel(Table table) {
            // Si no hay función seleccionada → mostrar lista
            if (selectedFunction < 0) {
                table.add("[white]Selecciona una función:[]").row();
                table.row();

                for (int i = 0; i < availableFunctions.size; i++) {
                    final int idx = i;
                    GPUFunction fn = availableFunctions.get(i);

                    table.button(
                        fn.icon + " [white]" + fn.name + "[]",
                        () -> {
                            selectedFunction = idx;
                            Core.scene.setKeyboardFocus(null);
                            deselect();
                            configure(null);
                        }
                    ).width(200f).height(36f).row();

                    table.add("[lightgray]" + fn.description + "[]")
                         .padLeft(8f).padBottom(6f).row();
                }
            } else {
                // Función seleccionada → mostrar su config
                GPUFunction fn = availableFunctions.get(selectedFunction);

                // Botón volver
                table.button("← Volver", () -> {
                    selectedFunction = -1;
                    deselect();
                    configure(null);
                }).size(80f, 28f).left().row();

                table.image().color(Color.gray).fillX().height(1f).pad(2f).row();

                // Config de la función
                fn.buildConfig(table, this);

                table.row();
                table.image().color(Color.gray).fillX().height(1f).pad(2f).row();

                // Botón exportar código
                table.button("[yellow]Ver código equivalente[]", () -> {
                    arc.util.Log.info(fn.toCode());
                }).width(200f).height(28f).row();
            }
        }

        /** Panel de modo código (igual que procesador normal) */
        private void buildCodePanel(Table table) {
            table.add("[yellow]Modo Código[]").row();
            table.add("[lightgray]" + IPT + " instrucciones/tick[]").row();
            table.add("[lightgray]Rango: " + (int)(range/8f) + " tiles[]").row();
            table.row();
            // Delegar al panel de código del LogicBlock padre
            super.buildConfiguration(table);
        }
    }
}
