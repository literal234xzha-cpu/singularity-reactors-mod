package singularity.blocks.delta;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Liquids;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;

/**
 * DeltaCondenser -- Cierra el ciclo termico vapor-agua.
 *
 * Recibe el calor residual del sistema y lo convierte en agua fria
 * que vuelve al reactor como refrigerante.
 * Eficiencia del condensador afecta la eficiencia total del ciclo.
 */
public class DeltaCondenser extends Block {

    public float condensRate   = 3f;    // agua producida por tick
    public float coolEff       = 0.92f; // eficiencia del ciclo
    public float waterTemp     = 45f;   // temperatura del agua de salida (Celsius)
    public TextureRegion liquidRegion;

    public DeltaCondenser(String name) {
        super(name);
        size = 3;
        update = true;
        solid = true;
        hasLiquids = true;
        liquidCapacity = 200f;
        configurable = true;
        saveConfig = false;
        category = Category.liquid;
        health = 2000;
        buildType = DeltaCondenserBuild::new;
    }

    @Override
    public void load() {
        super.load();
        liquidRegion = Core.atlas.find(name + "-liquid");
    }

    // =========================================================================
    // BUILD
    // =========================================================================

    public class DeltaCondenserBuild extends Building {

        public float steamIn    = 0f;   // vapor entrante (u/s)
        public float waterOut   = 0f;   // agua producida (u/s)
        public float cycleEff   = 0f;   // eficiencia del ciclo
        public float outTemp    = 45f;  // temperatura agua salida

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            return this;
        }

        @Override
        public void updateTile() {
            // Simular recepcion de vapor del sistema
            // En la practica, el condensador procesa agua caliente y la devuelve fria
            float available = liquids != null ? liquids.get(Liquids.water) : 0f;

            if (available > 0.1f) {
                float processed = Math.min(available, condensRate * Time.delta);
                if (liquids != null) liquids.remove(Liquids.water, processed * 0.1f);
                steamIn = processed * 60f;
                waterOut = processed * coolEff * 60f;
                cycleEff = coolEff;
            } else {
                steamIn = 0f;
                waterOut = 0f;
                cycleEff = 0f;
            }
        }

        @Override
        public void draw() {
            Draw.rect(region, x, y);
            if (liquids != null && liquids.currentAmount() > 0.01f && liquidRegion != null && liquidRegion.found()) {
                Draw.color(liquids.current().color);
                Draw.alpha(Mathf.clamp(liquids.currentAmount() / liquidCapacity));
                Draw.rect(liquidRegion, x, y);
                Draw.color();
            }
        }

        // =========================================================================
        // PANEL UNICO - Ciclo
        // =========================================================================

        @Override
        public void buildConfiguration(Table t) {
            t.defaults().pad(3f);

            t.add(gradient("CONDENSADOR DELTA", 0x00AACC, 0x00FFCC)).center().colspan(2).row();
            t.image().color(Color.valueOf("00AACC")).height(3f).growX().colspan(2).pad(2f).row();

            t.add("[lightgray]Vapor entrada:[]").left();
            t.label(() -> steamIn > 0.01f
                ? "[cyan]" + Strings.fixed(steamIn, 1) + " u/s[]"
                : "[gray]Sin vapor[]").right().row();

            t.add("[lightgray]Agua salida:[]").left();
            t.label(() -> waterOut > 0.01f
                ? "[blue]" + Strings.fixed(waterOut, 1) + " u/s[]"
                : "[gray]0 u/s[]").right().row();

            t.add("[lightgray]Eficiencia ciclo:[]").left();
            t.label(() -> {
                int pct = (int)(cycleEff * 100f);
                String col = pct > 85 ? "green" : pct > 60 ? "yellow" : "red";
                return "[" + col + "]" + pct + "%[]";
            }).right().row();

            t.add("[lightgray]Temp agua salida:[]").left();
            t.label(() -> "[cyan]" + (int)waterTemp + "C[]").right().row();

            t.add("[lightgray]Nivel agua:[]").left();
            t.label(() -> {
                float amt = liquids != null ? liquids.get(Liquids.water) : 0f;
                int pct = (int)(amt / liquidCapacity * 100f);
                String col = pct > 50 ? "blue" : pct > 20 ? "yellow" : "red";
                return "[" + col + "]" + pct + "%[]";
            }).right().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.label(() -> cycleEff > 0.01f
                ? "[green]Ciclo activo - " + (int)(cycleEff*100f) + "% eficiencia[]"
                : "[gray]Sin flujo - ciclo detenido[]"
            ).colspan(2).center().row();
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

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return liquid == Liquids.water;
        }

        @Override
        public void write(Writes w) { super.write(w); w.f(cycleEff); }

        @Override
        public void read(Reads r, byte rev) { super.read(r,rev); cycleEff=r.f(); }
    }
}
