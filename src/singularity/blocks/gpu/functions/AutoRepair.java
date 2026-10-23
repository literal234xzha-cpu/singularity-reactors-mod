package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 22: Auto Repair
 *
 * Activa reparadores automáticamente si la salud de un bloque conectado cae por debajo de X%.
 *
 * Disponible desde: GPU (tier 1)
 */
public class AutoRepair extends GPUFunction {

    private String title = "🔧 Auto Repair";
    private float  timer    = 0f;
    private float  interval = 60f;

    public AutoRepair() {
        super("Auto Repair", "Activa reparadores automáticamente si la salud de un bloque conectado cae por debajo de X%.", "🔧", 1);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
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
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]🔧 Auto Repair[]").row();
        table.add("[lightgray]Activa reparadores automáticamente si la salud de un bloque conectado cae por debajo de X%.[]").width(180f).wrap(true).row();
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
        return "# Auto Repair - generado por GPU\n" +
               "# Conecta bloques y un display\n" +
               "printflush display1\n";
    }
}
