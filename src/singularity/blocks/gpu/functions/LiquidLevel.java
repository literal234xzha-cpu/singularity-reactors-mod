package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 4: Liquid Level
 *
 * Muestra el nivel de líquido en tanques/bloques conectados.
 * Muestra tipo de líquido, cantidad y porcentaje de capacidad.
 * Alerta si baja de un umbral configurado.
 */
public class LiquidLevel extends GPUFunction {

    private float alertThreshold = 20f; // % mínimo antes de alerta
    private boolean showBar      = true;
    private boolean showType     = true;
    private String title         = "💧 LIQUID";

    private float timer    = 0f;
    private float interval = 60f;

    public LiquidLevel() {
        super("Liquid Level",
              "Nivel de líquido en tanques conectados con alerta",
              "💧", 0);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        boolean any = false;

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null || b.liquids == null) continue;

            float total    = b.liquids.currentAmount();
            float capacity = b.block.liquidCapacity;
            if (capacity <= 0f) continue;

            float pct = total / capacity * 100f;
            Liquid liq = b.liquids.current();
            boolean alert = pct < alertThreshold;
            String color  = alert ? "red" : (pct > 75f ? "cyan" : "white");
            String mark   = alert ? "⚠" : "·";

            any = true;
            sb.append("[").append(color).append("]")
              .append(mark).append(" ");

            if (showType && liq != null)
                sb.append(liq.localizedName).append(": ");

            sb.append(String.format("%.0f%%", pct));

            if (showBar) {
                sb.append(" [");
                int filled = (int)(pct / 10f);
                for (int j = 0; j < 10; j++)
                    sb.append(j < filled ? "█" : "░");
                sb.append("]");
            }
            sb.append("[]\n");
        }

        if (!any) sb.append("[lightgray]Sin tanques conectados[]");

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]💧 Liquid Level[]").row();
        table.add("[lightgray]" + description + "[]").row();
        table.row();

        table.add("[red]Alerta si % < X:[]").row();
        table.slider(0f, 90f, 5f, alertThreshold, v -> alertThreshold = v).width(160f).row();
        table.label(() -> "[white]" + (int)alertThreshold + "%[]").row();
        table.row();

        table.check("[cyan]Mostrar barra[]", showBar,  v -> showBar  = v).row();
        table.check("[white]Mostrar tipo[]", showType, v -> showType = v).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(20).row();
    }

    @Override
    public String toCode() {
        return
            "# Liquid Level - generado por GPU\n" +
            "sensor liq tank1 @liquidCapacity\n" +
            "sensor total tank1 @totalLiquids\n" +
            "op div pct total liq\n" +
            "op mul pct pct 100\n" +
            "print \"" + title + "\\n\"\n" +
            "print \"Nivel: \" pct \"%\\n\"\n" +
            "printflush display1\n";
    }
}
