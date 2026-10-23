package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import singularity.blocks.gpu.MicroGPU;

/**
 * Función 26: Auto SCRAM
 *
 * Activa el SCRAM del Delta Reactor automáticamente si la temperatura supera el umbral crítico.
 *
 * Disponible desde: HiperGPU (tier 2)
 */
public class AutoScram extends GPUFunction {

    private String title = "🚨 Auto SCRAM";
    private float  timer    = 0f;
    private float  interval = 10f;

    private float tempThreshold = 0.85f; // 85% de capacidad
    private boolean armed = true;

    public AutoScram() {
        super("Auto SCRAM", "Activa el SCRAM del Delta Reactor automáticamente si la temperatura supera el umbral crítico.", "🚨", 2);
    }

    @Override
    public void update(MicroGPU.MicroGPUBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append("[lightgray]Umbral: ").append((int)(tempThreshold * 100)).append("%[]\n");

        boolean scramTriggered = false;

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;

            // Aproximar temperatura usando el ratio de consumo/producción de energía
            float heat = getHeat(b);
            if (heat >= 0) {
                String status = heat >= tempThreshold ? "[red]⚠ SCRAM![]" : "[green]OK[]";
                sb.append("[white]").append(link.name).append(": ")
                  .append((int)(heat * 100)).append("% ").append(status).append("\n");

                if (armed && heat >= tempThreshold) {
                    // Apagar el bloque
                    b.enabled = false;
                    scramTriggered = true;
                }
            }
        }

        if (scramTriggered) {
            sb.append("\n[red]>>> SCRAM ACTIVADO <<<[]\n");
        }

        if (build.links.isEmpty()) {
            sb.append("[lightgray]Sin bloques conectados[]");
        }

        printToDisplays(build, sb.toString());
    }

    /** Aproxima la temperatura del bloque leyendo su graph de energía */
    private float getHeat(Building b) {
        try {
            if (b.power != null && b.power.graph != null) {
                float prod = b.power.graph.getPowerProduced();
                float need = b.power.graph.getPowerNeeded();
                if (need > 0) return Math.min(1f, prod / need);
                return b.power.graph.getBatteryStored() > 0 ? 0.5f : -1f;
            }
        } catch (Exception ignored) {}
        return -1f;
    }

    @Override
    public void buildConfig(Table table, MicroGPU.MicroGPUBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]🚨 Auto SCRAM[]").row();
        table.add("[lightgray]Activa el SCRAM del Delta Reactor automáticamente si la temperatura supera el umbral crítico.[]").width(180f).wrap(true).row();
        table.row();

        table.add("[white]Umbral de temperatura (%):[]").row();
        table.slider(50f, 99f, 1f, tempThreshold * 100f, v -> tempThreshold = v / 100f).width(160f).row();
        table.label(() -> "[white]" + (int)(tempThreshold * 100) + "%[]").row();
        table.row();

        table.add("[white]Verificar cada (ticks):[]").row();
        table.slider(1f, 60f, 1f, interval, v -> interval = v).width(160f).row();
        table.row();

        table.check("[white]SCRAM armado[]", armed, v -> armed = v).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(24).row();
    }

    @Override
    public String toCode() {
        return "# Auto SCRAM - generado por HiperGPU\n" +
               "# Monitorea temperatura y activa SCRAM\n" +
               "sensor temp reactor1 @heat\n" +
               "op greaterThan alarm temp 0.85\n" +
               "control enabled reactor1 0 0 0 0\n" +
               "printflush display1\n";
    }
}
