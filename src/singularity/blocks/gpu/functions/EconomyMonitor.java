package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import singularity.blocks.gpu.MicroGPU;

/**
 * Función 33: Economy Monitor
 *
 * Ratio de producción y consumo de recursos por minuto con historial.
 *
 * Disponible desde: HiperGPU (tier 2)
 */
public class EconomyMonitor extends GPUFunction {

    private String title = "📈 Economy Monitor";
    private float  timer    = 0f;
    private float  interval = 60f;

    private int historySize = 10;
    private float[] productionHistory = new float[20];
    private float[] consumptionHistory = new float[20];
    private int historyIndex = 0;
    private int historyCount = 0;

    private float prevProduction = 0f;
    private float prevConsumption = 0f;

    public EconomyMonitor() {
        super("Economy Monitor", "Ratio de producción y consumo de recursos por minuto con historial.", "📈", 2);
    }

    @Override
    public void update(MicroGPU.MicroGPUBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        float currentProduction = 0f;
        float currentConsumption = 0f;
        int totalItems = 0;
        int factoryCount = 0;

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;

            factoryCount++;

            if (b.power != null && b.power.graph != null) {
                currentProduction = b.power.graph.getPowerProduced() * 60f;
                currentConsumption = b.power.graph.getPowerNeeded() * 60f;
            }

            if (b.items != null) {
                totalItems += b.items.total();
            }
        }

        float productionDelta = currentProduction - prevProduction;
        float consumptionDelta = currentConsumption - prevConsumption;

        // Guardar en historial
        if (historyCount < historySize) historyCount++;
        productionHistory[historyIndex] = currentProduction;
        consumptionHistory[historyIndex] = currentConsumption;
        historyIndex = (historyIndex + 1) % historySize;

        // Promedios
        float avgProd = 0f, avgCons = 0f;
        for (int h = 0; h < historyCount; h++) {
            avgProd += productionHistory[h];
            avgCons += consumptionHistory[h];
        }
        if (historyCount > 0) {
            avgProd /= historyCount;
            avgCons /= historyCount;
        }

        sb.append("[green]Prod: ").append(formatNum(currentProduction)).append("/s[]\n");
        sb.append("[orange]Cons: ").append(formatNum(currentConsumption)).append("/s[]\n");
        sb.append("[white]Δ Prod: ").append(productionDelta >= 0 ? "[green]+" : "[red]").append(formatNum(productionDelta)).append("[]\n");
        sb.append("[white]Items: ").append(totalItems).append("[]\n");
        sb.append("[accent]Bloques: ").append(factoryCount).append("[]\n");

        // Tendencia
        if (historyCount >= 2) {
            int oldestIdx = (historyIndex - historyCount + historySize) % historySize;
            float trend = currentProduction - productionHistory[oldestIdx];
            if (trend > 0) sb.append("[green]↗ Tendencia al alza[]\n");
            else if (trend < 0) sb.append("[red]↘ Tendencia a la baja[]\n");
            else sb.append("[yellow]→ Estable[]\n");
        }

        prevProduction = currentProduction;
        prevConsumption = currentConsumption;

        if (build.links.isEmpty()) {
            sb.append("[lightgray]Sin bloques conectados[]");
        }

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, MicroGPU.MicroGPUBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]📈 Economy Monitor[]").row();
        table.add("[lightgray]Ratio de producción y consumo de recursos por minuto con historial.[]").width(180f).wrap(true).row();
        table.row();

        table.add("[white]Tamaño del historial:[]").row();
        table.slider(2f, 20f, 1f, historySize, v -> {
            historySize = (int) v;
            productionHistory = new float[historySize];
            consumptionHistory = new float[historySize];
            historyCount = 0;
            historyIndex = 0;
        }).width(160f).row();
        table.label(() -> "[white]" + historySize + " muestras[]").row();
        table.row();

        table.add("[white]Actualizar cada (ticks):[]").row();
        table.slider(10f, 300f, 10f, interval, v -> interval = v).width(160f).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(24).row();
    }

    @Override
    public String toCode() {
        return "# Economy Monitor - generado por HiperGPU\n" +
               "# Monitoreo de economía con historial\n" +
               "sensor prod factory1 @production\n" +
               "sensor cons factory1 @consumption\n" +
               "print \"Prod/min: \" prod\n" +
               "printflush display1\n";
    }
}
