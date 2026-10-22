package singularity.blocks;

import arc.graphics.Color;
import arc.scene.ui.Label;
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
import mindustry.world.blocks.power.Battery;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

public class BatteryMonitor extends Block {

    public int monitorRange = 15;
    public float lowThreshold = 0.20f;

    public BatteryMonitor(String name) {
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
        health = 200;
        buildType = BatteryMonitorBuild::new;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, monitorRange, StatUnit.blocks);
    }

    public class BatteryMonitorBuild extends Building {

        private final Seq<Building> batteries = new Seq<>();
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
            if (scanTimer >= SCAN_INTERVAL) { scanTimer = 0; scan(); }
        }

        private void scan() {
            batteries.clear();
            int tx = tileX(), ty = tileY();
            for (int dx = -monitorRange; dx <= monitorRange; dx++) {
                for (int dy = -monitorRange; dy <= monitorRange; dy++) {
                    Building b = Vars.world.build(tx + dx, ty + dy);
                    if (b == null || b.team != team || b == this) continue;
                    if (!(b.block instanceof Battery)) continue;
                    if (!batteries.contains(b)) batteries.add(b);
                }
            }
        }

        private float getBatPct(Building b) {
            if (b.power == null || b.power.graph == null) return 0f;
            float cap = b.power.graph.getTotalBatteryCapacity();
            if (cap <= 0) return 0f;
            return arc.math.Mathf.clamp(b.power.graph.getBatteryStored() / cap);
        }

        private float totalStored() {
            float t = 0f;
            for (Building b : batteries) {
                if (b.power != null && b.power.graph != null) t += b.power.graph.getBatteryStored();
            }
            return t;
        }

        private float totalCap() {
            float t = 0f;
            for (Building b : batteries) {
                if (b.power != null && b.power.graph != null) t += b.power.graph.getTotalBatteryCapacity();
            }
            return t;
        }

        @Override
        public void buildConfiguration(Table t) {
            t.defaults().pad(3f);
            t.add("[cyan]MONITOR BATERIAS[]").center().colspan(3).row();
            t.image().color(Color.valueOf("44aacc")).height(2f).growX().colspan(3).pad(2f).row();

            // Resumen
            t.label(() -> {
                float s = totalStored(), c = totalCap();
                int pct = c > 0 ? (int)(s / c * 100f) : 0;
                int low = 0;
                for (Building b : batteries) if (getBatPct(b) < lowThreshold) low++;
                String alert = low > 0 ? "  [red]! " + low + " baja[]" : "  [green]OK[]";
                return "[lightgray]Total: [cyan]" + fmt(s) + "/" + fmt(c) + "[]  [accent]" + pct + "%" + alert;
            }).colspan(3).left().row();

            t.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            t.add("[lightgray]Bateria[]").left().minWidth(110f);
            t.add("[lightgray]Carga[]").center().minWidth(80f);
            t.add("[lightgray]Estado[]").center().minWidth(55f).row();

            if (batteries.isEmpty()) {
                t.add("[gray]Sin baterias en rango[]").colspan(3).center().pad(8f).row();
            } else {
                int max = Math.min(batteries.size, 10);
                for (int i = 0; i < max; i++) {
                    Building b = batteries.get(i);
                    String rawName = b.block.localizedName;
                    final String name = rawName.length() > 13 ? rawName.substring(0, 11) + ".." : rawName;

                    t.add("[white]" + name + "[]").left().minWidth(110f);

                    t.label(() -> {
                        float pct2 = getBatPct(b);
                        int p = (int)(pct2 * 100f);
                        String col = pct2 < lowThreshold ? "red" : pct2 < 0.5f ? "yellow" : "cyan";
                        return "[" + col + "]" + p + "%[]";
                    }).center().minWidth(80f);

                    t.button(btn -> {
                        btn.clearChildren();
                        btn.add(new Label(b.enabled ? "[green]ON[]" : "[red]OFF[]")).center();
                    }, () -> b.enabled = !b.enabled).size(55f, 26f).center().row();
                }
                if (batteries.size > 10) {
                    t.add("[gray]... y " + (batteries.size - 10) + " mas[]").colspan(3).center().row();
                }
            }

            t.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            t.table(bt -> {
                bt.button("[green]Todas ON[]", () -> { for (Building b : batteries) b.enabled = true; }).size(80f, 28f).pad(2f);
                bt.button("[red]Todas OFF[]", () -> { for (Building b : batteries) b.enabled = false; }).size(80f, 28f).pad(2f);
                bt.button("[lightgray]R[]", this::scan).size(34f, 28f).pad(2f);
            }).colspan(3).center().row();
        }

        private String fmt(float v) {
            if (v >= 1000000f) return Strings.fixed(v / 1000000f, 2) + "M";
            if (v >= 1000f)    return Strings.fixed(v / 1000f, 1) + "k";
            return Strings.fixed(v, 0);
        }

        @Override
        public boolean shouldConsume() { return true; }
    }
}
