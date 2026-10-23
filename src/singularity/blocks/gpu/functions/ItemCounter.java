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
 * Función 3: Item Counter
 *
 * Muestra la cantidad de items en contenedores conectados.
 * Configura qué items monitorear, umbrales de alerta y formato.
 */
public class ItemCounter extends GPUFunction {

    private final Seq<Item> watched = new Seq<>();
    private int     alertThreshold = 100;
    private int     goodThreshold  = 5000;
    private boolean showPct        = false;
    private boolean onlyAlerts     = false;
    private String  title          = "📦 ITEMS";

    private float timer    = 0f;
    private float interval = 60f;

    public ItemCounter() {
        super("Item Counter",
              "Items en contenedores conectados con alertas",
              "📦", 0);
        watched.addAll(Items.copper, Items.lead, Items.silicon,
                       Items.titanium, Items.graphite);
    }

    @Override
    public void update(MicroGPU.MicroGPUBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        int[] totals = new int[watched.size];
        int[] caps   = new int[watched.size];

        for (int li = 0; li < build.links.size; li++) {
            LogicLink link = build.links.get(li);
            Building b = link.lastBuild;
            if (b == null || b.items == null) continue;
            for (int i = 0; i < watched.size; i++) {
                totals[i] += b.items.get(watched.get(i));
                caps[i]   += b.block.itemCapacity;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        boolean anyAlert = false;

        for (int i = 0; i < watched.size; i++) {
            Item item  = watched.get(i);
            int  total = totals[i];
            boolean alert = total < alertThreshold;
            boolean good  = total > goodThreshold;
            if (onlyAlerts && !alert) continue;
            anyAlert |= alert;

            String color = alert ? "red" : (good ? "green" : "white");
            String mark  = alert ? "⚠" : (good ? "✓" : "·");

            sb.append("[").append(color).append("]")
              .append(mark).append(" ")
              .append(item.localizedName).append(": ")
              .append(formatNum(total));

            if (showPct && caps[i] > 0)
                sb.append(String.format(" (%.0f%%)", (float)total / caps[i] * 100f));

            sb.append("[]\n");
        }

        if (onlyAlerts && !anyAlert)
            sb.append("[green]✓ Todos OK[]");

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, MicroGPU.MicroGPUBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]📦 Item Counter[]").row();
        table.add("[lightgray]" + description + "[]").row();
        table.row();

        table.add("[white]Items a monitorear:[]").row();

        // Lista completa en 2 columnas
        Item[] all = {
            Items.copper, Items.lead, Items.silicon, Items.titanium,
            Items.graphite, Items.plastanium, Items.thorium,
            Items.surgeAlloy, Items.phaseFabric, Items.metaglass,
            Items.coal, Items.sand, Items.scrap, Items.pyratite,
            Items.sporePod, Items.beryllium, Items.tungsten,
            Items.oxide, Items.carbide, Items.fissileMatter
        };

        int col = 0;
        for (Item it : all) {
            if (it == null) continue;
            final Item fi = it;
            table.check(fi.localizedName, watched.contains(fi), v -> {
                if (v) { if (!watched.contains(fi)) watched.add(fi); }
                else   { watched.remove(fi); }
            });
            if (++col % 2 == 0) table.row();
        }
        table.row();

        // Umbral alerta (rojo)
        table.add("[red]Alerta si < X:[]").row();
        table.slider(0f, 10000f, 100f, alertThreshold,
            v -> alertThreshold = (int)v).width(160f).row();
        table.label(() -> "[white]" + alertThreshold + " u[]").row();
        table.row();

        // Umbral bueno (verde)
        table.add("[green]OK si > X:[]").row();
        table.slider(0f, 100000f, 500f, goodThreshold,
            v -> goodThreshold = (int)v).width(160f).row();
        table.label(() -> "[white]" + goodThreshold + " u[]").row();
        table.row();

        table.check("[white]Mostrar %[]",       showPct,    v -> showPct    = v).row();
        table.check("[yellow]Solo alertas[]",   onlyAlerts, v -> onlyAlerts = v).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(20).row();
    }

    @Override
    public String toCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Item Counter - generado por GPU\n");
        sb.append("print \"").append(title).append("\\n\"\n");
        for (Item item : watched) {
            sb.append("sensor ").append(item.name)
              .append(" container1 @").append(item.name).append("\n");
            sb.append("print \"· ").append(item.localizedName)
              .append(": \" ").append(item.name).append(" \"\\n\"\n");
        }
        sb.append("printflush display1\n");
        return sb.toString();
    }
}
