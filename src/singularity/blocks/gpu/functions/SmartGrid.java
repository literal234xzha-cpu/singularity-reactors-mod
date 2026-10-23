package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 27: Smart Grid
 *
 * Gestión completa de reactores: enciende/apaga según demanda en tiempo real.
 *
 * Disponible desde: HiperGPU (tier 2)
 */
public class SmartGrid extends GPUFunction {

    private String title = "⚡ Smart Grid";
    private float  timer    = 0f;
    private float  interval = 30f;

    private float targetCapacity = 0.8f;
    private boolean autoControl = true;

    public SmartGrid() {
        super("Smart Grid", "Gestión completa de reactores: enciende/apaga según demanda en tiempo real.", "⚡", 2);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        float totalProd = 0f, totalNeed = 0f, totalStored = 0f, totalCap = 0f;

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;

            if (b.power != null && b.power.graph != null) {
                totalProd = b.power.graph.getPowerProduced() * 60f;
                totalNeed = b.power.graph.getPowerNeeded() * 60f;
                totalStored = b.power.graph.getBatteryStored();
                totalCap = b.power.graph.getBatteryCapacity();
                break; // el graph es compartido, uno basta
            }
        }

        sb.append("[green]▲ Prod: ").append(formatNum(totalProd)).append("/s[]\n");
        sb.append("[red]▼ Demanda: ").append(formatNum(totalNeed)).append("/s[]\n");

        float ratio = totalNeed > 0 ? totalProd / totalNeed : 1f;
        String ratioStr = ratio >= 1 ? "[green]" : "[red]";
        sb.append(ratioStr).append("Ratio: ").append(String.format("%.0f%%", ratio * 100)).append("[]\n");

        if (totalCap > 0) {
            float batPct = totalStored / totalCap * 100f;
            sb.append("[yellow]Bat: ").append(String.format("%.0f%%", batPct)).append("[]\n");
        }

        // Control automático: apagar/encender bloques según ratio
        if (autoControl) {
            for (int i = 0; i < build.links.size; i++) {
                LogicLink link = build.links.get(i);
                Building b = link.lastBuild;
                if (b == null) continue;

                if (ratio < targetCapacity) {
                    // Energía insuficiente: asegurar que generadores estén encendidos
                    b.enabled = true;
                } else if (ratio > targetCapacity * 1.5f) {
                    // Energía sobrante: apagar generadores redundantes
                    if (b.power != null && b.power.graph != null && b.power.graph.getPowerProduced() > 0) {
                        b.enabled = false;
                    }
                }
            }
        }

        if (build.links.isEmpty()) {
            sb.append("[lightgray]Sin bloques conectados[]");
        }

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]⚡ Smart Grid[]").row();
        table.add("[lightgray]Gestión completa de reactores: enciende/apaga según demanda en tiempo real.[]").width(180f).wrap(true).row();
        table.row();

        table.add("[white]Capacidad objetivo (%):[]").row();
        table.slider(50f, 100f, 5f, targetCapacity * 100f, v -> targetCapacity = v / 100f).width(160f).row();
        table.label(() -> "[white]" + (int)(targetCapacity * 100) + "%[]").row();
        table.row();

        table.add("[white]Verificar cada (ticks):[]").row();
        table.slider(5f, 120f, 5f, interval, v -> interval = v).width(160f).row();
        table.row();

        table.check("[white]Control automático[]", autoControl, v -> autoControl = v).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(24).row();
    }

    @Override
    public String toCode() {
        return "# Smart Grid - generado por HiperGPU\n" +
               "# Gestión automática de reactores\n" +
               "sensor power power1 @stored\n" +
               "op lessThan need power 0.8\n" +
               "control enabled reactor1 1 0 0 0\n" +
               "printflush display1\n";
    }
}
