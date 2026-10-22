package singularity.blocks.delta;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.Tile;

/**
 * DeltaBreaker -- Disyuntor automatico.
 * Se dispara (abre el circuito) si el consumo supera un umbral.
 * Reset automatico o manual segun configuracion.
 */
public class DeltaBreaker extends Block {

    public float defaultThreshold = 50000f;

    public DeltaBreaker(String name) {
        super(name);
        size = 1;
        update = true;
        solid = true;
        hasPower = true;
        configurable = true;
        saveConfig = true;
        category = Category.power;
        health = 200;
        buildType = DeltaBreakerBuild::new;
        
        config(Float.class, (DeltaBreakerBuild b, Float val) -> {
            b.threshold = Mathf.clamp(val, 1000f, 500000f);
        });
    }

    public class DeltaBreakerBuild extends Building {

        public float threshold = 50000f;
        public boolean tripped = false;
        public boolean autoReset = true;
        private int checkTimer = 0;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            threshold = defaultThreshold;
            return this;
        }

        @Override
        public void updateTile() {
            checkTimer++;
            if (checkTimer < 30) return;
            checkTimer = 0;

            if (power == null || power.graph == null) return;
            float cons = power.graph.getPowerNeeded() * 60f;

            if (!tripped && cons > threshold) {
                tripped = true;
                // Apagar todos los consumidores en este grafo
                for (Building b : power.graph.consumers) {
                    if (b != this && b.enabled) b.enabled = false;
                }
            }

            if (tripped && autoReset && cons < threshold * 0.7f) {
                tripped = false;
            }
        }

        @Override
        public void buildConfiguration(Table t) {
            t.defaults().pad(3f);

            t.add(gradient("DISYUNTOR DELTA", 0xFF2200, 0xFF6600)).center().colspan(2).row();
            t.image().color(tripped ? Color.red : Color.green).height(3f).growX().colspan(2).pad(2f).row();

            t.label(() -> tripped ? "[red]ABIERTO[]" : "[green]CERRADO[]").colspan(2).center().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.add("[lightgray]Umbral:[]").left();
            t.table(ct -> {
                ct.button("[lightgray]-[]", () -> threshold = Mathf.clamp(threshold - 5000f, 1000f, 500000f)).size(26f,26f);
                ct.label(() -> "[orange]" + fmt(threshold) + "/s[]").minWidth(70f).center();
                ct.button("[lightgray]+[]", () -> threshold = Mathf.clamp(threshold + 5000f, 1000f, 500000f)).size(26f,26f);
            }).right().row();

            if (power != null && power.graph != null) {
                t.add("[lightgray]Consumo actual:[]").left();
                t.label(() -> {
                    float c = power.graph.getPowerNeeded() * 60f;
                    String col = c > threshold ? "red" : c > threshold*0.8f ? "orange" : "green";
                    return "[" + col + "]" + fmt(c) + "/s[]";
                }).right().row();
            }

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.table(bt -> {
                bt.button(btn -> {
                    btn.clearChildren();
                    btn.add(new arc.scene.ui.Label(autoReset ? "[green]Auto[]" : "[gray]Manual[]")).center();
                }, () -> autoReset = !autoReset).size(70f,28f).pad(2f);

                bt.button("[cyan]Reset[]", () -> tripped = false).size(70f,28f).pad(2f);
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
        public Float config() { return threshold; }

        @Override
        public void write(Writes w) { super.write(w); w.f(threshold); w.bool(tripped); w.bool(autoReset); }

        @Override
        public void read(Reads r, byte rev) { super.read(r,rev); threshold=r.f(); tripped=r.bool(); autoReset=r.bool(); }
    }
}
