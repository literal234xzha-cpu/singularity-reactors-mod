package singularity.blocks;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.world.*;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.meta.*;

/**
 * PowerControllerRoot - Controlador directo de generadores.
 *
 * Detecta generadores en un rango corto y permite:
 *  - Toggle ON / OFF  (building.enabled)
 *  - Cap de producción 0-100%  (productionEfficiency)
 *  - Botones globales: todos ON / todos OFF / rescan
 */
public class PowerControllerRoot extends Block {

    /** Rango en tiles donde busca generadores. */
    public int controlRange = 8;

    public PowerControllerRoot(String name) {
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
        health = 300;

        buildType = PowerControllerRootBuild::new;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, controlRange, StatUnit.blocks);
    }

    public class PowerControllerRootBuild extends Building {

        private final Seq<PowerGenerator.GeneratorBuild> generators = new Seq<>();
        /** Caps persistentes por id de building (sobreviven al rescan). */
        private final ObjectMap<Integer, Float> capById = new ObjectMap<>();

        private Table configTable;
        private int scanTimer = 0;
        private final int SCAN_INTERVAL = 60;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            scanGenerators();
            return this;
        }

        @Override
        public void updateTile() {
            scanTimer++;
            if (scanTimer >= SCAN_INTERVAL) {
                scanTimer = 0;
                scanGenerators();
            }

            for (PowerGenerator.GeneratorBuild gen : generators) {
                if (gen == null || !gen.isAdded() || !gen.enabled) continue;

                float cap = capById.get(gen.id, 1f);
                if (gen.productionEfficiency > cap) {
                    gen.productionEfficiency = Mathf.lerpDelta(gen.productionEfficiency, cap, 0.1f);
                }
            }
        }

        private void scanGenerators() {
            generators.clear();

            int tx = tileX(), ty = tileY();
            ObjectMap<Integer, Float> stillPresent = new ObjectMap<>();

            for (int dx = -controlRange; dx <= controlRange; dx++) {
                for (int dy = -controlRange; dy <= controlRange; dy++) {
                    Building b = Vars.world.build(tx + dx, ty + dy);
                    if (b == null || b.team != team || b == this) continue;
                    if (!(b instanceof PowerGenerator.GeneratorBuild)) continue;
                    PowerGenerator.GeneratorBuild gen = (PowerGenerator.GeneratorBuild) b;
                    if (generators.contains(gen)) continue;

                    generators.add(gen);
                    stillPresent.put(gen.id, capById.get(gen.id, 1f));
                }
            }

            capById.clear();
            capById.putAll(stillPresent);
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
            t.defaults().pad(3f).left();

            t.add("[accent]~ CONTROLLER ROOT[]").center().colspan(3).row();
            t.image().color(Pal.accent).height(2f).growX().colspan(3).pad(2f).row();

            t.label(() -> "[lightgray]Rango: " + controlRange + " tiles   Detectados: [accent]"
                + generators.size + "[]   Activos: [green]" + countActive() + "[]")
             .colspan(3).left().row();

            t.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            t.add("[lightgray]Generador[]").left().minWidth(120f);
            t.add("[lightgray]Estado[]").center().minWidth(70f);
            t.add("[lightgray]Cap prod.[]").center().minWidth(110f).row();
            t.image().color(Color.darkGray).height(1f).growX().colspan(3).pad(1f).row();

            if (generators.isEmpty()) {
                t.add("[gray]Sin generadores en rango[]").colspan(3).center().pad(8f).row();
            } else {
                for (PowerGenerator.GeneratorBuild gen : generators) {
                    String name = gen.block.localizedName;
                    if (name.length() > 15) name = name.substring(0, 13) + "..";
                    final int genId = gen.id;

                    t.add("[white]" + name + "[]").left().minWidth(120f);

                    t.button("", () -> gen.enabled = !gen.enabled)
                        .update(b -> b.setText(gen.enabled ? "[green]ON[]" : "[red]OFF[]"))
                        .size(70f, 32f).center();

                    t.table(st -> {
                        st.button("[lightgray]-[]", () ->
                            capById.put(genId, Mathf.clamp(capById.get(genId, 1f) - 0.10f, 0f, 1f))
                        ).size(26f, 26f);

                        st.label(() -> {
                            int pct = (int)(capById.get(genId, 1f) * 100f);
                            Color c = pct >= 75 ? Color.green : pct >= 40 ? Color.yellow : Color.red;
                            return hexColor(c) + pct + "%[]";
                        }).minWidth(38f).center();

                        st.button("[lightgray]+[]", () ->
                            capById.put(genId, Mathf.clamp(capById.get(genId, 1f) + 0.10f, 0f, 1f))
                        ).size(26f, 26f);
                    }).center().minWidth(110f).row();
                }
            }

            t.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            t.table(bt -> {
                bt.button("[green]Todos ON[]", () -> {
                    for (PowerGenerator.GeneratorBuild g : generators) g.enabled = true;
                }).size(88f, 30f).pad(3f);

                bt.button("[red]Todos OFF[]", () -> {
                    for (PowerGenerator.GeneratorBuild g : generators) g.enabled = false;
                }).size(88f, 30f).pad(3f);

                bt.button("[lightgray]R Rescan[]", () -> {
                    scanGenerators();
                    rebuildUI();
                }).size(80f, 30f).pad(3f);
            }).colspan(3).center().row();
        }

        private int countActive() {
            int n = 0;
            for (PowerGenerator.GeneratorBuild g : generators) if (g.enabled) n++;
            return n;
        }

        private String hexColor(Color c) {
            return "[#" + c.toString().substring(0, 6) + "]";
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(capById.size);
            for (ObjectMap.Entry<Integer, Float> e : capById) {
                write.i(e.key);
                write.f(e.value);
            }
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            capById.clear();
            int n = read.i();
            for (int i = 0; i < n; i++) {
                capById.put(read.i(), read.f());
            }
        }

        @Override
        public boolean shouldConsume() {
            return true;
        }
    }
}
