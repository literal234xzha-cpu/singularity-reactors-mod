package singularity.blocks;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

/**
 * PowerControllerNet — Gestor de red eléctrica completa.
 *
 * Accede a la red via PowerGraph. Puede:
 *  - Ver producción / consumo / baterías
 *  - Toggle ON/OFF productores, consumidores y baterías
 *  - Cap global de producción
 *  - Modo emergencia: apaga consumidores no esenciales
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

    public class PowerControllerNetBuild extends Building {

        public float globalProductionCap = 1.0f;
        public boolean emergencyMode = false;
        /** Buildings disabled by emergency mode; restored on exit. */
        private final Seq<Building> emergencyDisabled = new Seq<>();

        /** 0=productores, 1=consumidores, 2=baterías */
        public int activeTab = 0;

        private Table configTable;

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

            for (Building b : graph.producers) {
                if (!(b instanceof PowerGenerator.GeneratorBuild gen)) continue;
                if (!gen.enabled) continue;
                if (gen.productionEfficiency > globalProductionCap) {
                    gen.productionEfficiency = Mathf.lerpDelta(
                        gen.productionEfficiency, globalProductionCap, 0.05f);
                }
            }

            if (emergencyMode) {
                for (Building b : graph.consumers) {
                    if (b instanceof PowerGenerator.GeneratorBuild) continue;
                    if (b.enabled) {
                        b.enabled = false;
                        if (!emergencyDisabled.contains(b)) {
                            emergencyDisabled.add(b);
                        }
                    }
                }
            }
        }

        private void setEmergency(boolean on) {
            PowerGraph graph = cachedGraph();
            if (on) {
                emergencyMode = true;
                emergencyDisabled.clear();
                if (graph != null) {
                    for (Building b : graph.consumers) {
                        if (b instanceof PowerGenerator.GeneratorBuild) continue;
                        if (b.enabled) {
                            b.enabled = false;
                            emergencyDisabled.add(b);
                        }
                    }
                }
            } else {
                emergencyMode = false;
                for (Building b : emergencyDisabled) {
                    if (b != null && b.isAdded()) b.enabled = true;
                }
                emergencyDisabled.clear();
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
            PowerGraph graph = cachedGraph();

            t.defaults().pad(3f);

            t.add("[accent]⚡ POWER CONTROLLER NET[]").center().colspan(2).row();
            t.image().color(Pal.accent).height(2f).growX().colspan(2).pad(2f).row();

            if (graph != null) {
                t.table(st -> {
                    st.defaults().pad(2f).left();
                    st.add("[lightgray]Producción:[]").left();
                    st.label(() -> {
                        PowerGraph g = cachedGraph();
                        float prod = g == null ? 0 : g.getPowerProduced() * 60f;
                        return "[green]" + fmt(prod) + "/s[]";
                    }).right().row();
                    st.add("[lightgray]Consumo:[]").left();
                    st.label(() -> {
                        PowerGraph g = cachedGraph();
                        float cons = g == null ? 0 : g.getPowerNeeded() * 60f;
                        return "[red]" + fmt(cons) + "/s[]";
                    }).right().row();
                    st.add("[lightgray]Balance:[]").left();
                    st.label(() -> {
                        PowerGraph g = cachedGraph();
                        if (g == null) return "[gray]—[]";
                        float bal = (g.getPowerProduced() - g.getPowerNeeded()) * 60f;
                        return (bal >= 0 ? "[green]+" : "[red]") + fmt(bal) + "/s[]";
                    }).right().row();
                    st.add("[lightgray]Satisfacción:[]").left();
                    st.label(() -> {
                        PowerGraph g = cachedGraph();
                        if (g == null) return "[gray]—[]";
                        float sat = g.getSatisfaction();
                        Color sc = sat >= 0.99f ? Color.green : sat >= 0.5f ? Color.yellow : Color.red;
                        return hexColor(sc) + (int)(sat * 100f) + "%[]";
                    }).right().row();
                    st.add("[lightgray]Baterías:[]").left();
                    st.label(() -> {
                        PowerGraph g = cachedGraph();
                        if (g == null) return "[gray]—[]";
                        return "[cyan]" + fmt(g.getBatteryStored()) + " / " + fmt(g.getTotalBatteryCapacity()) + "[]";
                    }).right().row();
                    st.add("[lightgray]Productores:[]").left();
                    st.label(() -> {
                        PowerGraph g = cachedGraph();
                        return "[accent]" + (g == null ? 0 : g.producers.size) + "[]";
                    }).right().row();
                    st.add("[lightgray]Consumidores:[]").left();
                    st.label(() -> {
                        PowerGraph g = cachedGraph();
                        return "[accent]" + (g == null ? 0 : g.consumers.size) + "[]";
                    }).right().row();
                }).colspan(2).growX().row();
            } else {
                t.add("[gray]Sin red eléctrica conectada[]").colspan(2).center().row();
            }

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.add("[lightgray]Cap global producción:[]").left();
            t.table(ct -> {
                ct.button("[lightgray]-10%[]", () ->
                    globalProductionCap = Mathf.clamp(globalProductionCap - 0.10f, 0f, 1f)
                ).size(52f, 28f);
                ct.label(() -> {
                    int pct = (int)(globalProductionCap * 100f);
                    Color c = pct >= 75 ? Color.green : pct >= 40 ? Color.yellow : Color.red;
                    return hexColor(c) + pct + "%[]";
                }).minWidth(40f).center();
                ct.button("[lightgray]+10%[]", () ->
                    globalProductionCap = Mathf.clamp(globalProductionCap + 0.10f, 0f, 1f)
                ).size(52f, 28f);
                ct.button("[white]100%[]", () -> globalProductionCap = 1.0f).size(44f, 28f);
            }).right().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.table(em -> {
                em.add("[lightgray]Modo emergencia:[]").left().padRight(8f);
                em.button("", () -> {
                    setEmergency(!emergencyMode);
                    rebuildUI();
                }).update(b -> b.setText(emergencyMode ? "[red]ACTIVO[]" : "[gray]INACTIVO[]"))
                  .size(90f, 30f);
            }).colspan(2).left().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.table(tabs -> {
                tabs.button("[accent]Productores[]", () -> { activeTab = 0; rebuildUI(); }).size(90f, 26f).pad(2f);
                tabs.button("[red]Consumidores[]", () -> { activeTab = 1; rebuildUI(); }).size(90f, 26f).pad(2f);
                tabs.button("[cyan]Baterías[]", () -> { activeTab = 2; rebuildUI(); }).size(70f, 26f).pad(2f);
            }).colspan(2).center().row();

            if (graph != null) {
                Seq<Building> list = activeTab == 0 ? graph.producers
                                   : activeTab == 1 ? graph.consumers
                                   : graph.batteries;
                buildList(t, list);
            }

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.table(bt -> {
                bt.button("[green]Prod. ON[]", () -> {
                    PowerGraph g = cachedGraph();
                    if (g != null) for (Building b : g.producers) b.enabled = true;
                }).size(80f, 28f).pad(2f);
                bt.button("[red]Prod. OFF[]", () -> {
                    PowerGraph g = cachedGraph();
                    if (g != null) for (Building b : g.producers) b.enabled = false;
                }).size(80f, 28f).pad(2f);
                bt.button("[green]Cons. ON[]", () -> {
                    PowerGraph g = cachedGraph();
                    if (g != null) for (Building b : g.consumers) b.enabled = true;
                }).size(80f, 28f).pad(2f);
                bt.button("[red]Cons. OFF[]", () -> {
                    PowerGraph g = cachedGraph();
                    if (g != null) for (Building b : g.consumers) b.enabled = false;
                }).size(80f, 28f).pad(2f);
            }).colspan(2).center().row();
        }

        private void buildList(Table t, Seq<Building> list) {
            if (list.isEmpty()) {
                t.add("[gray]Lista vacía[]").colspan(2).center().pad(4f).row();
                return;
            }

            int max = Math.min(list.size, 10);
            for (int i = 0; i < max; i++) {
                Building b = list.get(i);
                String name = b.block.localizedName;
                if (name.length() > 18) name = name.substring(0, 16) + "..";

                t.add("[white]" + name + "[]").left().minWidth(150f).padLeft(4f);
                t.button("", () -> b.enabled = !b.enabled)
                    .update(btn -> btn.setText(b.enabled ? "[green]ON[]" : "[red]OFF[]"))
                    .size(60f, 26f).right().padRight(4f).row();
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
        public void write(Writes write) {
            super.write(write);
            write.f(globalProductionCap);
            write.bool(emergencyMode);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            globalProductionCap = read.f();
            emergencyMode = read.bool();
        }

        @Override
        public boolean shouldConsume() {
            return true;
        }
    }
}
