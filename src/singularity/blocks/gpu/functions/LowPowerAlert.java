package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 2: Low Power Alert
 *
 * Monitorea la red eléctrica y muestra alerta cuando
 * la cobertura cae por debajo del umbral configurado.
 *
 * Niveles: OK → Warning → Critical (parpadea)
 */
public class LowPowerAlert extends GPUFunction {

    private float   warningThreshold  = 80f;
    private float   criticalThreshold = 50f;
    private String  customMessage     = "";
    private boolean blinkOnCritical   = true;

    private float   timer      = 0f;
    private float   interval   = 30f;
    private boolean blinkState = false;
    private float   blinkTimer = 0f;

    public LowPowerAlert() {
        super("Low Power Alert",
              "Alerta cuando la energía cae por debajo del umbral",
              "🚨", 0);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        timer      += Time.delta;
        blinkTimer += Time.delta;

        if (blinkTimer > 30f) {
            blinkTimer = 0f;
            blinkState = !blinkState;
        }
        if (timer < interval) return;
        timer = 0f;

        float prod = 0f, cons = 0f;

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b != null && b.power != null && b.power.graph != null) {
                prod = b.power.graph.getPowerProduced() * 60f;
                cons = b.power.graph.getPowerNeeded()   * 60f;
                break;
            }
        }

        float coverage = cons > 0f ? (prod / cons) * 100f : 100f;
        StringBuilder sb = new StringBuilder();

        if (coverage < criticalThreshold) {
            if (!blinkOnCritical || blinkState) {
                sb.append("[scarlet]⚠ CRÍTICO ⚠[]\n");
                sb.append("[red]ENERGÍA INSUFICIENTE[]\n\n");
                sb.append("[red]Cobertura: ").append(String.format("%.0f%%", coverage)).append("[]\n");
                sb.append("[red]Déficit: ").append(formatNum(cons - prod)).append("/s[]\n");
                if (!customMessage.isEmpty())
                    sb.append("\n[white]").append(customMessage).append("[]");
            }
        } else if (coverage < warningThreshold) {
            sb.append("[yellow]⚠ ADVERTENCIA[]\n");
            sb.append("[yellow]Energía Baja[]\n\n");
            sb.append("[white]Cobertura: ").append(String.format("%.0f%%", coverage)).append("\n");
            sb.append("[orange]Prod: ").append(formatNum(prod)).append("/s[]\n");
            sb.append("[red]Cons: ").append(formatNum(cons)).append("/s[]\n");
            if (!customMessage.isEmpty())
                sb.append("\n[white]").append(customMessage).append("[]");
        } else {
            sb.append("[green]✓ ENERGÍA OK[]\n\n");
            sb.append("[white]Cobertura: [green]").append(String.format("%.0f%%", coverage)).append("[]\n");
            sb.append("[green]").append(formatNum(prod)).append("/s[]\n");
        }

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[yellow]🚨 Low Power Alert[]").row();
        table.add("[lightgray]" + description + "[]").row();
        table.row();

        table.add("[yellow]Umbral Advertencia (%):[]").row();
        table.slider(10f, 100f, 5f, warningThreshold, v -> warningThreshold = v).width(160f).row();
        table.label(() -> "[white]" + (int)warningThreshold + "%[]").row();
        table.row();

        table.add("[red]Umbral Crítico (%):[]").row();
        table.slider(5f, 90f, 5f, criticalThreshold,
            v -> criticalThreshold = Math.min(v, warningThreshold - 5f)).width(160f).row();
        table.label(() -> "[white]" + (int)criticalThreshold + "%[]").row();
        table.row();

        table.check("[scarlet]Parpadear en crítico[]", blinkOnCritical, v -> blinkOnCritical = v).row();
        table.row();

        table.add("[lightgray]Mensaje personalizado:[]").row();
        table.field(customMessage, v -> customMessage = v).width(160f).maxTextLength(48).row();
    }

    @Override
    public String toCode() {
        return
            "# Low Power Alert - generado por GPU\n" +
            "sensor prod @nucleus @powerNetProduced\n" +
            "sensor cons @nucleus @powerNetConsumed\n" +
            "op mul prod prod 60\n" +
            "op mul cons cons 60\n" +
            "op div cov prod cons\n" +
            "op mul cov cov 100\n" +
            "jump crit lessThan cov " + (int)criticalThreshold + "\n" +
            "jump warn lessThan cov " + (int)warningThreshold + "\n" +
            "print \"[green]✓ OK[]\\n\"\njump end always\n" +
            "warn:\nprint \"[yellow]⚠ WARNING[]\\n\"\njump end always\n" +
            "crit:\nprint \"[red]⚠ CRÍTICO[]\\n\"\n" +
            "end:\nprint \"Cov: \" cov \"%\\n\"\nprintflush display1\n";
    }
}
