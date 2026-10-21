package singularity.blocks;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/**
 * UniversalBlockManager - Gestor universal de bloques configurables.
 *
 * Detecta en rango bloques con:
 *  - update -> toggle ON/OFF
 *  - config Item / Liquid -> selector
 */
public class UniversalBlockManager extends Block {

    public int manageRange = 15;

    public UniversalBlockManager(String name) {
        super(name);

        size = 3;
        update = true;
        solid = true;
        hasPower = true;
        consumesPower = true;
        outputsPower = false;
        configurable = true;
        saveConfig = false;

        category = Category.logic;
        health = 500;

        buildType = UniversalBlockManagerBuild::new;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, manageRange, StatUnit.blocks);
    }

    public class UniversalBlockManagerBuild extends Building {

        private final Seq<Building> managed = new Seq<>();

        /** 0=todos, 1=toggleables, 2=config-item, 3=config-liquid */
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
            managed.clear();
            int tx = tileX(), ty = tileY();

            for (int dx = -manageRange; dx <= manageRange; dx++) {
                for (int dy = -manageRange; dy <= manageRange; dy++) {
                    Building b = Vars.world.build(tx + dx, ty + dy);
                    if (b == null || b.team != team || b == this) continue;
                    if (managed.contains(b)) continue;

                    boolean hasToggle = b.block.update;
                    boolean hasConfig = supportsItemConfig(b) || supportsLiquidConfig(b);

                    if (hasToggle || hasConfig) {
                        managed.add(b);
                    }
                }
            }
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

            t.add("[accent]* UNIVERSAL BLOCK MANAGER[]").center().colspan(2).row();
            t.image().color(Pal.accent).height(2f).growX().colspan(2).pad(2f).row();

            t.label(() -> "[lightgray]Rango: " + manageRange + " tiles   Bloques: [accent]"
                + managed.size + "[]").colspan(2).left().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.table(tabs -> {
                tabs.button("[white]Todos[]", () -> { activeTab = 0; rebuildUI(); }).size(60f, 26f).pad(1f);
                tabs.button("[cyan]Toggle[]", () -> { activeTab = 1; rebuildUI(); }).size(60f, 26f).pad(1f);
                tabs.button("[yellow]Item[]", () -> { activeTab = 2; rebuildUI(); }).size(55f, 26f).pad(1f);
                tabs.button("[violet]Líquido[]", () -> { activeTab = 3; rebuildUI(); }).size(65f, 26f).pad(1f);
                tabs.button("[lightgray]R[]", () -> { scanBlocks(); rebuildUI(); }).size(36f, 26f).pad(1f);
            }).colspan(2).center().row();

            t.image().color(Color.darkGray).height(1f).growX().colspan(2).pad(1f).row();

            Seq<Building> filtered = getFiltered();
            if (filtered.isEmpty()) {
                t.add("[gray]Sin bloques en esta categoría[]").colspan(2).center().pad(8f).row();
            } else {
                int max = Math.min(filtered.size, 12);
                for (int i = 0; i < max; i++) {
                    buildRow(t, filtered.get(i));
                }
                if (filtered.size > 12) {
                    t.add("[gray]... y " + (filtered.size - 12) + " más[]")
                     .colspan(2).center().row();
                }
            }

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.table(bt -> {
                bt.button("[green]Todos ON[]", () -> {
                    for (Building b : managed) b.enabled = true;
                }).size(80f, 28f).pad(2f);

                bt.button("[red]Todos OFF[]", () -> {
                    for (Building b : managed) b.enabled = false;
                }).size(80f, 28f).pad(2f);
            }).colspan(2).center().row();
        }

        private void buildRow(Table t, Building b) {
            String name = b.block.localizedName;
            if (name.length() > 16) name = name.substring(0, 14) + "..";
            final String displayName = name;

            t.table(nt -> {
                nt.add("[white]" + displayName + "[]").left().minWidth(120f);
                nt.label(() -> b.enabled ? "[green]*[]" : "[red]*[]").padLeft(4f);
            }).left();

            t.table(ct -> {
                ct.button("", () -> b.enabled = !b.enabled)
                    .update(btn -> btn.setText(b.enabled ? "[green]ON[]" : "[red]OFF[]"))
                    .size(52f, 26f).pad(2f);

                if (supportsItemConfig(b)) {
                    buildItemSelector(ct, b);
                } else if (supportsLiquidConfig(b)) {
                    buildLiquidSelector(ct, b);
                }
            }).right().row();
        }

        private void buildItemSelector(Table t, Building b) {
            Seq<Item> items = Vars.content.items();

            t.button("[lightgray]<[]", () -> {
                Item cur = getCurrentItem(b);
                int idx = cur == null ? 0 : items.indexOf(cur);
                int next = (idx - 1 + items.size) % items.size;
                b.configure(items.get(next));
            }).size(26f, 26f).pad(1f);

            t.label(() -> {
                Item cur = getCurrentItem(b);
                return cur == null ? "[gray]any[]" : "[yellow]" + cur.localizedName + "[]";
            }).minWidth(60f).center();

            t.button("[lightgray]>[]", () -> {
                Item cur = getCurrentItem(b);
                int idx = cur == null ? -1 : items.indexOf(cur);
                int next = (idx + 1) % items.size;
                b.configure(items.get(next));
            }).size(26f, 26f).pad(1f);

            t.button("[gray]X[]", () -> b.configure(null)).size(26f, 26f).pad(1f);
        }

        private void buildLiquidSelector(Table t, Building b) {
            Seq<Liquid> liquids = Vars.content.liquids();

            t.button("[lightgray]<[]", () -> {
                Liquid cur = getCurrentLiquid(b);
                int idx = cur == null ? 0 : liquids.indexOf(cur);
                int next = (idx - 1 + liquids.size) % liquids.size;
                b.configure(liquids.get(next));
            }).size(26f, 26f).pad(1f);

            t.label(() -> {
                Liquid cur = getCurrentLiquid(b);
                return cur == null ? "[gray]any[]" : "[violet]" + cur.localizedName + "[]";
            }).minWidth(60f).center();

            t.button("[lightgray]>[]", () -> {
                Liquid cur = getCurrentLiquid(b);
                int idx = cur == null ? -1 : liquids.indexOf(cur);
                int next = (idx + 1) % liquids.size;
                b.configure(liquids.get(next));
            }).size(26f, 26f).pad(1f);

            t.button("[gray]X[]", () -> b.configure(null)).size(26f, 26f).pad(1f);
        }

        private Seq<Building> getFiltered() {
            if (activeTab == 0) return managed;

            Seq<Building> result = new Seq<>();
            for (Building b : managed) {
                boolean pass;
                if (activeTab == 1) {
                    pass = b.block.update;
                } else if (activeTab == 2) {
                    pass = supportsItemConfig(b);
                } else if (activeTab == 3) {
                    pass = supportsLiquidConfig(b);
                } else {
                    pass = true;
                }
                if (pass) result.add(b);
            }
            return result;
        }

        /** Detecta bloques que aceptan config Item aunque el valor actual sea null. */
        private boolean supportsItemConfig(Building b) {
            if (!b.block.configurable) return false;
            try {
                Object cfg = b.config();
                if (cfg instanceof Item) return true;
                // null suele significar "any" en sorters/unloaders
                if (cfg == null && b.block.hasItems) return true;
            } catch (Exception ignored) {
            }
            return false;
        }

        private boolean supportsLiquidConfig(Building b) {
            if (!b.block.configurable) return false;
            try {
                Object cfg = b.config();
                if (cfg instanceof Liquid) return true;
                if (cfg == null && b.block.hasLiquids && !b.block.hasItems) return true;
            } catch (Exception ignored) {
            }
            return false;
        }

        private Item getCurrentItem(Building b) {
            try {
                Object cfg = b.config();
                return (cfg instanceof Item) ? (Item) cfg : null;
            } catch (Exception e) {
                return null;
            }
        }

        private Liquid getCurrentLiquid(Building b) {
            try {
                Object cfg = b.config();
                return (cfg instanceof Liquid) ? (Liquid) cfg : null;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public boolean shouldConsume() {
            return true;
        }
    }
}
