package singularity.blocks.delta;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.Tile;

/**
 * DeltaLoadRegulator -- Regulador de carga automatico.
 * Enciende/apaga turbinas segun la demanda vs produccion de la red.
 */
public class DeltaLoadRegulator extends Block {

    public int regulatorRange = 30;
    public TextureRegion topRegion;

    public DeltaLoadRegulator(String name) {
        super(name);
        size = 3;
        update = true;
        solid = true;
        hasPower = true;
        consumesPower = true;
        configurable = true;
        saveConfig = false;
        category = Category.power;
        health = 600;
        buildType = DeltaLoadRegulatorBuild::new;
    }

    @Override
    public void load() {
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    public class DeltaLoadRegulatorBuild extends Building {

        public float shutdownThreshold = 0.5f;  // apagar turbinas si exceso > demanda * este %
        public float startupThreshold  = 0.3f;  // encender turbinas si deficit > demanda * este %
        public boolean autoMode = true;

        private int scanTimer = 0;
        private java.util.List<DeltaTurbine.DeltaTurbineBuild> turbines = new java.util.ArrayList<>();

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            return this;
        }

        @Override
        public void updateTile() {
            scanTimer++;
            if (scanTimer >= 60) {
                scanTimer = 0;
                scanTurbines();
                if (autoMode) regulate();
            }
        }

        @Override
        public void draw() {
            Draw.rect(region, x, y);
            if (topRegion != null && topRegion.found() && enabled) {
                Draw.rect(topRegion, x, y);
            }
        }

        private void scanTurbines() {
            turbines.clear();
            int tx = tileX(), ty = tileY();
            for (int dx = -regulatorRange; dx <= regulatorRange; dx++) {
                for (int dy = -regulatorRange; dy <= regulatorRange; dy++) {
                    Building b = Vars.world.build(tx + dx, ty + dy);
                    if (b instanceof DeltaTurbine.DeltaTurbineBuild && b.team == team) {
                        DeltaTurbine.DeltaTurbineBuild tb = (DeltaTurbine.DeltaTurbineBuild) b;
                        turbines.add(tb);
                    }
                }
            }
        }

        private void regulate() {
            if (power == null || power.graph == null) return;
            float prod = power.graph.getPowerProduced() * 60f;
            float cons = power.graph.getPowerNeeded() * 60f;
            if (cons < 1f) return;

            float surplus = prod - cons;
            float surplusRatio = surplus / cons;

            // Demasiada produccion -> apagar turbinas
            if (surplusRatio > shutdownThreshold) {
                for (DeltaTurbine.DeltaTurbineBuild tb : turbines) {
                    if (tb.enabled) { tb.enabled = false; break; }
                }
            }
            // Poca produccion -> encender turbinas
            else if (surplusRatio < -startupThreshold) {
                for (DeltaTurbine.DeltaTurbineBuild tb : turbines) {
                    if (!tb.enabled) { tb.enabled = true; break; }
                }
            }
        }

        @Override
        public void buildConfiguration(Table t) {
            t.defaults().pad(3f);
            t.add(gradient("REGULADOR DE CARGA", 0xFF8800, 0xFFCC00)).center().colspan(2).row();
            t.image().color(Color.valueOf("FF9900")).height(3f).growX().colspan(2).pad(2f).row();

            if (power != null && power.graph != null) {
                t.add("[lightgray]Demanda:[]").left();
                t.label(() -> "[red]" + fmt(power.graph.getPowerNeeded()*60f) + "/s[]").right().row();

                t.add("[lightgray]Produccion:[]").left();
                t.label(() -> "[green]" + fmt(power.graph.getPowerProduced()*60f) + "/s[]").right().row();

                t.add("[lightgray]Balance:[]").left();
                t.label(() -> {
                    float bal = (power.graph.getPowerProduced()-power.graph.getPowerNeeded())*60f;
                    return (bal >= 0 ? "[green]+" : "[red]") + fmt(bal) + "/s[]";
                }).right().row();
            }

            t.add("[lightgray]Turbinas:[]").left();
            t.label(() -> "[accent]" + turbines.size() + "[]").right().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.add("[lightgray]Umbral apagado:[]").left();
            t.table(ct -> {
                ct.button("[lightgray]-[]", () -> shutdownThreshold = Mathf.clamp(shutdownThreshold-0.05f, 0.1f, 0.9f)).size(26f,26f);
                ct.label(() -> "[orange]+" + (int)(shutdownThreshold*100f) + "%[]").minWidth(50f).center();
                ct.button("[lightgray]+[]", () -> shutdownThreshold = Mathf.clamp(shutdownThreshold+0.05f, 0.1f, 0.9f)).size(26f,26f);
            }).right().row();

            t.add("[lightgray]Umbral encendido:[]").left();
            t.table(ct -> {
                ct.button("[lightgray]-[]", () -> startupThreshold = Mathf.clamp(startupThreshold-0.05f, 0.1f, 0.9f)).size(26f,26f);
                ct.label(() -> "[cyan]-" + (int)(startupThreshold*100f) + "%[]").minWidth(50f).center();
                ct.button("[lightgray]+[]", () -> startupThreshold = Mathf.clamp(startupThreshold+0.05f, 0.1f, 0.9f)).size(26f,26f);
            }).right().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.table(bt -> {
                bt.button(btn -> {
                    btn.clearChildren();
                    btn.add(new arc.scene.ui.Label(autoMode ? "[green]AUTO[]" : "[gray]MANUAL[]")).center();
                }, () -> autoMode = !autoMode).size(80f,30f).pad(3f);

                bt.button("[green]Todo ON[]", () -> { 
                    for (DeltaTurbine.DeltaTurbineBuild tb : turbines) tb.enabled = true; 
                }).size(75f,30f).pad(3f);
                
                bt.button("[red]Todo OFF[]", () -> { 
                    for (DeltaTurbine.DeltaTurbineBuild tb : turbines) tb.enabled = false; 
                }).size(75f,30f).pad(3f);
            }).colspan(2).center().row();
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

        @Override
        public void write(Writes w) { super.write(w); w.f(shutdownThreshold); w.f(startupThreshold); w.bool(autoMode); }

        @Override
        public void read(Reads r, byte rev) { super.read(r,rev); shutdownThreshold=r.f(); startupThreshold=r.f(); autoMode=r.bool(); }
    }
}
