package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 6: Health Monitor
 *
 * Monitorea la salud de bloques conectados.
 * Muestra barra de salud, porcentaje y alerta si un bloque
 * está siendo atacado o tiene salud crítica.
 */
public class HealthMonitor extends GPUFunction {

    private float alertThreshold = 50f; // % HP para alerta
    private float criticalThreshold = 25f;
    private boolean showBar  = true;
    private boolean showName = true;
    private String  title    = "❤ HEALTH";

    private float timer    = 0f;
    private float interval = 30f; // más frecuente para detectar daño

    public HealthMonitor() {
        super("Health Monitor",
              "Salud de bloques conectados con alerta de daño",
              "❤", 0);
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
            if (b == null || b.maxHealth() <= 0) continue;

            float pct = b.health / b.maxHealth() * 100f;
            boolean critical = pct < criticalThreshold;
            boolean warning  = pct < alertThreshold;
            String color     = critical ? "scarlet" : (warning ? "yellow" : "green");
            String mark      = critical ? "⚠" : (warning ? "!" : "✓");

            any = true;
            sb.append("[").append(color).append("]")
              .append(mark).append(" ");

            if (showName)
                sb.append(link.name).append(": ");

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

        if (!any) sb.append("[lightgray]Sin bloques conectados[]");

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]❤ Health Monitor[]").row();
        table.add("[lightgray]" + description + "[]").row();
        table.row();

        table.add("[yellow]Umbral advertencia (%):[]").row();
        table.slider(10f, 90f, 5f, alertThreshold, v -> alertThreshold = v).width(160f).row();
        table.label(() -> "[white]" + (int)alertThreshold + "%[]").row();
        table.row();

        table.add("[red]Umbral crítico (%):[]").row();
        table.slider(5f, 80f, 5f, criticalThreshold,
            v -> criticalThreshold = Math.min(v, alertThreshold - 5f)).width(160f).row();
        table.label(() -> "[white]" + (int)criticalThreshold + "%[]").row();
        table.row();

        table.check("[white]Mostrar barra[]", showBar,  v -> showBar  = v).row();
        table.check("[white]Mostrar nombre[]", showName, v -> showName = v).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(20).row();
    }

    @Override
    public String toCode() {
        return
            "# Health Monitor - generado por GPU\n" +
            "sensor hp block1 @health\n" +
            "sensor maxhp block1 @maxHealth\n" +
            "op div pct hp maxhp\n" +
            "op mul pct pct 100\n" +
            "print \"" + title + "\\n\"\n" +
            "print \"HP: \" pct \"%\\n\"\n" +
            "printflush display1\n";
    }
}
