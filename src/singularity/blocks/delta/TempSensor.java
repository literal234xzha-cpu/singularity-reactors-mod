package singularity.blocks.delta;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BlockGroup;

/**
 * TempSensor -- Sensor de temperatura para reactores Delta.
 * Se linkea a un reactor especifico, muestra temperatura y dispara alarma si supera umbral.
 */
public class TempSensor extends Block {

    public TextureRegion topRegion;

    public TempSensor(String name) {
        super(name);
        size = 1;
        update = true;
        solid = true;
        configurable = true;
        saveConfig = true;
        category = Category.power;
        health = 100;
        buildType = TempSensorBuild::new;
        group = BlockGroup.logic;

        config(Integer.class, (TempSensorBuild b, Integer pos) -> {
            Building other = Vars.world.build(pos);
            boolean removeOld = (b.reactor == other);
            if (removeOld) b.reactor = null;
            else if (other instanceof DeltaReactor.DeltaReactorBuild && other.team == b.team) {
                b.reactor = (DeltaReactor.DeltaReactorBuild) other;
            }
        });
    }

    @Override
    public void load() {
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    public class TempSensorBuild extends Building {

        public DeltaReactor.DeltaReactorBuild reactor;
        public float alertThreshold = 800f;
        public boolean alertActive = false;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            return this;
        }

        @Override
        public void updateTile() {
            if (reactor == null || !reactor.isValid()) {
                reactor = null;
                alertActive = false;
                return;
            }

            alertActive = reactor.temperature >= alertThreshold;
        }

        @Override
        public void draw() {
            Draw.rect(region, x, y);
            if (topRegion != null && topRegion.found()) {
                float curTemp = reactor != null ? reactor.temperature : 20f;
                float rot = Mathf.clamp((curTemp - 20f) / 1000f) * 180f;
                Draw.rect(topRegion, x, y, -rot);
            }
        }

        @Override
        public void buildConfiguration(Table t) {
            t.defaults().pad(3f);

            t.add(gradient("SENSOR TEMPERATURA", 0x00AAFF, 0xFF4400)).center().colspan(2).row();
            t.image().color(alertActive ? Color.red : Color.green).height(3f).growX().colspan(2).pad(2f).row();

            if (reactor == null || !reactor.isValid()) {
                t.add("[gray]No linkeado[]").colspan(2).center().row();
                t.add("[lightgray]Click en reactor para linkear[]").colspan(2).center().row();
            } else {
                t.add("[lightgray]Reactor:[]").left();
                t.add("[accent]" + reactor.block.localizedName + "[]").right().row();

                t.add("[lightgray]Temperatura:[]").left();
                t.label(() -> {
                    float temp = reactor.temperature;
                    float critTemp = (reactor.block instanceof DeltaReactor) ? ((DeltaReactor)reactor.block).criticalTemp : 1000f;
                    String col = temp >= critTemp ? "red" :
                                 temp >= alertThreshold ? "orange" : "green";
                    return "[" + col + "]" + (int)temp + "°C[]";
                }).right().row();

                t.add("[lightgray]Umbral alarma:[]").left();
                t.table(ct -> {
                    ct.button("[lightgray]-[]", () -> alertThreshold = Mathf.clamp(alertThreshold - 50f, 200f, 1200f)).size(26f,26f);
                    ct.label(() -> "[orange]" + (int)alertThreshold + "°C[]").minWidth(60f).center();
                    ct.button("[lightgray]+[]", () -> alertThreshold = Mathf.clamp(alertThreshold + 50f, 200f, 1200f)).size(26f,26f);
                }).right().row();

                t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

                t.label(() -> alertActive ? "[red]!!! ALERTA TEMPERATURA !!![]" : "[green]Normal[]").colspan(2).center().row();
            }
        }

        @Override
        public boolean onConfigureBuildTapped(Building other) {
            if (this == other) return false;
            if (other instanceof DeltaReactor.DeltaReactorBuild && other.team == team) {
                configure(other.pos());
                return false;
            }
            return true;
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
        public Integer config() { return reactor == null ? -1 : reactor.pos(); }

        @Override
        public void write(Writes w) {
            super.write(w);
            w.i(reactor == null ? -1 : reactor.pos());
            w.f(alertThreshold);
        }

        @Override
        public void read(Reads r, byte rev) {
            super.read(r,rev);
            int pos = r.i();
            alertThreshold = r.f();
            if (pos != -1) {
                Building b = Vars.world.build(pos);
                if (b instanceof DeltaReactor.DeltaReactorBuild) {
                    reactor = (DeltaReactor.DeltaReactorBuild) b;
                }
            }
        }
    }
}
