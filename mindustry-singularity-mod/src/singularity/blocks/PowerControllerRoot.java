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
import mindustry.type.Category;
import mindustry.world.*;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.meta.*;

/**
 * PowerControllerRoot — Controlador directo de generadores.
 *
 * Detecta generadores en un rango corto (8 tiles por defecto) y permite:
 *  - Toggle ON / OFF  (building.enabled)
 *  - Cap de producción 0–100%  (productionEfficiency)
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

    // =========================================================================
    // BUILD
    // =========================================================================

    public class PowerControllerRootBuild extends Building {

        private final Seq<PowerGenerator.GeneratorBuild> generators = new Seq<>();
        private final Seq<Float> caps = new Seq<>();

        private int scanTimer = 0;
        private static final int SCAN_INTERVAL = 60;

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

            // Aplicar cap de eficiencia a cada generador activo
            for (int i = 0; i < generators.size; i++) {
                PowerGenerator.GeneratorBuild gen = generators.get(i);
                if (gen == null || !gen.isAdded()) continue;
                if (!gen.enabled) continue;

                float cap = caps.get(i);
                if (gen.productionEfficiency > cap) {
                    gen.productionEfficiency = Mathf.lerpDelta(gen.productionEfficiency, cap, 0.1f);
                }
            }
        }

        private void scanGenerators() {
            generators.clear();
            caps.clear();

            int tx = tileX(), ty = tileY();

            for (int dx = -controlRange; dx <= controlRange; dx++) {
                for (int dy = -controlRange; dy <= controlRange; dy++) {
                    Building b = Vars.world.build(tx + dx, ty + dy);
                    if (b == null) continue;
                    if (b.team != team) continue;
                    if (b == this) continue;
                    if (!(b instanceof PowerGenerator.GeneratorBuild gen)) continue;
                    if (generators.contains(gen)) continue;

                    generators.add(gen);
                    caps.add(1.0f);
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
            t.defaults().pad(3f).left();

            // Título
            t.add("[accent]⚡ CONTROLLER ROOT[]").center().colspan(3).row();
            t.image().color(Pal.accent).height(2f).growX().colspan(3).pad(2f).row();

            // Info
            t.add("[lightgray]Rango: " + controlRange + " tiles   Detectados: [accent]"
                + generators.size + "[]   Activos: [green]" + countActive() + "[]")
             .colspan(3).left().row();

            t.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            // Encabezados
            t.add("[lightgray]Generador[]").left().minWidth(120f);
            t.add("[lightgray]Estado[]").center().minWidth(70f);
            t.add("[lightgray]Cap prod.[]").center().minWidth(110f).row();
            t.image().color(Color.darkGray).height(1f).growX().colspan(3).pad(1f).row();

            if (generators.isEmpty()) {
                t.add("[gray]Sin generadores en rango[]").colspan(3).center().pad(8f).row();
            } else {
                for (int i = 0; i < generators.size; i++) {
                    final int idx = i;
                    PowerGenerator.GeneratorBuild gen = generators.get(idx);
                    String name = gen.block.localizedName;
                    if (name.length() > 15) name = name.substring(0, 13) + "..";

                    // Nombre
                    t.add("[white]" + name + "[]").left().minWidth(120f);

                    // Toggle ON/OFF — usamos Cons<Button> para texto dinámico
                    t.button(btn -> {
                        btn.clearChildren();
                        Label lbl = new Label(gen.enabled ? "[green]ON[]" : "[red]OFF[]");
                        btn.add(lbl).center();
                    }, () -> {
                        gen.enabled = !gen.enabled;
                    }).size(70f, 32f).center();

                    // Cap con -10 / valor / +10
                    t.table(st -> {
                        st.button("[lightgray]-[]", () -> {
                            caps.set(idx, Mathf.clamp(caps.get(idx) - 0.10f, 0f, 1f));
                        }).size(26f, 26f);

                        st.label(() -> {
                            int pct = (int)(caps.get(idx) * 100f);
                            Color c = pct >= 75 ? Color.green : pct >= 40 ? Color.yellow : Color.red;
                            return hexColor(c) + pct + "%[]";
                        }).minWidth(38f).center();

                        st.button("[lightgray]+[]", () -> {
                            caps.set(idx, Mathf.clamp(caps.get(idx) + 0.10f, 0f, 1f));
                        }).size(26f, 26f);
                    }).center().minWidth(110f).row();
                }
            }

            t.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            // Botones globales
            t.table(bt -> {
                bt.button("[green]Todos ON[]", () -> {
                    for (PowerGenerator.GeneratorBuild g : generators) g.enabled = true;
                }).size(88f, 30f).pad(3f);

                bt.button("[red]Todos OFF[]", () -> {
                    for (PowerGenerator.GeneratorBuild g : generators) g.enabled = false;
                }).size(88f, 30f).pad(3f);

                bt.button("[lightgray]↺ Rescan[]", this::scanGenerators)
                  .size(80f, 30f).pad(3f);
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
        public boolean shouldConsume() {
            return true;
        }
    }
}
