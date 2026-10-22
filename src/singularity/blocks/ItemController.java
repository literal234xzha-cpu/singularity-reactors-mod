package singularity.blocks;

import arc.graphics.Color;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.Junction;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

public class ItemController extends Block {

    public int controlRange = 15;

    public ItemController(String name) {
        super(name);
        size = 2;
        update = true;
        solid = true;
        hasPower = true;
        consumesPower = true;
        outputsPower = false;
        configurable = true;
        saveConfig = false;
        category = Category.logic;
        health = 250;
        buildType = ItemControllerBuild::new;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, controlRange, StatUnit.blocks);
    }

    public class ItemControllerBuild extends Building {

        private final Seq<Building> managed = new Seq<>();
        private int activeTab = 0; // 0=todos, 1=cintas, 2=sorters
        private int scanTimer = 0;
        private static final int SCAN_INTERVAL = 60;
        private Table configTable;

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
            managed.clear();
            int tx = tileX(), ty = tileY();
            for (int dx = -controlRange; dx <= controlRange; dx++) {
                for (int dy = -controlRange; dy <= controlRange; dy++) {
                    Building b = Vars.world.build(tx + dx, ty + dy);
                    if (b == null || b.team != team || b == this) continue;
                    if (managed.contains(b)) continue;
                    if (isItemBlock(b)) managed.add(b);
                }
            }
        }

        private boolean isItemBlock(Building b) {
            if (b.block instanceof Conveyor) return true;
            if (b.block instanceof Router) return true;
            if (b.block instanceof Junction) return true;
            if (b.block instanceof Sorter) return true;
            if (b.block.hasItems && b.block.configurable) return true;
            return false;
        }

        private boolean isSorterType(Building b) {
            if (b.block instanceof Sorter) return true;
            try { return b.block.configurable && (b.config() instanceof Item); }
            catch (Exception e) { return false; }
        }

        private boolean isConveyorType(Building b) {
            return (b.block instanceof Conveyor) || (b.block instanceof Router) || (b.block instanceof Junction);
        }

        private Item getCurrentItem(Building b) {
            try {
                Object cfg = b.config();
                return (cfg instanceof Item) ? (Item) cfg : null;
            } catch (Exception e) { return null; }
        }

        private Seq<Building> getFiltered() {
            if (activeTab == 0) return managed;
            Seq<Building> result = new Seq<>();
            for (Building b : managed) {
                if (activeTab == 1 && isConveyorType(b)) result.add(b);
                else if (activeTab == 2 && isSorterType(b)) result.add(b);
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
            t.add("[yellow]CONTROLADOR ITEMS[]").center().colspan(2).row();
            t.image().color(Color.valueOf("ccaa00")).height(2f).growX().colspan(2).pad(2f).row();

            t.label(() -> "[lightgray]Rango: [accent]" + controlRange + " tiles[]   Bloques: [accent]" + managed.size + "[]").colspan(2).left().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.table(tabs -> {
                tabs.button("[white]Todos[]",   () -> { activeTab = 0; rebuildUI(); }).size(60f, 26f).pad(1f);
                tabs.button("[cyan]Cintas[]",   () -> { activeTab = 1; rebuildUI(); }).size(60f, 26f).pad(1f);
                tabs.button("[yellow]Sorters[]",() -> { activeTab = 2; rebuildUI(); }).size(65f, 26f).pad(1f);
                tabs.button("[lightgray]R[]",   () -> { scan(); rebuildUI(); }).size(32f, 26f).pad(1f);
            }).colspan(2).center().row();

            t.image().color(Color.darkGray).height(1f).growX().colspan(2).pad(1f).row();

            Seq<Building> filtered = getFiltered();
            if (filtered.isEmpty()) {
                t.add("[gray]Sin bloques en esta categoria[]").colspan(2).center().pad(8f).row();
            } else {
                int max = Math.min(filtered.size, 12);
                for (int i = 0; i < max; i++) {
                    Building b = filtered.get(i);
                    String rawName = b.block.localizedName;
                    final String name = rawName.length() > 14 ? rawName.substring(0, 12) + ".." : rawName;

                    t.table(nt -> {
                        nt.add("[white]" + name + "[]").left().minWidth(100f);
                        nt.label(() -> b.enabled ? "[green]*[]" : "[red]*[]").padLeft(2f);
                    }).left();

                    t.table(ct -> {
                        ct.button(btn -> {
                            btn.clearChildren();
                            btn.add(new Label(b.enabled ? "[green]ON[]" : "[red]OFF[]")).center();
                        }, () -> b.enabled = !b.enabled).size(48f, 26f).pad(1f);

                        if (isSorterType(b)) {
                            Seq<Item> items = Vars.content.items();
                            ct.button("[lightgray]<[]", () -> {
                                Item cur = getCurrentItem(b);
                                int idx = cur == null ? 0 : items.indexOf(cur);
                                b.configure(items.get((idx - 1 + items.size) % items.size));
                            }).size(24f, 26f).pad(1f);

                            ct.label(() -> {
                                Item cur = getCurrentItem(b);
                                return cur == null ? "[gray]any[]" : "[yellow]" + cur.localizedName + "[]";
                            }).minWidth(50f).center();

                            ct.button("[lightgray]>[]", () -> {
                                Item cur = getCurrentItem(b);
                                int idx = cur == null ? -1 : items.indexOf(cur);
                                b.configure(items.get((idx + 1) % items.size));
                            }).size(24f, 26f).pad(1f);

                            ct.button("[gray]X[]", () -> b.configure(null)).size(24f, 26f).pad(1f);
                        }
                    }).right().row();
                }
                if (filtered.size > 12) {
                    t.add("[gray]... y " + (filtered.size - 12) + " mas[]").colspan(2).center().row();
                }
            }

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.table(bt -> {
                bt.button("[green]Todos ON[]",  () -> { for (Building b : managed) b.enabled = true;  }).size(80f, 28f).pad(2f);
                bt.button("[red]Todos OFF[]",   () -> { for (Building b : managed) b.enabled = false; }).size(80f, 28f).pad(2f);
            }).colspan(2).center().row();
        }

        @Override
        public boolean shouldConsume() { return true; }
    }
}
