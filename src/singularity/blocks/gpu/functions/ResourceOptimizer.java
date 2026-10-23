package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 31: Resource Optimizer
 *
 * Balancea producción de items entre fábricas según demanda y almacenamiento.
 *
 * Disponible desde: HiperGPU (tier 2)
 */
public class ResourceOptimizer extends GPUFunction {

    private String title = "♻ Resource Optimizer";
    private float  timer    = 0f;
    private float  interval = 60f;

    private float maxFill = 0.9f;
    private boolean autoBalance = true;

    public ResourceOptimizer() {
        super("Resource Optimizer", "Balancea producción de items entre fábricas según demanda y almacenamiento.", "♻", 2);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        int factoriesOn = 0;
        int factoriesTotal = 0;
        int storageFull = 0;
        int storageTotal = 0;
        int totalItems = 0;

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;

            factoriesTotal++;
            if (b.enabled) factoriesOn++;

            if (b.items != null) {
                totalItems += b.items.total();
            }
        }

        // Si el almacenamiento total es alto, pausar fábricas
        if (autoBalance) {
            // Aproximar: si hay muchos items acumulados, pausar
            float avgItems = factoriesTotal > 0 ? (float) totalItems / factoriesTotal : 0;
            boolean shouldPause = avgItems > 500; // umbral configurable

            for (int i = 0; i < build.links.size; i++) {
                LogicLink link = build.links.get(i);
                Building b = link.lastBuild;
                if (b == null) continue;

                if (shouldPause) {
                    b.enabled = false;
                    factoriesOn--;
                } else {
                    b.enabled = true;
                }
            }
        }

        sb.append("[white]Fábricas: ").append(factoriesOn).append("/").append(factoriesTotal).append("[]\n");
        sb.append("[orange]Items totales: ").append(totalItems).append("[]\n");

        if (factoriesTotal > 0) {
            float ratio = factoriesTotal > 0 ? (float) factoriesOn / factoriesTotal : 0;
            String status = ratio > 0.8f ? "[green]Produciendo" : ratio > 0.3f ? "[yellow]Parcial" : "[red]Pausado";
            sb.append(status).append("[]\n");
        }

        if (build.links.isEmpty()) {
            sb.append("[lightgray]Sin bloques conectados[]");
        }

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]♻ Resource Optimizer[]").row();
        table.add("[lightgray]Balancea producción de items entre fábricas según demanda y almacenamiento.[]").width(180f).wrap(true).row();
        table.row();

        table.add("[white]Máximo llenado de almacén (%):[]").row();
        table.slider(50f, 100f, 5f, maxFill * 100f, v -> maxFill = v / 100f).width(160f).row();
        table.label(() -> "[white]" + (int)(maxFill * 100) + "%[]").row();
        table.row();

        table.check("[white]Balance automático[]", autoBalance, v -> autoBalance = v).row();
        table.row();

        table.add("[white]Verificar cada (ticks):[]").row();
        table.slider(10f, 300f, 10f, interval, v -> interval = v).width(160f).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(24).row();
    }

    @Override
    public String toCode() {
        return "# Resource Optimizer - generado por HiperGPU\n" +
               "# Balancea producción según almacenamiento\n" +
               "sensor items vault1 @totalItems\n" +
               "op greaterThan full items 900\n" +
               "control enabled factory1 0 0 0 0\n" +
               "printflush display1\n";
    }
}
