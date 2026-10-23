package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import singularity.blocks.gpu.MicroGPU;

/**
 * Función 1: Energy Counter
 *
 * Muestra producción / consumo / balance de la red eléctrica en displays conectados.
 *
 * Configurable:
 * - Qué datos mostrar (checkboxes)
 * - Velocidad de actualización
 * - Título personalizable
 */
public class EnergyCounter extends GPUFunction {

    private boolean showProduction  = true;
    private boolean showConsumption = true;
    private boolean showBalance     = true;
    private boolean showStorage     = false;
    private String  title           = "⚡ ENERGY";

    private float timer    = 0f;
    private float interval = 60f; // 1 segundo

    public EnergyCounter() {
        super("Energy Counter",
              "Producción, consumo y balance de la red eléctrica",
              "⚡", 0);
    }

    @Override
    public void update(MicroGPU.MicroGPUBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        float prod = 0f, cons = 0f, stored = 0f, storeMax = 0f;

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b != null && b.power != null && b.power.graph != null) {
                prod     = b.power.graph.getPowerProduced() * 60f;
                cons     = b.power.graph.getPowerNeeded()   * 60f;
                stored   = b.power.graph.getBatteryStored();
                storeMax = b.power.graph.getBatteryCapacity();
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        if (showProduction)
            sb.append("[green]▲ ").append(formatNum(prod)).append("/s[]\n");
        if (showConsumption)
            sb.append("[red]▼ ").append(formatNum(cons)).append("/s[]\n");
        if (showBalance) {
            float bal = prod - cons;
            sb.append(bal >= 0 ? "[cyan]" : "[scarlet]")
              .append("= ").append(bal >= 0 ? "+" : "")
              .append(formatNum(bal)).append("/s[]\n");
        }
        if (showStorage && storeMax > 0) {
            float pct = storeMax > 0 ? stored / storeMax * 100f : 0f;
            sb.append("[yellow]▣ ").append(String.format("%.0f%%", pct)).append("[]\n");
        }

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, MicroGPU.MicroGPUBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]⚡ Energy Counter[]").row();
        table.add("[lightgray]" + description + "[]").row();
        table.row();

        table.check("[green]Producción[]",  showProduction,  v -> showProduction  = v).row();
        table.check("[red]Consumo[]",       showConsumption, v -> showConsumption = v).row();
        table.check("[cyan]Balance[]",      showBalance,     v -> showBalance     = v).row();
        table.check("[yellow]Batería %[]",  showStorage,     v -> showStorage     = v).row();
        table.row();

        table.add("[lightgray]Actualizar cada (ticks):[]").row();
        table.slider(10f, 300f, 10f, interval, v -> interval = v).width(160f).row();
        table.label(() -> "[white]" + (int)(interval / 60f) + "s[]").row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(20).row();
    }

    @Override
    public String toCode() {
        return
            "# Energy Counter - generado por GPU\n" +
            "sensor prod @nucleus @powerNetProduced\n" +
            "sensor cons @nucleus @powerNetConsumed\n" +
            "op mul prod prod 60\n" +
            "op mul cons cons 60\n" +
            "op sub bal prod cons\n" +
            "print \"" + title + "\\n\"\n" +
            "print \"▲ \" prod \"/s\\n\"\n" +
            "print \"▼ \" cons \"/s\\n\"\n" +
            "print \"= \" bal \"/s\\n\"\n" +
            "printflush display1\n";
    }
}
