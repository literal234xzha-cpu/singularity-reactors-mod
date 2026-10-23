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
 * HiperGPU — tier 2. 200 ipt, 35 funciones + rotor RGB.
 */
public class HiperGPU extends LogicBlock {

    public static final int IPT = 200;
    public TextureRegion glowRegion, rotatorRegion, topRegion;
    public final Seq<GPUFunction> availableFunctions = new Seq<>();

    public HiperGPU(String name) {
        super(name);
        size = 3;
        instructionsPerTick = IPT;
        range = 8 * 20f;
        health = 700;
        category = Category.logic;
        buildType = HiperGPUBuild::new;
        registerFunctions();
    }

    protected void registerFunctions() {
        // Tier 0
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
        // Tier 2
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

    @Override
    public void load() {
        super.load();
        glowRegion    = Core.atlas.find(name + "-glow");
        rotatorRegion = Core.atlas.find(name + "-rotator");
        topRegion     = Core.atlas.find(name + "-top");
    }

    public class HiperGPUBuild extends LogicBuild {

        public boolean codeMode         = false;
        public int     selectedFunction  = -1;
        public int     activeTab         = 0;
        public boolean showWiki          = false;
        public final Seq<GPUEvent> activeEvents = new Seq<>();

        private float glow      = 0f;
        private float glowTimer = 0f;
        private float spinAngle = 0f;

        @Override
        public void updateTile() {
            super.updateTile();
            glowTimer += Time.delta;
            spinAngle += 60f * Time.delta;
            float activity = (!codeMode && selectedFunction >= 0) ? 1f :
                             (executor != null && executor.instructions.length > 0) ? 1f : 0f;
            glow = Mathf.lerp(glow, activity, 0.05f * Time.delta);

            if (!codeMode && selectedFunction >= 0 &&
                    selectedFunction < availableFunctions.size) {
                availableFunctions.get(selectedFunction).update(this);
            }

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
                float pulse = Mathf.sin(glowTimer, 45f, 0.2f) + 0.8f;
                Draw.color(Color.valueOf("AA00FF"), glow * pulse * 0.9f);
                Draw.rect(glowRegion, x, y);
                Draw.color(Color.valueOf("00FFFF"), glow * 0.4f);
                Draw.rect(glowRegion, x, y);
                Draw.color();
            }
            if (rotatorRegion != null && rotatorRegion.found()) {
                Draw.rect(rotatorRegion, x, y, spinAngle);
            }
            if (topRegion != null && topRegion.found()) {
                Draw.rect(topRegion, x, y);
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            table.defaults().pad(3f);
            table.table(h -> {
                h.add("[cyan]HiperGPU[]").padRight(4f);
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
            table.add("[cyan]⚡ Vectorial[]").row();
            for (GPUVecInstruction vec : GPUVecInstruction.ALL) {
                table.table(row -> {
                    row.add("[white]" + vec.name + "[]").width(100f).left();
                    row.button("[green]▶[]", () -> vec.execute(this, new String[0])).size(28f, 24f);
                }).row();
            }
        }

        private void buildEventPanel(Table table) {
            table.add("[cyan]📡 Eventos[]").row();
            for (int i = 0; i < activeEvents.size; i++) {
                final int idx = i;
                GPUEvent ev = activeEvents.get(i);
                table.table(row -> {
                    row.check("[white]" + ev.name + "[]", ev.active, v -> ev.active = v).width(140f);
                    row.button("[red]✕[]", () -> { activeEvents.remove(idx); deselect(); configure(null); }).size(24f, 24f);
                }).row();
            }
            table.row();
            for (GPUEvent tmpl : GPUEvent.TEMPLATES) {
                final GPUEvent t = tmpl;
                table.button(t.name, () -> {
                    activeEvents.add(new GPUEvent(t.type, t.name, t.description, t.example, t.tier));
                    deselect(); configure(null);
                }).width(180f).height(26f).row();
            }
        }
    }
}
