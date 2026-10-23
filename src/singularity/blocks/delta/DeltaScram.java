package singularity.blocks.delta;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.Tile;

/**
 * DeltaScram -- Sistema SCRAM de emergencia.
 * Apaga instantaneamente TODOS los reactores Delta en rango.
 * Cooldown de 120s antes de poder usar de nuevo.
 */
public class DeltaScram extends Block {

    public int scramRange = 60;
    public float cooldownTime = 120f * 60f; // 120 segundos

    public DeltaScram(String name) {
        super(name);
        size = 2;
        update = true;
        solid = true;
        hasPower = true;
        consumesPower = true;
        configurable = true;
        saveConfig = false;
        category = Category.power;
        health = 500;
        buildType = DeltaScramBuild::new;
    }

    public class DeltaScramBuild extends Building {

        public float cooldown = 0f;
        private Table configTable;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            return this;
        }

        @Override
        public void updateTile() {
            if (cooldown > 0f) cooldown -= Time.delta;
        }

        private void triggerScram() {
            if (cooldown > 0f) return;

            int count = 0;
            int tx = tileX(), ty = tileY();
            for (int dx = -scramRange; dx <= scramRange; dx++) {
                for (int dy = -scramRange; dy <= scramRange; dy++) {
                    Building b = Vars.world.build(tx + dx, ty + dy);
                    if (b instanceof DeltaReactor.DeltaReactorBuild && b.team == team) {
                        DeltaReactor.DeltaReactorBuild r = (DeltaReactor.DeltaReactorBuild) b;
                        r.scrammed = true;
                        r.meltTimer = 0f;
                        count++;
                    }
                }
            }

            if (count > 0) cooldown = cooldownTime;
        }

        @Override
        public void buildConfiguration(Table t) {
            configTable = t;
            t.defaults().pad(3f);

            t.add(gradient("SISTEMA SCRAM", 0xFF0000, 0xFF4400)).center().colspan(2).row();
            t.image().color(Color.red).height(4f).growX().colspan(2).pad(2f).row();

            t.label(() -> cooldown > 0f
                ? "[gray]Cooldown: " + (int)(cooldown/60f) + "s[]"
                : "[red]LISTO[]"
            ).colspan(2).center().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.add("[lightgray]Apaga: [white]todos los reactores[]").colspan(2).center().row();
            t.add("[lightgray]Radio: [accent]" + scramRange + " tiles[]").colspan(2).center().row();
            t.add("[lightgray]Cooldown: [orange]" + (int)(cooldownTime/60f) + "s[]").colspan(2).center().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.button(btn -> {
                btn.clearChildren();
                boolean ready = cooldown <= 0f;
                btn.add(new arc.scene.ui.Label(ready ? "[red]!!! SCRAM TOTAL !!![]" : "[gray]Cooldown...[]")).center();
            }, this::triggerScram).size(200f, 50f).disabled(b -> cooldown > 0f).colspan(2).center().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.add("[gray]EMERGENCIA UNICAMENTE\nDetiene fusion nuclear[]").colspan(2).center().row();
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
        public boolean shouldConsume() { return true; }

        @Override
        public void write(Writes w) { super.write(w); w.f(cooldown); }

        @Override
        public void read(Reads r, byte rev) { super.read(r,rev); cooldown=r.f(); }
    }
}
