package singularity.blocks;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Tile;
import mindustry.world.blocks.power.ConsumeGenerator;

/**
 * SingularityReactor -- Generador de singularidad con sistema de prioridades.
 *
 * Extiende ConsumeGenerator agregando:
 *  - Campo priority (1-5): define el orden de apagado en modo emergencia.
 *    Prioridad 1 = se apaga primero, Prioridad 5 = ultimo en apagarse.
 *  - UI de configuracion con botones +/- para cambiar la prioridad.
 *  - El estado de prioridad se guarda en el save.
 */
public class SingularityReactor extends ConsumeGenerator {

    /** Prioridad por defecto al construir el reactor. */
    public int defaultPriority = 3;

    public SingularityReactor(String name) {
        super(name);
        configurable = true;
        saveConfig = true;

        // Config guarda la prioridad como Integer
        config(Integer.class, (SingularityReactorBuild build, Integer val) -> {
            build.priority = Mathf.clamp(val, 1, 5);
        });
    }

    // =========================================================================
    // BUILD
    // =========================================================================

    public class SingularityReactorBuild extends ConsumeGeneratorBuild {

        /** Prioridad de este reactor (1=baja, 5=alta). */
        public int priority;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            priority = defaultPriority;
            return this;
        }

        @Override
        public void buildConfiguration(Table table) {
            // Llamar al panel original si existe (calor, warmup, etc.)
            // No llamamos super para no duplicar UI — solo mostramos la prioridad

            table.defaults().pad(4f);

            table.add("[accent]Prioridad del Reactor[]").center().colspan(3).row();
            table.image().color(priorityColor()).height(2f).growX().colspan(3).pad(2f).row();

            table.add("[lightgray]Prioridad:[]").left();

            table.table(pt -> {
                pt.button("[lightgray]-[]", () -> {
                    if (priority > 1) { priority--; deselect(); }
                }).size(34f, 34f);

                pt.label(() -> {
                    String col = priorityColorName();
                    String stars = repeatChar('*', priority) + repeatChar('-', 5 - priority);
                    return "[" + col + "]" + priority + " " + stars + "[]";
                }).minWidth(90f).center();

                pt.button("[lightgray]+[]", () -> {
                    if (priority < 5) { priority++; deselect(); }
                }).size(34f, 34f);
            }).colspan(2).center().row();

            table.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            table.label(() -> {
                if (priority == 1) return "[red]Se apaga primero en emergencia[]";
                if (priority == 2) return "[orange]Prioridad baja[]";
                if (priority == 3) return "[yellow]Prioridad media[]";
                if (priority == 4) return "[lime]Prioridad alta[]";
                return "[green]Ultimo en apagarse[]";
            }).colspan(3).center().row();

            table.image().color(Color.gray).height(1f).growX().colspan(3).pad(2f).row();

            // Estado del reactor
            table.label(() -> {
                float pct = (int)(productionEfficiency * 100f);
                String state = enabled
                    ? (warmup > 0.9f ? "[green]Operando al " + pct + "%[]" : "[yellow]Calentando...[]")
                    : "[red]Apagado[]";
                return "[lightgray]Estado: " + state;
            }).colspan(3).center().row();
        }

        private Color priorityColor() {
            if (priority <= 1) return Color.red;
            if (priority == 2) return Color.orange;
            if (priority == 3) return Color.yellow;
            if (priority == 4) return Color.lime;
            return Color.green;
        }

        private String priorityColorName() {
            if (priority <= 1) return "red";
            if (priority == 2) return "orange";
            if (priority == 3) return "yellow";
            if (priority == 4) return "lime";
            return "green";
        }

        private String repeatChar(char c, int times) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < times; i++) sb.append(c);
            return sb.toString();
        }

        @Override
        public Integer config() {
            return priority;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(priority);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            priority = read.i();
            if (priority < 1 || priority > 5) priority = defaultPriority;
        }
    }
}
