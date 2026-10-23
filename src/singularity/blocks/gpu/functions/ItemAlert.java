package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import singularity.blocks.gpu.MicroGPU;

/**
 * Función 9: Item Alert
 *
 * Monitorea items específicos y genera alertas cuando
 * la cantidad cae por debajo de umbrales personalizados por item.
 *
 * Diferencia con ItemCounter: esta función está enfocada
 * SOLO en alertas, con umbrales individuales por item,
 * y puede mostrar solo los items en estado crítico.
 */
public class ItemAlert extends GPUFunction {

    // Par item → umbral mínimo
    private final Seq<Item>  alertItems  = new Seq<>();
    private final Seq<Float> thresholds  = new Seq<>();  // mínimo por item

    private boolean soundAlert   = false; // reservado para futuro
    private boolean onlyCritical = true;
    private String  title        = "⚠ ALERTAS";

    private float timer    = 0f;
    private float interval = 60f;

    public ItemAlert() {
        super("Item Alert",
              "Alertas personalizadas por cantidad de item específico",
              "⚠", 0);
        // Defaults
        alertItems.add(Items.silicon,   Items.surgeAlloy, Items.phaseFabric);
        thresholds.add(500f,            200f,             100f);
    }

    @Override
    public void update(MicroGPU.MicroGPUBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        // Contar items en todos los contenedores
        int[] totals = new int[alertItems.size];
        for (int li = 0; li < build.links.size; li++) {
            LogicLink link = build.links.get(li);
            Building b = link.lastBuild;
            if (b == null || b.items == null) continue;
            for (int i = 0; i < alertItems.size; i++)
                totals[i] += b.items.get(alertItems.get(i));
        }

        StringBuilder sb = new StringBuilder();
        boolean anyAlert = false;

        for (int i = 0; i < alertItems.size; i++) {
            Item  item      = alertItems.get(i);
            int   total     = totals[i];
            float threshold = thresholds.get(i);
            boolean alert   = total < threshold;
            if (onlyCritical && !alert) continue;
            anyAlert = true;

            String color = alert ? "scarlet" : "green";
            String mark  = alert ? "⚠" : "✓";

            sb.append("[").append(color).append("]")
              .append(mark).append(" ")
              .append(item.localizedName).append(": ")
              .append(formatNum(total))
              .append(alert ? " / " + formatNum(threshold) : "")
              .append("[]\n");
        }

        if (!anyAlert && onlyCritical) {
            sb.append("[green]✓ Sin alertas activas[]");
        } else if (!anyAlert) {
            sb.append("[green]✓ Todos los items OK[]");
        }

        // Header solo si hay alertas
        String header = anyAlert
            ? "[scarlet]" + title + "[]\n"
            : "[green]" + title + "[]\n";

        printToDisplays(build, header + sb.toString());
    }

    @Override
    public void buildConfig(Table table, MicroGPU.MicroGPUBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]⚠ Item Alert[]").row();
        table.add("[lightgray]" + description + "[]").row();
        table.row();

        // Lista de alertas configuradas
        table.add("[white]Alertas configuradas:[]").row();
        for (int i = 0; i < alertItems.size; i++) {
            final int idx = i;
            Item item = alertItems.get(i);
            float thresh = thresholds.get(i);

            table.table(row -> {
                row.add("[yellow]" + item.localizedName + "[]").width(80f).padRight(4f);
                row.add("< ").padRight(2f);
                row.field(String.valueOf((int)thresh), v -> {
                    try { thresholds.set(idx, Float.parseFloat(v)); }
                    catch (NumberFormatException ignored) {}
                }).width(60f).padRight(4f);
                row.button("[red]✕[]", () -> {
                    alertItems.remove(idx);
                    thresholds.remove(idx);
                }).size(24f, 24f);
            }).row();
        }
        table.row();

        // Agregar nuevo item de alerta
        table.add("[white]Agregar item:[]").row();
        Item[] common = {
            Items.copper, Items.lead, Items.silicon, Items.titanium,
            Items.graphite, Items.plastanium, Items.thorium,
            Items.surgeAlloy, Items.phaseFabric, Items.metaglass
        };
        int col = 0;
        for (Item it : common) {
            if (it == null || alertItems.contains(it)) continue;
            final Item fi = it;
            table.button("+ " + fi.localizedName, () -> {
                alertItems.add(fi);
                thresholds.add(100f);
            }).height(24f);
            if (++col % 2 == 0) table.row();
        }
        table.row();

        table.check("[yellow]Solo mostrar alertas activas[]", onlyCritical, v -> onlyCritical = v).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(20).row();
    }

    @Override
    public String toCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Item Alert - generado por GPU\n");
        sb.append("print \"").append(title).append("\\n\"\n");
        for (int i = 0; i < alertItems.size; i++) {
            Item  item  = alertItems.get(i);
            float thresh = thresholds.get(i);
            sb.append("sensor ").append(item.name)
              .append(" container1 @").append(item.name).append("\n");
            sb.append("jump ok").append(i).append(" greaterThan ")
              .append(item.name).append(" ").append((int)thresh).append("\n");
            sb.append("print \"⚠ ").append(item.localizedName)
              .append(": \" ").append(item.name).append(" \"\\n\"\n");
            sb.append("ok").append(i).append(":\n");
        }
        sb.append("printflush display1\n");
        return sb.toString();
    }
}
