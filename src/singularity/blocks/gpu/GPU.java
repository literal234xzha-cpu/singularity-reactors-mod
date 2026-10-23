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
import singularity.blocks.gpu.functions.*;

/**
 * GPU — Procesador GPU de gama media (tier 1).
 *
 * Hereda las 10 funciones del MicroGPU y agrega:
 * - 15 funciones adicionales (11-25)
 * - Instrucciones vectoriales (gpuvec)
 * - Sistema de eventos (onevent)
 * - Wiki in-game
 * - 50 instrucciones/tick
 * - Rango: 14 tiles
 */
public class GPU extends MicroGPU {

    public static final int GPU_IPT = 50;

    public TextureRegion topRegion;

    public GPU(String name) {
        super(name);
        size = 2;
        instructionsPerTick = GPU_IPT;
        range = 8 * 14f;
        health = 320;
        category = Category.logic;
        buildType = GPUBuild::new;
    }

    @Override
    public void load() {
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    protected void registerFunctions() {
        // Heredar funciones 1-10 de MicroGPU
        super.registerFunctions();

        // Funciones 11-25 exclusivas de GPU
        availableFunctions.add(new PowerBalance());
        availableFunctions.add(new ReactorMonitor());
        availableFunctions.add(new AutoGate());
        availableFunctions.add(new MultiItemCounter());
        availableFunctions.add(new LiquidFlowMonitor());
        availableFunctions.add(new EnemyRadar());
        availableFunctions.add(new PowerScheduler());
        availableFunctions.add(new DamageAlert());
        availableFunctions.add(new ReactorEfficiency());
        availableFunctions.add(new MultiDisplay());
        availableFunctions.add(new NetworkStats());
        availableFunctions.add(new AutoRepair());
        availableFunctions.add(new SyncClock());
        availableFunctions.add(new UnitCommander());
        availableFunctions.add(new OverflowManager());
    }

    public class GPUBuild extends MicroGPUBuild {

        // Sistema de eventos
        public final Seq<GPUEvent>          activeEvents = new Seq<>();
        // Sistema vectorial
        public final Seq<GPUVecInstruction> vecQueue     = new Seq<>();

        private float glow      = 0f;
        private float glowTimer = 0f;

        @Override
        public void updateTile() {
            super.updateTile();
            glowTimer += Time.delta;

            // Evaluar eventos
            for (int i = 0; i < activeEvents.size; i++) {
                GPUEvent event = activeEvents.get(i);
                if (event.evaluate(this)) {
                    // Ejecutar acciones vectoriales asignadas al evento
                    for (String actionId : event.actions) {
                        GPUVecInstruction vec = findVec(actionId);
                        if (vec != null) vec.execute(this, new String[0]);
                    }
                }
            }
        }

        @Override
        public void draw() {
            super.draw();
            // Ventilador giratorio
            if (topRegion != null && topRegion.found()) {
                float activity = (!codeMode && selectedFunction >= 0) ? 1f : 0.3f;
                Draw.rect(topRegion, x, y, glowTimer * (1f + activity));
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            table.defaults().pad(3f);

            // Header con modo + wiki
            table.table(h -> {
                h.add("[cyan]GPU[]").padRight(6f);
                h.button(codeMode ? "[cyan]< Funciones[]" : "[yellow]Código >[]", () -> {
                    codeMode = !codeMode;
                    Core.scene.setKeyboardFocus(null);
                    deselect(); configure(null);
                }).size(100f, 28f).padRight(4f);
                h.button("[white]📖[]", () -> {
                    showWiki = !showWiki;
                    deselect(); configure(null);
                }).size(32f, 28f);
            }).row();

            table.image().color(Color.gray).fillX().height(1f).pad(2f).row();

            if (showWiki) {
                GPUWiki.buildWikiPanel(table);
                return;
            }

            if (!codeMode) {
                // Tabs: Funciones | Vectorial | Eventos
                table.table(tabs -> {
                    tabs.button("Funciones", () -> { activeTab = 0; deselect(); configure(null); })
                        .size(70f, 26f).checked(b -> activeTab == 0);
                    tabs.button("Vec",       () -> { activeTab = 1; deselect(); configure(null); })
                        .size(50f, 26f).checked(b -> activeTab == 1);
                    tabs.button("Eventos",   () -> { activeTab = 2; deselect(); configure(null); })
                        .size(60f, 26f).checked(b -> activeTab == 2);
                }).row();

                table.image().color(Color.darkGray).fillX().height(1f).pad(2f).row();

                switch (activeTab) {
                    case 0: buildFunctionPanel(table); break;
                    case 1: buildVecPanel(table);      break;
                    case 2: buildEventPanel(table);    break;
                }
            } else {
                showCodePanel(table);
            }
        }

        private void buildVecPanel(Table table) {
            table.add("[cyan]⚡ Instrucciones Vectoriales[]").row();
            table.add("[lightgray]Operan sobre todos\nlos bloques a la vez[]").row();
            table.row();

            GPUVecInstruction.VecCategory[] cats = GPUVecInstruction.VecCategory.values();
            for (GPUVecInstruction.VecCategory cat : cats) {
                table.add("[yellow]" + cat.name() + "[]").row();
                for (GPUVecInstruction vec : GPUVecInstruction.ALL) {
                    if (vec.category != cat || vec.tier > 1) continue;
                    table.table(row -> {
                        row.add("[white]" + vec.name + "[]").width(100f).left().padRight(4f);
                        row.button("[green]▶[]", () -> vec.execute(this, new String[0]))
                           .size(28f, 24f);
                    }).row();
                    table.add("[lightgray]" + vec.description + "[]")
                         .padLeft(8f).padBottom(4f).row();
                }
            }
        }

        private void buildEventPanel(Table table) {
            table.add("[cyan]📡 Eventos[]").row();
            table.add("[lightgray]Se disparan automáticamente\ncuando algo cambia[]").row();
            table.row();

            // Eventos activos
            if (activeEvents.isEmpty()) {
                table.add("[gray]Sin eventos activos[]").row();
            } else {
                for (int i = 0; i < activeEvents.size; i++) {
                    final int idx = i;
                    GPUEvent ev = activeEvents.get(i);
                    table.table(row -> {
                        row.check("[white]" + ev.name + "[]", ev.active, v -> ev.active = v)
                           .width(130f);
                        row.button("[red]✕[]", () -> {
                            activeEvents.remove(idx);
                            deselect(); configure(null);
                        }).size(24f, 24f);
                    }).row();
                    table.add("[lightgray]Umbral: " + (int)ev.threshold + "  Acciones: " + ev.actions.size + "[]")
                         .padLeft(8f).padBottom(4f).row();
                }
            }
            table.row();

            // Agregar evento
            table.add("[white]+ Agregar evento:[]").row();
            for (GPUEvent template : GPUEvent.TEMPLATES) {
                if (template.tier > 1) continue;
                final GPUEvent tmpl = template;
                table.button(tmpl.name, () -> {
                    GPUEvent ev = new GPUEvent(tmpl.type, tmpl.name,
                        tmpl.description, tmpl.example, tmpl.tier);
                    activeEvents.add(ev);
                    deselect(); configure(null);
                }).width(180f).height(28f).row();
            }
        }

        // ── Estado UI ────────────────────────────────────────────────────────
        private int     activeTab = 0;
        private boolean showWiki  = false;

        private GPUVecInstruction findVec(String id) {
            for (GPUVecInstruction v : GPUVecInstruction.ALL)
                if (v.id.equals(id)) return v;
            return null;
        }

        private void showCodePanel(Table table) {
            table.add("[yellow]Modo Código - GPU[]").row();
            table.add("[lightgray]" + GPU_IPT + " instrucciones/tick[]").row();
            table.add("[lightgray]Rango: " + (int)(range/8f) + " tiles[]").row();
            table.row();
            super.buildConfiguration(table);
        }
    }
}
