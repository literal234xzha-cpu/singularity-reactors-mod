package singularity.blocks;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

public class PowerMonitorLocal extends Block {

    public PowerMonitorLocal(String name) {
        super(name);
        size = 2;
        update = true;
        solid = true;
        hasPower = true;
        consumesPower = true;
        outputsPower = false;
        configurable = true;
        saveConfig = false;
        category = Category.power;
        health = 150;
        buildType = PowerMonitorLocalBuild::new;
    }

    public class PowerMonitorLocalBuild extends Building {

        public float lastProduction = 0f;
        public float lastConsumption = 0f;
        public float lastSatisfaction = 1f;
        public float lastBatStored = 0f;
        public float lastBatCap = 0f;
        public boolean inDeficit = false;
        public float alertTimer = 0f;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            return this;
        }

        @Override
        public void updateTile() {
            if (power == null || power.graph == null) return;
            PowerGraph g = power.graph;
            lastProduction   = g.getPowerProduced()  * 60f;
            lastConsumption  = g.getPowerNeeded()    * 60f;
            lastSatisfaction = g.getSatisfaction();
            lastBatStored    = g.getBatteryStored();
            lastBatCap       = g.getTotalBatteryCapacity();
            inDeficit = (lastProduction - lastConsumption) < -0.5f;
            if (inDeficit) alertTimer += arc.util.Time.delta;
            else alertTimer = 0f;
        }

        @Override
        public void buildConfiguration(Table t) {
            t.defaults().pad(3f);
            t.add("[accent]MONITOR LOCAL[]").center().colspan(2).row();
            t.image().color(Pal.accent).height(2f).growX().colspan(2).pad(2f).row();

            t.add("[lightgray]Produccion:[]").left();
            t.label(() -> "[green]" + fmt(lastProduction) + "/s[]").right().row();

            t.add("[lightgray]Consumo:[]").left();
            t.label(() -> "[red]" + fmt(lastConsumption) + "/s[]").right().row();

            t.add("[lightgray]Balance:[]").left();
            t.label(() -> {
                float bal = lastProduction - lastConsumption;
                String sign = bal >= 0 ? "+" : "";
                String col = bal >= 0 ? "green" : "red";
                return "[" + col + "]" + sign + fmt(bal) + "/s[]";
            }).right().row();

            t.add("[lightgray]Satisfaccion:[]").left();
            t.label(() -> {
                int pct = (int)(lastSatisfaction * 100f);
                String col = pct >= 99 ? "green" : pct >= 50 ? "yellow" : "red";
                return "[" + col + "]" + pct + "%[]";
            }).right().row();

            if (lastBatCap > 0) {
                t.add("[lightgray]Baterias:[]").left();
                t.label(() -> "[cyan]" + fmt(lastBatStored) + " / " + fmt(lastBatCap) + "[]").right().row();
            }

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.label(() -> {
                if (inDeficit) {
                    boolean blink = ((int)(alertTimer / 30f) % 2) == 0;
                    return blink ? "[red]! DEFICIT DE ENERGIA[]" : "[scarlet]! DEFICIT[]";
                }
                return lastSatisfaction >= 0.99f ? "[green]OK Red estable[]" : "[yellow]~ Energia parcial[]";
            }).colspan(2).center().row();
        }

        private String fmt(float v) {
            float a = Math.abs(v);
            if (a >= 1000000f) return Strings.fixed(v / 1000000f, 2) + "M";
            if (a >= 1000f)    return Strings.fixed(v / 1000f, 1) + "k";
            return Strings.fixed(v, 0);
        }

        @Override
        public boolean shouldConsume() { return true; }
    }
}
