package singularity.blocks;

import arc.graphics.Color;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

/**
 * UniversalBlockManager — Gestor universal de bloques configurables.
 *
 * Detecta en rango todos los bloques con:
 *  - enabled  → toggle ON/OFF (cintas, generadores, fabricantes, switches...)
 *  - config() instanceof Item  → selector de item (sorters, unloaders, inversores...)
 *  - config() instanceof Liquid → selector de líquido (liquid sorters...)
 *
 * Llama a building.configure(value) exactamente como lo haría el jugador
 * al hacer clic en el bloque directamente.
 */
public class UniversalBlockManager extends Block {

    /** Rango de detección en tiles. */
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

    // =========================================================================
    // BUILD
    // =========================================================================

    public class UniversalBlockManagerBuild extends Building {

        // Todos los bloques detectados en rango
        private final Seq<Building> managed = new Seq<>();

        // Filtro activo de pestaña
        // 0 = todos, 1 = toggleables, 2 = config-item, 3 = config-liquid
        private int activeTab = 0;

        // Búsqueda por nombre (simple string filter)
        private String searchFilter = "";

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
                    if (b == null) continue;
                    if (b.team != team) continue;
                    if (b == this) continue;
                    if (managed.contains(b)) continue;

                    // Incluir si: tiene enabled (toggleable) O tiene config de item/liquid
                    boolean hasToggle = b.block.update;
                    boolean hasConfig = isItemConfig(b) || isLiquidConfig(b);

                    if (hasToggle || hasConfig) {
                        managed.add(b);
                    }
                }
            }
        }

        // =========================================================================
        // UI
        // =========================================================================

        @Override
        public void buildConfiguration(Table table) {
            buildUI(table);
        }

        private void buildUI(Table t) {
            t.defaults().pad(3f);

            // Título
            t.add("[accent]◈ UNIVERSAL BLOCK MANAGER[]").center().colspan(2).row();
            t.image().color(Pal.accent).height(2f).growX().colspan(2).pad(2f).row();

            // Contador
            t.label(() -> "[lightgray]Rango: " + manageRange + " tiles   Bloques: [accent]"
                + managed.size + "[]").colspan(2).left().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            // Pestañas de filtro
            t.table(tabs -> {
                tabs.button("[white]Todos[]",      () -> activeTab = 0).size(60f, 26f).pad(1f);
                tabs.button("[cyan]Toggle[]",      () -> activeTab = 1).size(60f, 26f).pad(1f);
                tabs.button("[yellow]Item[]",      () -> activeTab = 2).size(55f, 26f).pad(1f);
                tabs.button("[violet]Líquido[]",   () -> activeTab = 3).size(65f, 26f).pad(1f);
                tabs.button("[lightgray]↺[]",      this::scanBlocks   ).size(36f, 26f).pad(1f);
            }).colspan(2).center().row();

            t.image().color(Color.darkGray).height(1f).growX().colspan(2).pad(1f).row();

            // Lista filtrada
            Seq<Building> filtered = getFiltered();
            if (filtered.isEmpty()) {
                t.add("[gray]Sin bloques en esta categoría[]").colspan(2).center().pad(8f).row();
            } else {
                int max = Math.min(filtered.size, 12);
                for (int i = 0; i < max; i++) {
                    Building b = filtered.get(i);
                    buildRow(t, b);
                }
                if (filtered.size > 12) {
                    t.add("[gray]... y " + (filtered.size - 12) + " más[]")
                     .colspan(2).center().row();
                }
            }

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            // Acciones globales
            t.table(bt -> {
                bt.button("[green]Todos ON[]", () -> {
                    for (Building b : managed) b.enabled = true;
                }).size(80f, 28f).pad(2f);

                bt.button("[red]Todos OFF[]", () -> {
                    for (Building b : managed) b.enabled = false;
                }).size(80f, 28f).pad(2f);
            }).colspan(2).center().row();
        }

        /** Construye una fila de la UI para un bloque. */
        private void buildRow(Table t, Building b) {
            String name = b.block.localizedName;
            if (name.length() > 16) name = name.substring(0, 14) + "..";
            final String displayName = name;

            // Nombre + estado
            t.table(nt -> {
                nt.add("[white]" + displayName + "[]").left().minWidth(120f);
                // Indicador estado
                nt.label(() -> b.enabled ? "[green]●[]" : "[red]●[]").padLeft(4f);
            }).left();

            // Controles según tipo de config
            t.table(ct -> {
                // Toggle ON/OFF siempre disponible
                ct.button(btn -> {
                    btn.clearChildren();
                    btn.add(new Label(b.enabled ? "[green]ON[]" : "[red]OFF[]")).center();
                }, () -> b.enabled = !b.enabled).size(52f, 26f).pad(2f);

                // Config de item
                if (isItemConfig(b)) {
                    buildItemSelector(ct, b);
                }
                // Config de líquido
                else if (isLiquidConfig(b)) {
                    buildLiquidSelector(ct, b);
                }
            }).right().row();
        }

        /** Selector de item — flechas < > para ciclar por todos los items. */
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

            // Botón "any" (null = acepta todo)
            t.button("[gray]∅[]", () -> b.configure(null)).size(26f, 26f).pad(1f);
        }

        /** Selector de líquido — flechas < > para ciclar. */
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

            t.button("[gray]∅[]", () -> b.configure(null)).size(26f, 26f).pad(1f);
        }

        // =========================================================================
        // Helpers
        // =========================================================================

        private Seq<Building> getFiltered() {
            if (activeTab == 0) return managed;

            Seq<Building> result = new Seq<>();
            for (Building b : managed) {
                switch (activeTab) {
                    case 1 -> { if (b.block.update) result.add(b); }
                    case 2 -> { if (isItemConfig(b))   result.add(b); }
                    case 3 -> { if (isLiquidConfig(b)) result.add(b); }
                }
            }
            return result;
        }

        private boolean isItemConfig(Building b) {
            try {
                Object cfg = b.config();
                return cfg instanceof Item;
            } catch (Exception e) {
                return false;
            }
        }

        private boolean isLiquidConfig(Building b) {
            try {
                Object cfg = b.config();
                return cfg instanceof Liquid;
            } catch (Exception e) {
                return false;
            }
        }

        private Item getCurrentItem(Building b) {
            try {
                Object cfg = b.config();
                return cfg instanceof Item item ? item : null;
            } catch (Exception e) {
                return null;
            }
        }

        private Liquid getCurrentLiquid(Building b) {
            try {
                Object cfg = b.config();
                return cfg instanceof Liquid liq ? liq : null;
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
