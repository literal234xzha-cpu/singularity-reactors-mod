package singularity.blocks;

import arc.graphics.Color;
import arc.math.Mathf;
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
 * LiquidController — Gestor de tuberías y bloques con líquidos.
 *
 * Detecta en rango todos los bloques que tienen líquidos (hasLiquids).
 * Permite:
 *  - Ver qué líquido contiene cada bloque y cuánto (nivel %)
 *  - Toggle ON/OFF de cada bloque (detiene/reanuda flujo)
 *  - Alertas de nivel bajo (configurable, por defecto < 20%)
 *  - Vista por tipo de líquido (filtra por agua, cryo, slag, oil, etc.)
 *  - Toggle global: detener toda la red de líquidos
 */
public class LiquidController extends Block {

    /** Rango de detección en tiles. */
    public int liquidRange = 12;

    /** Umbral de alerta de nivel bajo (0.0 – 1.0). */
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

    // =========================================================================
    // BUILD
    // =========================================================================

    public class LiquidControllerBuild extends Building {

        private final Seq<Building> liquidBlocks = new Seq<>();

        // Filtro por líquido (null = todos)
        private Liquid filterLiquid = null;

        // Pestaña: 0=todos, 1=alertas, 2=activos, 3=inactivos
        private int activeTab = 0;

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
                    if (b == null) continue;
                    if (b.team != team) continue;
                    if (b == this) continue;
                    if (!b.block.hasLiquids) continue;
                    if (liquidBlocks.contains(b)) continue;

                    liquidBlocks.add(b);
                }
            }
        }

        /** Cuenta cuántos bloques tienen nivel bajo de líquido. */
        private int countLowLevel() {
            int n = 0;
            for (Building b : liquidBlocks) {
                if (b.liquids == null) continue;
                float cap = b.block.liquidCapacity;
                if (cap <= 0) continue;
                float amt = b.liquids.currentAmount();
                if (amt / cap < lowLevelThreshold) n++;
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
                    case 1 -> isLowLevel(b);          // alertas
                    case 2 -> b.enabled;               // activos
                    case 3 -> !b.enabled;              // inactivos
                    default -> true;                   // todos
                };
                if (!pass) continue;

                // Filtro por tipo de líquido
                if (filterLiquid != null && b.liquids != null) {
                    if (b.liquids.current() != filterLiquid) continue;
                }

                result.add(b);
            }
            return result;
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
            t.add("[cyan]💧 LIQUID CONTROLLER[]").center().colspan(3).row();
            t.image().color(Color.valueOf("44aacc")).height(2f).growX().colspan(3).pad(2f).row();

            // Stats generales
            final int total = liquidBlocks.size;
            final int lowAlerts = countLowLevel();
            int activeCount = 0;
            for (Building b : liquidBlocks) if (b.enabled) activeCount++;
            final int active = activeCount;

            t.table(st -> {
                st.add("[lightgray]Rango: [cyan]" + liquidRange + " tiles[]   ").left();
                st.add("[lightgray]Bloques: [accent]" + total + "[]   ").left();
                st.add("[lightgray]Activos: [green]" + active + "[]   ").left();
                if (lowAlerts > 0) {
                    st.add("[red]⚠ " + lowAlerts + " nivel bajo[]").left();
                } else {
                    st.add("[green]✓ Niveles OK[]").left();
                }
            }).colspan(3).left().row();

            t.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            // Filtro por líquido
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
                }).size(28f, 26f);

                ft.button("[gray]∅[]", () -> filterLiquid = null).size(26f, 26f);
            }).right().colspan(2).row();

            t.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            // Pestañas
            t.table(tabs -> {
                tabs.button("[white]Todos[]",    () -> activeTab = 0).size(58f, 26f).pad(1f);
                tabs.button("[red]⚠ Alertas[]", () -> activeTab = 1).size(72f, 26f).pad(1f);
                tabs.button("[green]Activos[]",  () -> activeTab = 2).size(62f, 26f).pad(1f);
                tabs.button("[gray]Inactivos[]", () -> activeTab = 3).size(72f, 26f).pad(1f);
                tabs.button("[lightgray]↺[]",   this::scanBlocks   ).size(34f, 26f).pad(1f);
            }).colspan(3).center().row();

            t.image().color(Color.darkGray).height(1f).growX().colspan(3).pad(1f).row();

            // Encabezados de columna
            t.add("[lightgray]Bloque[]").left().minWidth(110f).padLeft(4f);
            t.add("[lightgray]Nivel[]").center().minWidth(90f);
            t.add("[lightgray]Estado[]").center().minWidth(55f).row();
            t.image().color(Color.darkGray).height(1f).growX().colspan(3).pad(1f).row();

            // Lista
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

            // Acciones globales
            t.table(bt -> {
                bt.button("[green]Todos ON[]", () -> {
                    for (Building b : liquidBlocks) b.enabled = true;
                }).size(80f, 28f).pad(2f);

                bt.button("[red]Todos OFF[]", () -> {
                    for (Building b : liquidBlocks) b.enabled = false;
                }).size(80f, 28f).pad(2f);

                // Umbral de alerta
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

            // Nombre con indicador de alerta
            t.table(nt -> {
                if (isLowLevel(b)) {
                    nt.add("[red]⚠ []").padRight(2f);
                }
                nt.add("[white]" + name + "[]").left();
            }).left().minWidth(110f).padLeft(4f);

            // Nivel de líquido
            t.table(lt -> {
                if (b.liquids != null && b.block.liquidCapacity > 0) {
                    float cap = b.block.liquidCapacity;
                    float amt = b.liquids.currentAmount();
                    float pct = amt / cap;
                    Liquid liq = b.liquids.current();

                    String liqName = liq != null ? liq.localizedName : "vacío";
                    Color liqColor = pct < lowLevelThreshold ? Color.red
                                   : pct < 0.5f ? Color.yellow
                                   : Color.cyan;

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

            // Toggle ON/OFF
            t.button(btn -> {
                btn.clearChildren();
                btn.add(new Label(b.enabled ? "[green]ON[]" : "[red]OFF[]")).center();
            }, () -> b.enabled = !b.enabled).size(55f, 26f).center().row();
        }

        private String hexColor(Color c) {
            return "[#" + c.toString().substring(0, 6) + "]";
        }

        @Override
        public boolean shouldConsume() {
            return true;
        }
    }
}
