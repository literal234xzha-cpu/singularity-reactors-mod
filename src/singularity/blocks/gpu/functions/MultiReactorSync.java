package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import singularity.blocks.gpu.MicroGPU;

/**
 * Función 32: Multi Reactor Sync
 *
 * Sincroniza múltiples Delta Reactors para temperatura uniforme y máxima eficiencia.
 *
 * Disponible desde: HiperGPU (tier 2)
 */
public class MultiReactorSync extends GPUFunction {

    private String title = "🔄 Multi Reactor Sync";
    private float  timer    = 0f;
    private float  interval = 10f;

    private float targetTemp = 0.65f;
    private float tolerance  = 0.1f;
    private boolean autoSync = true;

    public MultiReactorSync() {
        super("Multi Reactor Sync", "Sincroniza múltiples Delta Reactors para temperatura uniforme y máxima eficiencia.", "🔄", 2);
    }

    @Override
    public void update(MicroGPU.MicroGPUBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        float sumTemp = 0f;
        int reactorCount = 0;
        float minTemp = 1f;
        float maxTempR = 0f;

        // Primera pasada: recopilar temperaturas
        int linkCount = build.links.size;
        float[] temps = new float[linkCount];
        for (int i = 0; i < linkCount; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;

            float temp = getTemp(b);
            if (temp >= 0) {
                temps[reactorCount] = temp;
                sumTemp += temp;
                minTemp = Math.min(minTemp, temp);
                maxTempR = Math.max(maxTempR, temp);
                reactorCount++;
            }
        }

        if (reactorCount == 0) {
            sb.append("[lightgray]Sin reactores conectados[]");
            printToDisplays(build, sb.toString());
            return;
        }

        float avgTemp = sumTemp / reactorCount;
        float spread = maxTempR - minTemp;

        sb.append("[white]Reactores: ").append(reactorCount).append("[]\n");
        String tempStr = avgTemp > targetTemp + tolerance ? "[red]" : avgTemp < targetTemp - tolerance ? "[yellow]" : "[green]";
        sb.append(tempStr).append("Promedio: ").append((int)(avgTemp * 100)).append("%[]\n");
        sb.append("[white]Spread: ").append((int)(spread * 100)).append("%[]\n");

        // Segunda pasada: sincronizar
        if (autoSync) {
            int idx = 0;
            for (int i = 0; i < linkCount; i++) {
                LogicLink link = build.links.get(i);
                Building b = link.lastBuild;
                if (b == null) continue;
                float temp = getTemp(b);
                if (temp < 0) continue;

                idx++;
                if (temp > targetTemp + tolerance) {
                    b.enabled = false;
                    sb.append("[red]").append(idx).append(": OFF (hot)[]\n");
                } else if (temp < targetTemp - tolerance) {
                    b.enabled = true;
                    sb.append("[green]").append(idx).append(": ON (cold)[]\n");
                } else {
                    sb.append("[yellow]").append(idx).append(": synced[]\n");
                }
            }
        } else {
            for (int i = 0; i < reactorCount; i++) {
                sb.append("[white]Reactor ").append(i + 1).append(": ")
                  .append((int)(temps[i] * 100)).append("%[]\n");
            }
        }

        printToDisplays(build, sb.toString());
    }

    private float getTemp(Building b) {
        try {
            if (b.power != null && b.power.graph != null) {
                float prod = b.power.graph.getPowerProduced();
                float need = b.power.graph.getPowerNeeded();
                if (need > 0) return Math.min(1f, prod / need);
            }
        } catch (Exception ignored) {}
        return -1f;
    }

    @Override
    public void buildConfig(Table table, MicroGPU.MicroGPUBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]🔄 Multi Reactor Sync[]").row();
        table.add("[lightgray]Sincroniza múltiples Delta Reactors para temperatura uniforme y máxima eficiencia.[]").width(180f).wrap(true).row();
        table.row();

        table.add("[white]Temperatura objetivo (%):[]").row();
        table.slider(20f, 95f, 1f, targetTemp * 100f, v -> targetTemp = v / 100f).width(160f).row();
        table.label(() -> "[white]" + (int)(targetTemp * 100) + "%[]").row();
        table.row();

        table.add("[white]Tolerancia (%):[]").row();
        table.slider(1f, 30f, 1f, tolerance * 100f, v -> tolerance = v / 100f).width(160f).row();
        table.label(() -> "[white]±" + (int)(tolerance * 100) + "%[]").row();
        table.row();

        table.check("[white]Sincronización automática[]", autoSync, v -> autoSync = v).row();
        table.row();

        table.add("[white]Verificar cada (ticks):[]").row();
        table.slider(1f, 60f, 1f, interval, v -> interval = v).width(160f).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(24).row();
    }

    @Override
    public String toCode() {
        return "# Multi Reactor Sync - generado por HiperGPU\n" +
               "# Sincroniza reactores\n" +
               "sensor t1 reactor1 @heat\n" +
               "sensor t2 reactor2 @heat\n" +
               "op avg t1 t2\n" +
               "control enabled reactor1 1 0 0 0\n" +
               "printflush display1\n";
    }
}
