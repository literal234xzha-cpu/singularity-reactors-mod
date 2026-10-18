package singularity.blocks;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/**
 * LiquidController — Gestor de tuberías y bloques con líquidos.
 */
public class LiquidController extends Block {

    public int liquidRange = 12;
    public float lowLevelThreshold = 0.20f;

    public LiquidController(String name) {
        super(name);

        size = 2;
        update = true;
        solid = true;
        hasPower = true;
        consumesPower = true;
        outputsPower = false;
        configurable = true;
        saveConfig = false;

        category = Category.liquid;
        health = 250;

        buildType = LiquidControllerBuild::new;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, liquidRange, StatUnit.blocks);
    }

    public class LiquidControllerBuild extends Building {

        private final Seq<Building> liquidBlocks = new Seq<>();
        private Liquid filterLiquid = null;
        /** 0=todos, 1=alertas, 2=activos, 3=inactivos */
        private int activeTab = 0;

        private Table configTable;
        private int scanTimer = 0;
        private static final int SCAN_INTERVAL = 60;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            scanBlocks();
            return this;
        }

        @Override
        public void updateTile() {
            scanTimer++;
            if (scanTimer >= SCAN_INTERVAL) {
                scanTimer = 0;
                scanBlocks();
            }
        }

        private void scanBlocks() {
            liquidBlocks.clear();
            int tx = tileX(), ty = tileY();

            for (int dx = -liquidRange; dx <= liquidRange; dx++) {
                for (int dy = -liquidRange; dy <= liquidRange; dy++) {
                    Building b = Vars.world.build(tx + dx, ty + dy);
                    if (b == null || b.team != team || b == this) continue;
                    if (!b.block.hasLiquids) continue;
                    if (liquidBlocks.contains(b)) continue;
                    liquidBlocks.add(b);
                }
            }
        }

        private int countLowLevel() {
            int n = 0;
            for (Building b : liquidBlocks) {
                if (isLowLevel(b)) n++;
            }
            return n;
        }

        private boolean isLowLevel(Building b) {
            if (b.liquids == null) return false;
            float cap = b.block.liquidCapacity;
            if (cap <= 0) return false;
            return b.liquids.currentAmount() / cap < lowLevelThreshold;
        }

        private Seq<Building> getFiltered() {
            Seq<Building> result = new Seq<>();
            for (Building b : liquidBlocks) {
                boolean pass = switch (activeTab) {
                    case 1 -> isLowLevel(b);
                    case 2 -> b.enabled;
                    case 3 -> !b.enabled;
                    default -> true;
                };
                if (!pass) continue;

                if (filterLiquid != null && b.liquids != null) {
                    if (b.liquids.current() != filterLiquid) continue;
                }

                result.add(b);
            }
            return result;
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

            t.add("[cyan]💧 LIQUID CONTROLLER[]").center().colspan(3).row();
            t.image().color(Color.valueOf("44aacc")).height(2f).growX().colspan(3).pad(2f).row();

            t.table(st -> {
                st.label(() -> {
                    int lowAlerts = countLowLevel();
                    int active = 0;
                    for (Building b : liquidBlocks) if (b.enabled) active++;
                    String alerts = lowAlerts > 0
                        ? "[red]⚠ " + lowAlerts + " nivel bajo[]"
                        : "[green]✓ Niveles OK[]";
                    return "[lightgray]Rango: [cyan]" + liquidRange + " tiles[]   "
                        + "[lightgray]Bloques: [accent]" + liquidBlocks.size + "[]   "
                        + "[lightgray]Activos: [green]" + active + "[]   " + alerts;
                }).left();
            }).colspan(3).left().row();

            t.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            t.add("[lightgray]Filtrar líquido:[]").left();
            t.table(ft -> {
                ft.button("[lightgray]< []", () -> {
                    Seq<Liquid> lqs = Vars.content.liquids();
                    if (filterLiquid == null) {
                        filterLiquid = lqs.get(lqs.size - 1);
                    } else {
                        int idx = lqs.indexOf(filterLiquid);
                        filterLiquid = idx <= 0 ? null : lqs.get(idx - 1);
                    }
                    rebuildUI();
                }).size(28f, 26f);

                ft.label(() -> filterLiquid == null
                    ? "[gray]todos[]"
                    : "[cyan]" + filterLiquid.localizedName + "[]"
                ).minWidth(70f).center();

                ft.button("[lightgray] >[]", () -> {
                    Seq<Liquid> lqs = Vars.content.liquids();
                    if (filterLiquid == null) {
                        filterLiquid = lqs.first();
                    } else {
                        int idx = lqs.indexOf(filterLiquid);
                        filterLiquid = idx >= lqs.size - 1 ? null : lqs.get(idx + 1);
                    }
                    rebuildUI();
                }).size(28f, 26f);

                ft.button("[gray]∅[]", () -> {
                    filterLiquid = null;
                    rebuildUI();
                }).size(26f, 26f);
            }).right().colspan(2).row();

            t.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            t.table(tabs -> {
                tabs.button("[white]Todos[]", () -> { activeTab = 0; rebuildUI(); }).size(58f, 26f).pad(1f);
                tabs.button("[red]⚠ Alertas[]", () -> { activeTab = 1; rebuildUI(); }).size(72f, 26f).pad(1f);
                tabs.button("[green]Activos[]", () -> { activeTab = 2; rebuildUI(); }).size(62f, 26f).pad(1f);
                tabs.button("[gray]Inactivos[]", () -> { activeTab = 3; rebuildUI(); }).size(72f, 26f).pad(1f);
                tabs.button("[lightgray]↺[]", () -> { scanBlocks(); rebuildUI(); }).size(34f, 26f).pad(1f);
            }).colspan(3).center().row();

            t.image().color(Color.darkGray).height(1f).growX().colspan(3).pad(1f).row();

            t.add("[lightgray]Bloque[]").left().minWidth(110f).padLeft(4f);
            t.add("[lightgray]Nivel[]").center().minWidth(90f);
            t.add("[lightgray]Estado[]").center().minWidth(55f).row();
            t.image().color(Color.darkGray).height(1f).growX().colspan(3).pad(1f).row();

            Seq<Building> filtered = getFiltered();
            if (filtered.isEmpty()) {
                t.add("[gray]Sin bloques en esta categoría[]").colspan(3).center().pad(8f).row();
            } else {
                int max = Math.min(filtered.size, 10);
                for (int i = 0; i < max; i++) {
                    buildRow(t, filtered.get(i));
                }
                if (filtered.size > 10) {
                    t.add("[gray]... y " + (filtered.size - 10) + " más[]")
                     .colspan(3).center().row();
                }
            }

            t.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            t.table(bt -> {
                bt.button("[green]Todos ON[]", () -> {
                    for (Building b : liquidBlocks) b.enabled = true;
                }).size(80f, 28f).pad(2f);

                bt.button("[red]Todos OFF[]", () -> {
                    for (Building b : liquidBlocks) b.enabled = false;
                }).size(80f, 28f).pad(2f);

                bt.table(at -> {
                    at.add("[lightgray]Alerta<[]").left();
                    at.label(() -> "[yellow]" + (int)(lowLevelThreshold * 100f) + "%[]").minWidth(30f).center();
                    at.button("[lightgray]-[]", () ->
                        lowLevelThreshold = Mathf.clamp(lowLevelThreshold - 0.05f, 0.05f, 0.95f)
                    ).size(22f, 22f);
                    at.button("[lightgray]+[]", () ->
                        lowLevelThreshold = Mathf.clamp(lowLevelThreshold + 0.05f, 0.05f, 0.95f)
                    ).size(22f, 22f);
                }).pad(2f);
            }).colspan(3).center().row();
        }

        private void buildRow(Table t, Building b) {
            String rawName = b.block.localizedName;
            final String name = rawName.length() > 14 ? rawName.substring(0, 12) + ".." : rawName;

            t.table(nt -> {
                if (isLowLevel(b)) {
                    nt.add("[red]⚠ []").padRight(2f);
                }
                nt.add("[white]" + name + "[]").left();
            }).left().minWidth(110f).padLeft(4f);

            t.table(lt -> {
                if (b.liquids != null && b.block.liquidCapacity > 0) {
                    lt.label(() -> {
                        float a2 = b.liquids.currentAmount();
                        float p2 = b.block.liquidCapacity > 0 ? a2 / b.block.liquidCapacity : 0;
                        Color c2 = p2 < lowLevelThreshold ? Color.red
                                  : p2 < 0.5f ? Color.yellow
                                  : Color.cyan;
                        return hexColor(c2) + (int)(p2 * 100f) + "% "
                            + (b.liquids.current() != null ? b.liquids.current().localizedName : "")
                            + "[]";
                    }).minWidth(90f).center();
                } else {
                    lt.add("[gray]N/A[]").center();
                }
            }).center().minWidth(90f);

            t.button("", () -> b.enabled = !b.enabled)
                .update(btn -> btn.setText(b.enabled ? "[green]ON[]" : "[red]OFF[]"))
                .size(55f, 26f).center().row();
        }

        private String hexColor(Color c) {
            return "[#" + c.toString().substring(0, 6) + "]";
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(lowLevelThreshold);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            lowLevelThreshold = read.f();
        }

        @Override
        public boolean shouldConsume() {
            return true;
        }
    }
}
