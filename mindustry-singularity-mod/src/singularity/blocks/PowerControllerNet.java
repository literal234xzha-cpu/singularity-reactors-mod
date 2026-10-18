package singularity.blocks;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

/**
 * PowerControllerNet — Gestor de red eléctrica completa (estilo "Shizuku").
 *
 * Accede a la red via PowerGraph sin tocar directamente cada bloque.
 * Puede:
 *  - Ver producción / consumo / baterías de toda la red
 *  - Listar y toggle ON/OFF productores, consumidores y baterías
 *  - Aplicar un cap global de producción a la red (limita todos los productores)
 *  - Modo de emergencia: apaga todos los consumidores no esenciales
 *  - Ver satisfacción y balance en tiempo real
 */
public class PowerControllerNet extends Block {

    public PowerControllerNet(String name) {
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
        health = 600;

        buildType = PowerControllerNetBuild::new;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.powerUse, 5f, StatUnit.powerSecond);
    }

    // =========================================================================
    // BUILD
    // =========================================================================

    public class PowerControllerNetBuild extends Building {

        // Cap global de producción (0.0 – 1.0), aplicado a todos los productores
        public float globalProductionCap = 1.0f;

        // Modo emergencia: deshabilita consumidores
        public boolean emergencyMode = false;

        // Pestaña activa: 0=resumen, 1=productores, 2=consumidores, 3=baterias
        public int activeTab = 0;

        // Cache de listas del grafo (se refresca desde el grafo en cada draw)
        private PowerGraph cachedGraph() {
            return (power != null) ? power.graph : null;
        }

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            return this;
        }

        @Override
        public void updateTile() {
            PowerGraph graph = cachedGraph();
            if (graph == null) return;

            // Aplicar cap global a todos los productores del grafo
            for (Building b : graph.producers) {
                if (!(b instanceof PowerGenerator.GeneratorBuild gen)) continue;
                if (!gen.enabled) continue;
                if (gen.productionEfficiency > globalProductionCap) {
                    gen.productionEfficiency = Mathf.lerpDelta(
                        gen.productionEfficiency, globalProductionCap, 0.05f);
                }
            }

            // Modo emergencia: deshabilita consumidores (excepto generadores)
            if (emergencyMode) {
                for (Building b : graph.consumers) {
                    if (b instanceof PowerGenerator.GeneratorBuild) continue;
                    b.enabled = false;
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
            PowerGraph graph = cachedGraph();

            t.defaults().pad(3f);

            // ---- Título ----
            t.add("[accent]⚡ POWER CONTROLLER NET[]").center().colspan(2).row();
            t.image().color(Pal.accent).height(2f).growX().colspan(2).pad(2f).row();

            // ---- Stats de red ----
            if (graph != null) {
                float prod = graph.getPowerProduced() * 60f;
                float cons = graph.getPowerNeeded()   * 60f;
                float sat  = graph.getSatisfaction();
                float batStored = graph.getBatteryStored();
                float batCap    = graph.getTotalBatteryCapacity();

                t.table(st -> {
                    st.defaults().pad(2f).left();
                    st.add("[lightgray]Producción:[]").left();
                    st.add("[green]" + fmt(prod) + "/s[]").right().row();
                    st.add("[lightgray]Consumo:[]").left();
                    st.add("[red]" + fmt(cons) + "/s[]").right().row();
                    st.add("[lightgray]Balance:[]").left();
                    float bal = prod - cons;
                    String balStr = (bal >= 0 ? "[green]+" : "[red]") + fmt(bal) + "/s[]";
                    st.add(balStr).right().row();
                    st.add("[lightgray]Satisfacción:[]").left();
                    Color sc = sat >= 0.99f ? Color.green : sat >= 0.5f ? Color.yellow : Color.red;
                    st.add(hexColor(sc) + (int)(sat * 100f) + "%[]").right().row();
                    if (batCap > 0) {
                        st.add("[lightgray]Baterías:[]").left();
                        st.add("[cyan]" + fmt(batStored) + " / " + fmt(batCap) + "[]").right().row();
                    }
                    st.add("[lightgray]Productores:[]").left();
                    st.add("[accent]" + graph.producers.size + "[]").right().row();
                    st.add("[lightgray]Consumidores:[]").left();
                    st.add("[accent]" + graph.consumers.size + "[]").right().row();
                }).colspan(2).growX().row();
            } else {
                t.add("[gray]Sin red eléctrica conectada[]").colspan(2).center().row();
            }

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            // ---- Cap global de producción ----
            t.add("[lightgray]Cap global producción:[]").left();
            t.table(ct -> {
                ct.button("[lightgray]-10%[]", () -> {
                    globalProductionCap = Mathf.clamp(globalProductionCap - 0.10f, 0f, 1f);
                }).size(52f, 28f);
                ct.label(() -> {
                    int pct = (int)(globalProductionCap * 100f);
                    Color c = pct >= 75 ? Color.green : pct >= 40 ? Color.yellow : Color.red;
                    return hexColor(c) + pct + "%[]";
                }).minWidth(40f).center();
                ct.button("[lightgray]+10%[]", () -> {
                    globalProductionCap = Mathf.clamp(globalProductionCap + 0.10f, 0f, 1f);
                }).size(52f, 28f);
                ct.button("[white]100%[]", () -> globalProductionCap = 1.0f).size(44f, 28f);
            }).right().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            // ---- Modo emergencia ----
            t.table(em -> {
                em.add("[lightgray]Modo emergencia:[]").left().padRight(8f);
                em.button(btn -> {
                    btn.clearChildren();
                    btn.add(new Label(emergencyMode ? "[red]ACTIVO[]" : "[gray]INACTIVO[]")).center();
                }, () -> {
                    emergencyMode = !emergencyMode;
                    // Al desactivar, re-habilitar consumidores
                    if (!emergencyMode && graph != null) {
                        for (Building b : graph.consumers) {
                            if (!(b instanceof PowerGenerator.GeneratorBuild)) {
                                b.enabled = true;
                            }
                        }
                    }
                }).size(90f, 30f);
            }).colspan(2).left().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            // ---- Pestañas: productores / consumidores / baterías ----
            t.table(tabs -> {
                tabs.button("[accent]Productores[]",  () -> activeTab = 0).size(90f, 26f).pad(2f);
                tabs.button("[red]Consumidores[]",    () -> activeTab = 1).size(90f, 26f).pad(2f);
                tabs.button("[cyan]Baterías[]",       () -> activeTab = 2).size(70f, 26f).pad(2f);
            }).colspan(2).center().row();

            // Lista según pestaña activa
            if (graph != null) {
                Seq<Building> list = activeTab == 0 ? graph.producers
                                   : activeTab == 1 ? graph.consumers
                                   : graph.batteries;
                buildList(t, list);
            }

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            // ---- Acciones globales ----
            t.table(bt -> {
                bt.button("[green]Prod. ON[]", () -> {
                    if (graph != null) for (Building b : graph.producers) b.enabled = true;
                }).size(80f, 28f).pad(2f);
                bt.button("[red]Prod. OFF[]", () -> {
                    if (graph != null) for (Building b : graph.producers) b.enabled = false;
                }).size(80f, 28f).pad(2f);
                bt.button("[green]Cons. ON[]", () -> {
                    if (graph != null) for (Building b : graph.consumers) b.enabled = true;
                }).size(80f, 28f).pad(2f);
                bt.button("[red]Cons. OFF[]", () -> {
                    if (graph != null) for (Building b : graph.consumers) b.enabled = false;
                }).size(80f, 28f).pad(2f);
            }).colspan(2).center().row();
        }

        /** Dibuja la lista de bloques con toggle ON/OFF. */
        private void buildList(Table t, Seq<Building> list) {
            if (list.isEmpty()) {
                t.add("[gray]Lista vacía[]").colspan(2).center().pad(4f).row();
                return;
            }

            // Máximo 10 entradas para no desbordar la UI
            int max = Math.min(list.size, 10);
            for (int i = 0; i < max; i++) {
                Building b = list.get(i);
                String name = b.block.localizedName;
                if (name.length() > 18) name = name.substring(0, 16) + "..";

                t.add("[white]" + name + "[]").left().minWidth(150f).padLeft(4f);
                t.button(btn -> {
                    btn.clearChildren();
                    btn.add(new Label(b.enabled ? "[green]ON[]" : "[red]OFF[]")).center();
                }, () -> b.enabled = !b.enabled).size(60f, 26f).right().padRight(4f).row();
            }

            if (list.size > 10) {
                t.add("[gray]... y " + (list.size - 10) + " más[]").colspan(2).center().row();
            }
        }

        private String fmt(float value) {
            float abs = Math.abs(value);
            if (abs >= 1_000_000f) return Strings.fixed(value / 1_000_000f, 2) + "M";
            if (abs >= 1_000f)     return Strings.fixed(value / 1_000f, 1) + "k";
            return Strings.fixed(value, 0);
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
