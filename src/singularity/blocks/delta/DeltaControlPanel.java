package singularity.blocks.delta;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.power.PowerGraph;

/**
 * DeltaControlPanel -- Dashboard central del Sistema Delta.
 *
 * Tabs: REACTORES | RED | ALARMAS
 * - Reactores: temperatura y estado de cada DeltaReactor en rango
 * - Red: produccion, consumo, frecuencia simulada, baterias
 * - Alarmas: lista de alertas activas
 */
public class DeltaControlPanel extends Block {

    public int panelRange = 50;
    public TextureRegion topRegion;

    public DeltaControlPanel(String name) {
        super(name);
        size = 4;
        update = true;
        solid = true;
        hasPower = true;
        consumesPower = true;
        configurable = true;
        saveConfig = false;
        category = Category.power;
        health = 800;
        buildType = DeltaControlPanelBuild::new;
    }

    @Override
    public void load() {
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    public class DeltaControlPanelBuild extends Building {

        private int activeTab = 0; // 0=reactores, 1=red, 2=alarmas
        private final Seq<DeltaReactor.DeltaReactorBuild> reactors = new Seq<>();
        private final Seq<DeltaTurbine.DeltaTurbineBuild> turbines = new Seq<>();
        private int scanTimer = 0;
        private Table configTable;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            scan();
            return this;
        }

        @Override
        public void updateTile() {
            scanTimer++;
            if (scanTimer >= 60) { scanTimer = 0; scan(); }
        }

        @Override
        public void draw() {
            Draw.rect(region, x, y);
            if (topRegion != null && topRegion.found() && enabled) {
                Draw.rect(topRegion, x, y);
            }
        }

        private void scan() {
            reactors.clear(); turbines.clear();
            int tx = tileX(), ty = tileY();
            for (int dx = -panelRange; dx <= panelRange; dx++) {
                for (int dy = -panelRange; dy <= panelRange; dy++) {
                    Building b = Vars.world.build(tx + dx, ty + dy);
                    if (b == null || b.team != team) continue;
                    if (b instanceof DeltaReactor.DeltaReactorBuild) {
                        DeltaReactor.DeltaReactorBuild r = (DeltaReactor.DeltaReactorBuild) b;
                        if (!reactors.contains(r)) reactors.add(r);
                    }
                    if (b instanceof DeltaTurbine.DeltaTurbineBuild) {
                        DeltaTurbine.DeltaTurbineBuild tb = (DeltaTurbine.DeltaTurbineBuild) b;
                        if (!turbines.contains(tb)) turbines.add(tb);
                    }
                }
            }
        }

        private float totalProd() {
            float t = 0f;
            for (DeltaTurbine.DeltaTurbineBuild tb : turbines) t += tb.getPowerProduction() * 60f;
            return t;
        }

        private float simFrequency() {
            // Simular frecuencia de red (60Hz = perfecto balance)
            if (power == null || power.graph == null) return 60f;
            float sat = power.graph.getSatisfaction();
            return 58f + sat * 4f; // rango 58-62 Hz
        }

        private Seq<String> getAlarms() {
            Seq<String> alarms = new Seq<>();
            for (DeltaReactor.DeltaReactorBuild r : reactors) {
                float critTemp = (r.block instanceof DeltaReactor) ? ((DeltaReactor)r.block).criticalTemp : 1000f;
                float safeTemp = (r.block instanceof DeltaReactor) ? ((DeltaReactor)r.block).maxSafeTemp : 800f;
                
                if (r.temperature > critTemp)
                    alarms.add("[red]CRITICO: Reactor sobrecalentado[]");
                else if (r.temperature > safeTemp)
                    alarms.add("[orange]WARN: Sobretemperatura en reactor[]");
                if (r.internalLife < r.maxHealth * 0.3f)
                    alarms.add("[orange]WARN: Vida util baja en reactor[]");
                if (r.scrammed)
                    alarms.add("[yellow]INFO: Reactor en SCRAM[]");
            }
            if (power != null && power.graph != null && power.graph.getSatisfaction() < 0.5f)
                alarms.add("[red]CRITICO: Deficit de energia en la red[]");
            if (alarms.isEmpty()) alarms.add("[green]Sin alarmas activas[]");
            return alarms;
        }

        private void rebuildUI() {
            if (configTable == null) return;
            configTable.clearChildren();
            buildUI(configTable);
        }

        @Override
        public void buildConfiguration(Table table) {
            configTable = table;
            table.clearChildren();
            buildUI(table);
        }

        private void buildUI(Table t) {
            t.defaults().pad(3f);

            t.add(gradient("PANEL DE CONTROL DELTA", 0x4400FF, 0x00AAFF)).center().colspan(3).row();
            t.image().color(Color.valueOf("4400FF")).height(3f).growX().colspan(3).pad(2f).row();

            // Tabs
            t.table(tabs -> {
                tabs.button("[accent]REACTORES[]", () -> { activeTab = 0; rebuildUI(); }).size(95f, 28f).pad(1f);
                tabs.button("[cyan]RED[]",         () -> { activeTab = 1; rebuildUI(); }).size(65f, 28f).pad(1f);
                tabs.button("[red]ALARMAS[]",      () -> { activeTab = 2; rebuildUI(); }).size(80f, 28f).pad(1f);
                tabs.button("[lightgray]R[]",      () -> { scan(); rebuildUI(); }).size(32f, 28f).pad(1f);
            }).colspan(3).center().row();

            t.image().color(Color.darkGray).height(1f).growX().colspan(3).pad(1f).row();

            if (activeTab == 0) buildReactorTab(t);
            else if (activeTab == 1) buildNetworkTab(t);
            else buildAlarmsTab(t);
        }

        private void buildReactorTab(Table t) {
            t.add("[lightgray]Reactores detectados: [accent]" + reactors.size + "[]").colspan(3).left().row();
            t.image().color(Color.darkGray).height(1f).growX().colspan(3).pad(1f).row();

            if (reactors.isEmpty()) {
                t.add("[gray]Sin reactores en rango[]").colspan(3).center().pad(8f).row();
                return;
            }

            t.add("[lightgray]Reactor[]").left().minWidth(80f);
            t.add("[lightgray]Temp[]").center().minWidth(70f);
            t.add("[lightgray]Estado[]").center().minWidth(80f).row();

            int max = Math.min(reactors.size, 8);
            for (int i = 0; i < max; i++) {
                DeltaReactor.DeltaReactorBuild r = reactors.get(i);
                float temp = r.temperature;
                String tcol = temp > 1000f ? "red" : temp > 800f ? "orange" : temp > 600f ? "yellow" : "green";
                String state = r.scrammed ? "[red]SCRAM[]" : temp > 1000f ? "[red]CRITICO[]"
                    : temp > 800f ? "[orange]WARN[]" : temp > 200f ? "[green]OK[]" : "[gray]FRIO[]";

                t.add("[white]R" + (i+1) + "[]").left().minWidth(80f);
                t.add("[" + tcol + "]" + (int)temp + "C[]").center().minWidth(70f);
                t.add(state).center().minWidth(80f).row();
            }
        }

        private void buildNetworkTab(Table t) {
            float prod = totalProd();
            float freq = simFrequency();

            t.add("[lightgray]Turbinas activas: [accent]" + turbines.size + "[]").colspan(2).left().row();
            t.image().color(Color.darkGray).height(1f).growX().colspan(2).pad(1f).row();

            t.add("[lightgray]Produccion Delta:[]").left();
            t.label(() -> "[green]" + fmt(totalProd()) + "/s[]").right().row();

            if (power != null && power.graph != null) {
                PowerGraph g = power.graph;
                t.add("[lightgray]Consumo red:[]").left();
                t.label(() -> "[red]" + fmt(g.getPowerNeeded()*60f) + "/s[]").right().row();

                t.add("[lightgray]Balance:[]").left();
                t.label(() -> {
                    float bal = (g.getPowerProduced()-g.getPowerNeeded())*60f;
                    return (bal >= 0 ? "[green]+" : "[red]") + fmt(bal) + "/s[]";
                }).right().row();

                t.add("[lightgray]Satisfaccion:[]").left();
                t.label(() -> {
                    int pct = (int)(g.getSatisfaction()*100f);
                    String col = pct > 95 ? "green" : pct > 60 ? "yellow" : "red";
                    return "[" + col + "]" + pct + "%[]";
                }).right().row();
            }

            t.add("[lightgray]Frecuencia sim.:[]").left();
            t.label(() -> {
                float f = simFrequency();
                String col = f >= 59.5f && f <= 60.5f ? "green" : f >= 58.5f ? "yellow" : "red";
                return "[" + col + "]" + Strings.fixed(f, 1) + " Hz[]";
            }).right().row();
        }

        private void buildAlarmsTab(Table t) {
            Seq<String> alarms = getAlarms();
            t.add("[lightgray]Alarmas activas: [accent]" + (alarms.size == 1 && alarms.get(0).contains("Sin") ? 0 : alarms.size) + "[]")
                .colspan(2).left().row();
            t.image().color(Color.darkGray).height(1f).growX().colspan(2).pad(1f).row();

            for (String alarm : alarms) {
                t.add(alarm).colspan(2).left().row();
            }
        }

        private String gradient(String text, int c1, int c2) {
            int r1=(c1>>16)&0xFF,g1=(c1>>8)&0xFF,b1=c1&0xFF;
            int r2=(c2>>16)&0xFF,g2=(c2>>8)&0xFF,b2=c2&0xFF;
            char[] chars=text.toCharArray(); int n=Math.max(chars.length-1,1),ci=0;
            StringBuilder sb=new StringBuilder();
            for (char c : chars) {
                if (c==' ') { sb.append(' '); continue; }
                float tv=ci/(float)n; ci++;
                sb.append(String.format("[#%02X%02X%02X]%c",
                    (int)(r1+(r2-r1)*tv),(int)(g1+(g2-g1)*tv),(int)(b1+(b2-b1)*tv),c));
            }
            return sb+"[]";
        }

        private String fmt(float v) {
            if (v>=1000000f) return Strings.fixed(v/1000000f,2)+"M";
            if (v>=1000f)    return Strings.fixed(v/1000f,1)+"k";
            return Strings.fixed(v,0);
        }

        @Override
        public boolean shouldConsume() { return true; }
    }
}
