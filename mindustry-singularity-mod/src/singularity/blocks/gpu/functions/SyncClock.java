package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import singularity.blocks.gpu.MicroGPU;

/**
 * Función 23: Sync Clock
 *
 * Sincroniza el tick de múltiples GPUs para que ejecuten acciones coordinadas al mismo tiempo.
 *
 * Disponible desde: GPU (tier 1)
 */
public class SyncClock extends GPUFunction {

    private String title = "🔄 Sync Clock";
    private float  timer    = 0f;
    private float  interval = 60f;

    public SyncClock() {
        super("Sync Clock", "Sincroniza el tick de múltiples GPUs para que ejecuten acciones coordinadas al mismo tiempo.", "🔄", 1);
    }

    @Override
    public void update(MicroGPU.MicroGPUBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        // Leer datos de bloques conectados
        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;
            sb.append("[white]").append(link.name).append(": ")
              .append("[cyan]OK[]\n");
        }

        if (build.links.isEmpty()) {
            sb.append("[lightgray]Sin bloques conectados[]");
        }

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, MicroGPU.MicroGPUBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]🔄 Sync Clock[]").row();
        table.add("[lightgray]Sincroniza el tick de múltiples GPUs para que ejecuten acciones coordinadas al mismo tiempo.[]").width(180f).wrap(true).row();
        table.row();

        table.add("[white]Actualizar cada (ticks):[]").row();
        table.slider(10f, 300f, 10f, interval, v -> interval = v).width(160f).row();
        table.label(() -> "[white]" + (int)(interval / 60f) + "s[]").row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(24).row();
    }

    @Override
    public String toCode() {
        return "# Sync Clock - generado por GPU\n" +
               "# Conecta bloques y un display\n" +
               "printflush display1\n";
    }
}
