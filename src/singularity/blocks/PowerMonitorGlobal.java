package singularity.blocks;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
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
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

public class PowerMonitorGlobal extends Block {

    public int scanRange = 40;

    public PowerMonitorGlobal(String name) {
        super(name);
        size = 3;
        update = true;
        solid = true;
        hasPower = true;
        consumesPower = true;
        outputsPower = false;
        configurable = true;
        saveConfig = false;
        category = Category.power;
        health = 400;
        buildType = PowerMonitorGlobalBuild::new;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, scanRange, StatUnit.blocks);
    }

    public class PowerMonitorGlobalBuild extends Building {

        public float totalProduction = 0f;
        public float totalConsumption = 0f;
        public float totalBatStored = 0f;
        public float totalBatCap = 0f;
        public int networkCount = 0;

        private int scanTimer = 0;
        private static final int SCAN_INTERVAL = 60;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            scan();
            return this;
        }

        @Override
        public void updateTile() {
            scanTimer++;
            if (scanTimer >= SCAN_INTERVAL) {
                scanTimer = 0;
                scan();
            }
        }

        private void scan() {
            ObjectSet<PowerGraph> graphs = new ObjectSet<>();
            int tx = tileX(), ty = tileY();

            for (int dx = -scanRange; dx <= scanRange; dx++) {
                for (int dy = -scanRange; dy <= scanRange; dy++) {
                    Building b = Vars.world.build(tx + dx, ty + dy);
                    if (b == null || b.team != team) continue;
                    if (b.power == null || b.power.graph == null) continue;
                    graphs.add(b.power.graph);
                }
            }

            totalProduction  = 0f;
            totalConsumption = 0f;
            totalBatStored   = 0f;
            totalBatCap      = 0f;
            networkCount     = graphs.size;

            for (PowerGraph g : graphs) {
                totalProduction  += g.getPowerProduced()  * 60f;
                totalConsumption += g.getPowerNeeded()    * 60f;
                totalBatStored   += g.getBatteryStored();
                totalBatCap      += g.getTotalBatteryCapacity();
            }
        }

        @Override
        public void buildConfiguration(Table t) {
            t.defaults().pad(3f);
            t.add("[accent]MONITOR GLOBAL[]").center().colspan(2).row();
            t.image().color(Pal.accent).height(2f).growX().colspan(2).pad(2f).row();

            t.add("[lightgray]Rango:[]").left();
            t.add("[accent]" + scanRange + " tiles[]").right().row();

            t.add("[lightgray]Redes:[]").left();
            t.label(() -> "[accent]" + networkCount + "[]").right().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.add("[lightgray]Produccion total:[]").left();
            t.label(() -> "[green]" + fmt(totalProduction) + "/s[]").right().row();

            t.add("[lightgray]Consumo total:[]").left();
            t.label(() -> "[red]" + fmt(totalConsumption) + "/s[]").right().row();

            t.add("[lightgray]Balance total:[]").left();
            t.label(() -> {
                float bal = totalProduction - totalConsumption;
                String sign = bal >= 0 ? "+" : "";
                String col  = bal >= 0 ? "green" : "red";
                return "[" + col + "]" + sign + fmt(bal) + "/s[]";
            }).right().row();

            if (totalBatCap > 0) {
                t.add("[lightgray]Baterias:[]").left();
                t.label(() -> "[cyan]" + fmt(totalBatStored) + " / " + fmt(totalBatCap) + "[]").right().row();

                t.add("[lightgray]Carga:[]").left();
                t.label(() -> {
                    int pct = totalBatCap > 0 ? (int)(totalBatStored / totalBatCap * 100f) : 0;
                    String col = pct >= 60 ? "green" : pct >= 30 ? "yellow" : "red";
                    return "[" + col + "]" + pct + "%[]";
                }).right().row();
            }

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.label(() -> {
                float bal = totalProduction - totalConsumption;
                if (networkCount == 0) return "[gray]Sin redes en rango[]";
                if (bal < -0.5f) return "[red]! DEFICIT GLOBAL[]";
                return "[green]OK " + networkCount + " redes estables[]";
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
