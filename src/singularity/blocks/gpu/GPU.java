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
 * GPU — tier 1. 50 ipt, 25 funciones + vectorial + eventos + wiki.
 */
public class GPU extends LogicBlock {

    public static final int IPT = 50;
    public TextureRegion glowRegion, topRegion;
    public final Seq<GPUFunction> availableFunctions = new Seq<>();

    public GPU(String name) {
        super(name);
        size = 2;
        instructionsPerTick = IPT;
        range = 8 * 14f;
        health = 320;
        category = Category.logic;
        buildType = GPUBuild::new;
        registerFunctions();
    }

    protected void registerFunctions() {
        // Tier 0 (heredadas)
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
        // Tier 1
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

    @Override
    public void load() {
        super.load();
        glowRegion = Core.atlas.find(name + "-glow");
        topRegion  = Core.atlas.find(name + "-top");
    }

    public class GPUBuild extends LogicBuild {

        public boolean codeMode        = false;
        public int     selectedFunction = -1;
        public int     activeTab        = 0;
        public boolean showWiki         = false;
        public final Seq<GPUEvent> activeEvents = new Seq<>();

        private float glow      = 0f;
        private float glowTimer = 0f;

        @Override
        public void updateTile() {
            super.updateTile();
            glowTimer += Time.delta;
            float activity = (!codeMode && selectedFunction >= 0) ? 1f :
                             (executor != null && executor.instructions.length > 0) ? 1f : 0f;
            glow = Mathf.lerp(glow, activity, 0.06f * Time.delta);

            // Funciones precargadas
            if (!codeMode && selectedFunction >= 0 &&
                    selectedFunction < availableFunctions.size) {
                availableFunctions.get(selectedFunction).update(this);
            }

            // Eventos
            for (int i = 0; i < activeEvents.size; i++) {
                GPUEvent ev = activeEvents.get(i);
                if (ev.evaluate(this)) {
                    for (String actionId : ev.actions) {
                        for (GPUVecInstruction vec : GPUVecInstruction.ALL) {
                            if (vec.id.equals(actionId)) {
                                vec.execute(this, new String[0]);
                                break;
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void draw() {
            super.draw();
            if (glow > 0.01f && glowRegion != null && glowRegion.found()) {
                float pulse = Mathf.sin(glowTimer, 60f, 0.25f) + 0.75f;
                Draw.color(Color.valueOf("22FF88"), glow * pulse * 0.8f);
                Draw.rect(glowRegion, x, y);
                Draw.color();
            }
            if (topRegion != null && topRegion.found()) {
                Draw.rect(topRegion, x, y, glowTimer * 1.5f);
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            table.defaults().pad(3f);

            // Header
            table.table(h -> {
                h.add("[cyan]GPU[]").padRight(4f);
                h.button(codeMode ? "[cyan]< Func[]" : "[yellow]Código[]",
                    () -> { codeMode = !codeMode; deselect(); configure(null); }
                ).size(80f, 26f).padRight(4f);
                h.button("[white]📖[]",
                    () -> { showWiki = !showWiki; deselect(); configure(null); }
                ).size(30f, 26f);
            }).row();
            table.image().color(Color.gray).fillX().height(1f).pad(2f).row();

            if (showWiki) { GPUWiki.buildWikiPanel(table); return; }

            if (codeMode) {
                table.add("[yellow]Modo Código - " + IPT + " ipt[]").row();
                table.row();
                super.buildConfiguration(table);
                return;
            }

            // Tabs
            table.table(tabs -> {
                tabs.button("Func",    () -> { activeTab = 0; deselect(); configure(null); }).size(55f,24f).checked(b -> activeTab==0);
                tabs.button("Vec",     () -> { activeTab = 1; deselect(); configure(null); }).size(45f,24f).checked(b -> activeTab==1);
                tabs.button("Eventos", () -> { activeTab = 2; deselect(); configure(null); }).size(60f,24f).checked(b -> activeTab==2);
            }).row();
            table.image().color(Color.darkGray).fillX().height(1f).pad(2f).row();

            if (activeTab == 0) buildFunctionPanel(table);
            if (activeTab == 1) buildVecPanel(table);
            if (activeTab == 2) buildEventPanel(table);
        }

        private void buildFunctionPanel(Table table) {
            if (selectedFunction < 0) {
                table.add("[white]Selecciona función:[]").row();
                for (int i = 0; i < availableFunctions.size; i++) {
                    final int idx = i;
                    GPUFunction fn = availableFunctions.get(i);
                    table.button(fn.icon + " [white]" + fn.name + "[]",
                        () -> { selectedFunction = idx; deselect(); configure(null); }
                    ).width(200f).height(32f).row();
                }
            } else {
                GPUFunction fn = availableFunctions.get(selectedFunction);
                table.button("← Volver", () -> { selectedFunction = -1; deselect(); configure(null); })
                     .size(80f, 26f).left().row();
                table.image().color(Color.darkGray).fillX().height(1f).pad(2f).row();
                fn.buildConfig(table, this);
            }
        }

        private void buildVecPanel(Table table) {
            table.add("[cyan]⚡ Instrucciones Vectoriales[]").row();
            table.add("[lightgray]Opera sobre todos los bloques a la vez[]").row();
            table.row();
            for (GPUVecInstruction vec : GPUVecInstruction.ALL) {
                if (vec.tier > 1) continue;
                table.table(row -> {
                    row.add("[white]" + vec.name + "[]").width(100f).left();
                    row.button("[green]▶[]", () -> vec.execute(this, new String[0])).size(28f, 24f);
                }).row();
                table.add("[lightgray]" + vec.description + "[]").padLeft(8f).padBottom(4f).row();
            }
        }

        private void buildEventPanel(Table table) {
            table.add("[cyan]📡 Eventos[]").row();
            table.add("[lightgray]Se disparan cuando algo cambia[]").row();
            table.row();
            if (activeEvents.isEmpty()) {
                table.add("[gray]Sin eventos. Añade uno abajo.[]").row();
            }
            for (int i = 0; i < activeEvents.size; i++) {
                final int idx = i;
                GPUEvent ev = activeEvents.get(i);
                table.table(row -> {
                    row.check("[white]" + ev.name + "[]", ev.active, v -> ev.active = v).width(140f);
                    row.button("[red]✕[]", () -> { activeEvents.remove(idx); deselect(); configure(null); }).size(24f, 24f);
                }).row();
            }
            table.row();
            table.add("[white]+ Añadir evento:[]").row();
            for (GPUEvent tmpl : GPUEvent.TEMPLATES) {
                if (tmpl.tier > 1) continue;
                final GPUEvent t = tmpl;
                table.button(t.name, () -> {
                    GPUEvent ev = new GPUEvent(t.type, t.name, t.description, t.example, t.tier);
                    activeEvents.add(ev);
                    deselect(); configure(null);
                }).width(180f).height(26f).row();
            }
        }
    }
}
