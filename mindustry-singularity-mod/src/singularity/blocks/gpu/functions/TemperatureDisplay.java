package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.delta.DeltaReactor;
import singularity.blocks.gpu.GPUFunction;
import singularity.blocks.gpu.MicroGPU;

/**
 * Función 10: Temperature Display
 *
 * Monitorea la temperatura del Delta Reactor conectado.
 * Muestra temperatura actual, zona de operación y alerta
 * si se acerca a la zona crítica o meltdown.
 *
 * Zonas:
 *   < 200°C  → Frío (no genera)
 *   200-800°C → Óptimo (verde)
 *   800-1000°C → Advertencia (amarillo)
 *   > 1000°C  → CRÍTICO (rojo, riesgo meltdown)
 */
public class TemperatureDisplay extends GPUFunction {

    private boolean showBar      = true;
    private boolean showZone     = true;
    private boolean alertCrit    = true;
    private String  title        = "🌡 TEMP";

    private float timer    = 0f;
    private float interval = 30f;

    public TemperatureDisplay() {
        super("Temperature Display",
              "Temperatura del Delta Reactor con zonas de seguridad",
              "🌡", 0);
    }

    @Override
    public void update(MicroGPU.MicroGPUBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        boolean found = false;

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (!(b instanceof DeltaReactor.DeltaReactorBuild)) continue;

            DeltaReactor.DeltaReactorBuild reactor = (DeltaReactor.DeltaReactorBuild) b;
            float temp = reactor.temperature;
            found = true;

            // Determinar zona
            String zone, color;
            if (temp < 200f) {
                zone  = "FRÍO";
                color = "cyan";
            } else if (temp < 800f) {
                zone  = "ÓPTIMO";
                color = "green";
            } else if (temp < 1000f) {
                zone  = "ADVERTENCIA";
                color = "yellow";
            } else {
                zone  = "CRÍTICO";
                color = "scarlet";
            }

            sb.append("[").append(color).append("]").append(title).append("[]\n");
            sb.append("[").append(color).append("]")
              .append(String.format("%.0f°C", temp)).append("[]\n");

            if (showZone)
                sb.append("[").append(color).append("]◉ ").append(zone).append("[]\n");

            if (showBar) {
                // Barra de 0 a 1200°C
                float pct = Math.min(temp / 1200f, 1f);
                int filled = (int)(pct * 10f);
                sb.append("[");
                for (int j = 0; j < 10; j++) {
                    if (j < filled) {
                        // Gradiente de color: cyan→green→yellow→red
                        if      (j < 2) sb.append("[cyan]█[]");
                        else if (j < 6) sb.append("[green]█[]");
                        else if (j < 8) sb.append("[yellow]█[]");
                        else            sb.append("[scarlet]█[]");
                    } else {
                        sb.append("[gray]░[]");
                    }
                }
                sb.append("]\n");
            }

            if (alertCrit && temp >= 1000f)
                sb.append("[scarlet]⚠ RIESGO MELTDOWN[]");

            sb.append("\n");
        }

        if (!found) {
            sb.append("[lightgray]").append(title).append("[]\n");
            sb.append("[lightgray]Sin Delta Reactor conectado[]");
        }

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, MicroGPU.MicroGPUBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]🌡 Temperature Display[]").row();
        table.add("[lightgray]" + description + "[]").row();
        table.row();

        table.add("[lightgray]Conecta un Delta Reactor para monitorear[]").row();
        table.row();

        table.add("[white]Zonas de temperatura:[]").row();
        table.add("[cyan]< 200°C → Frío (no genera)[]").row();
        table.add("[green]200-800°C → Óptimo[]").row();
        table.add("[yellow]800-1000°C → Advertencia[]").row();
        table.add("[scarlet]> 1000°C → CRÍTICO[]").row();
        table.row();

        table.check("[white]Mostrar barra[]",    showBar,   v -> showBar   = v).row();
        table.check("[white]Mostrar zona[]",     showZone,  v -> showZone  = v).row();
        table.check("[scarlet]Alerta crítica[]", alertCrit, v -> alertCrit = v).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(20).row();
    }

    @Override
    public String toCode() {
        return
            "# Temperature Display - generado por GPU\n" +
            "sensor temp deltaReactor1 @heat\n" +
            "print \"" + title + "\\n\"\n" +
            "print temp \"°C\\n\"\n" +
            "jump crit greaterThan temp 1000\n" +
            "jump warn greaterThan temp 800\n" +
            "jump ok greaterThan temp 200\n" +
            "print \"[cyan]FRÍO[]\\n\"\njump end always\n" +
            "ok:   print \"[green]ÓPTIMO[]\\n\"\njump end always\n" +
            "warn: print \"[yellow]ADVERTENCIA[]\\n\"\njump end always\n" +
            "crit: print \"[scarlet]CRÍTICO[]\\n\"\n" +
            "end:  printflush display1\n";
    }
}
