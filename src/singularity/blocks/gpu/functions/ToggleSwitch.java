package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 8: Toggle Switch
 *
 * Activa/desactiva bloques conectados con lógica configurable.
 * Modos:
 * - Manual: ON/OFF desde la UI
 * - Auto por energía: apaga si energía < umbral
 * - Auto por tiempo: ciclos ON/OFF periódicos
 * - Auto por item: apaga si contenedor lleno
 */
public class ToggleSwitch extends GPUFunction {

    public enum SwitchMode { MANUAL, AUTO_POWER, AUTO_TIME, AUTO_ITEM }

    private SwitchMode mode         = SwitchMode.MANUAL;
    private boolean    manualState  = true;
    private float      powerThresh  = 30f;   // % energía para apagar
    private float      onTicks      = 300f;  // ticks ON en modo tiempo
    private float      offTicks     = 120f;  // ticks OFF en modo tiempo
    private float      itemThresh   = 80f;   // % capacidad para apagar
    private String     title        = "🔌 SWITCH";

    // Estado interno
    private float  cycleTimer  = 0f;
    private boolean cycleState = true;
    private float  timer       = 0f;
    private float  interval    = 20f;

    public ToggleSwitch() {
        super("Toggle Switch",
              "Activa/desactiva bloques por condición o manualmente",
              "🔌", 0);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        timer      += Time.delta;
        cycleTimer += Time.delta;

        // Ciclo ON/OFF para modo tiempo
        if (mode == SwitchMode.AUTO_TIME) {
            float total = onTicks + offTicks;
            cycleState  = (cycleTimer % total) < onTicks;
        }

        if (timer < interval) return;
        timer = 0f;

        boolean targetState = computeState(build);

        // Aplicar estado a todos los bloques conectados
        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;
            b.enabled = targetState;
        }

        // Display
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        String stateColor = targetState ? "green" : "red";
        String stateText  = targetState ? "ON ●" : "OFF ○";
        sb.append("[").append(stateColor).append("]Estado: ").append(stateText).append("[]\n");
        sb.append("[gray]Modo: ").append(mode.name()).append("[]\n");
        sb.append("[white]Bloques: ").append(build.links.size).append("[]\n");

        printToDisplays(build, sb.toString());
    }

    private boolean computeState(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        switch (mode) {
            case MANUAL:     return manualState;
            case AUTO_TIME:  return cycleState;
            case AUTO_POWER: {
                for (int i = 0; i < build.links.size; i++) {
                    Building b = build.links.get(i).lastBuild;
                    if (b != null && b.power != null && b.power.graph != null) {
                        float prod = b.power.graph.getPowerProduced();
                        float cons = b.power.graph.getPowerNeeded();
                        float cov  = cons > 0 ? prod / cons * 100f : 100f;
                        return cov >= powerThresh;
                    }
                }
                return true;
            }
            case AUTO_ITEM: {
                for (int i = 0; i < build.links.size; i++) {
                    Building b = build.links.get(i).lastBuild;
                    if (b != null && b.items != null && b.block.itemCapacity > 0) {
                        float pct = (float)b.items.total() / b.block.itemCapacity * 100f;
                        return pct < itemThresh;
                    }
                }
                return true;
            }
            default: return true;
        }
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]🔌 Toggle Switch[]").row();
        table.add("[lightgray]" + description + "[]").row();
        table.row();

        // Selector de modo
        table.add("[white]Modo:[]").row();
        table.button("Manual",       () -> mode = SwitchMode.MANUAL).width(140f).height(26f)
            .checked(b -> mode == SwitchMode.MANUAL).row();
        table.button("Auto Energía", () -> mode = SwitchMode.AUTO_POWER).width(140f).height(26f)
            .checked(b -> mode == SwitchMode.AUTO_POWER).row();
        table.button("Auto Tiempo",  () -> mode = SwitchMode.AUTO_TIME).width(140f).height(26f)
            .checked(b -> mode == SwitchMode.AUTO_TIME).row();
        table.button("Auto Items",   () -> mode = SwitchMode.AUTO_ITEM).width(140f).height(26f)
            .checked(b -> mode == SwitchMode.AUTO_ITEM).row();
        table.row();

        // Controles según modo
        table.table(ctrl -> {
            ctrl.button("[green]ON[]",  () -> { manualState = true;  mode = SwitchMode.MANUAL; }).size(60f, 30f).padRight(4f);
            ctrl.button("[red]OFF[]",   () -> { manualState = false; mode = SwitchMode.MANUAL; }).size(60f, 30f);
        }).row();
        table.row();

        // Config Auto Energía
        table.add("[yellow]Auto energía - apagar si < (%):[]").row();
        table.slider(5f, 95f, 5f, powerThresh, v -> powerThresh = v).width(160f).row();
        table.label(() -> "[white]" + (int)powerThresh + "%[]").row();
        table.row();

        // Config Auto Tiempo
        table.add("[white]Ciclo ON (ticks):[]").row();
        table.slider(60f, 3600f, 60f, onTicks,  v -> onTicks  = v).width(160f).row();
        table.add("[white]Ciclo OFF (ticks):[]").row();
        table.slider(60f, 3600f, 60f, offTicks, v -> offTicks = v).width(160f).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(20).row();
    }

    @Override
    public String toCode() {
        return
            "# Toggle Switch - generado por GPU\n" +
            "sensor prod @nucleus @powerNetProduced\n" +
            "sensor cons @nucleus @powerNetConsumed\n" +
            "op div cov prod cons\n" +
            "op mul cov cov 100\n" +
            "op lessThan off cov " + (int)powerThresh + "\n" +
            "control enabled block1 off\n" +
            "print \"Switch: \" off \"\\n\"\n" +
            "printflush display1\n";
    }
}
