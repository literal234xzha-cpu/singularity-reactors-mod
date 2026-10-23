package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 28: Base Dashboard
 *
 * Panel completo: energía + items + temperatura + unidades en un solo display.
 *
 * Disponible desde: HiperGPU (tier 2)
 */
public class BaseDashboard extends GPUFunction {

    private String title = "📊 Base Dashboard";
    private float  timer    = 0f;
    private float  interval = 30f;

    private boolean showPower   = true;
    private boolean showItems   = true;
    private boolean showUnits   = true;
    private boolean showTemp    = true;

    public BaseDashboard() {
        super("Base Dashboard", "Panel completo: energía + items + temperatura + unidades en un solo display.", "📊", 2);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        float totalProd = 0f, totalNeed = 0f;
        int totalItems = 0;
        int reactorCount = 0;
        float maxTemp = 0f;
        int unitCount = 0;

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;

            if (b.power != null && b.power.graph != null) {
                totalProd = b.power.graph.getPowerProduced() * 60f;
                totalNeed = b.power.graph.getPowerNeeded() * 60f;
            }

            if (b.items != null) {
                totalItems += b.items.total();
            }

            reactorCount++;
        }

        try { unitCount = build.team.data().unitCount; } catch (Exception ignored) {}

        if (showPower) {
            sb.append("[cyan]⚡ Prod: ").append(formatNum(totalProd)).append("/s[]\n");
            sb.append("[red]Demanda: ").append(formatNum(totalNeed)).append("/s[]\n");
            float balance = totalProd - totalNeed;
            String balStr = balance >= 0 ? "[green]+" : "[red]";
            sb.append(balStr).append("Balance: ").append(formatNum(balance)).append("/s[]\n");
        }

        if (showItems) {
            sb.append("[orange]📦 Items: ").append(totalItems).append("[]\n");
        }

        if (showTemp && reactorCount > 0) {
            float ratio = totalNeed > 0 ? totalProd / totalNeed : 1f;
            String tempStr = ratio > 0.8f ? "[red]" : ratio > 0.5f ? "[yellow]" : "[green]";
            sb.append(tempStr).append("🌡 Carga: ").append((int)(ratio * 100)).append("%[]\n");
            sb.append("[white]Bloques: ").append(reactorCount).append("[]\n");
        }

        if (showUnits) {
            sb.append("[accent]⚔ Unidades: ").append(unitCount).append("[]\n");
        }

        if (build.links.isEmpty()) {
            sb.append("[lightgray]Sin bloques conectados[]");
        }

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]📊 Base Dashboard[]").row();
        table.add("[lightgray]Panel completo: energía + items + temperatura + unidades en un solo display.[]").width(180f).wrap(true).row();
        table.row();

        table.add("[white]Mostrar:[]").row();
        table.check("[cyan]⚡ Energía[]", showPower, v -> showPower = v).row();
        table.check("[orange]📦 Items[]", showItems, v -> showItems = v).row();
        table.check("[yellow]🌡 Temperatura[]", showTemp, v -> showTemp = v).row();
        table.check("[accent]⚔ Unidades[]", showUnits, v -> showUnits = v).row();
        table.row();

        table.add("[white]Actualizar cada (ticks):[]").row();
        table.slider(5f, 120f, 5f, interval, v -> interval = v).width(160f).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(24).row();
    }

    @Override
    public String toCode() {
        return "# Base Dashboard - generado por HiperGPU\n" +
               "# Panel completo de la base\n" +
               "sensor power power1 @stored\n" +
               "sensor items vault1 @totalItems\n" +
               "print \"Energia: \" power\n" +
               "print \"Items: \" items\n" +
               "printflush display1\n";
    }
}
