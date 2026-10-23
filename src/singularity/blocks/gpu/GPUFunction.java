package singularity.blocks.gpu;

import arc.scene.ui.layout.Table;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import mindustry.world.blocks.logic.MessageBlock;

/**
 * Clase base para funciones precargadas de las GPUs.
 */
public abstract class GPUFunction {

    public final String name;
    public final String description;
    public final String icon;
    public final int tier; // 0=MicroGPU, 1=GPU, 2=HiperGPU

    public GPUFunction(String name, String description, String icon, int tier) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.tier = tier;
    }

    /** Se llama cada tick cuando la función está activa */
    public abstract void update(MicroGPU.MicroGPUBuild build);

    /** Construye los controles en la UI */
    public abstract void buildConfig(Table table, MicroGPU.MicroGPUBuild build);

    /** Código Mindustry Logic equivalente */
    public abstract String toCode();

    // ── Utilidades compartidas ────────────────────────────────────────────────

    public static String formatNum(double val) {
        if (val >= 1_000_000) return String.format("%.1fM", val / 1_000_000);
        if (val >= 1_000)     return String.format("%.1fk", val / 1_000);
        return String.format("%.0f", val);
    }

    /** Escribe texto en todos los MessageBlock conectados a la GPU */
    public static void printToDisplays(MicroGPU.MicroGPUBuild build, String text) {
        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b instanceof MessageBlock.MessageBuild) {
                MessageBlock.MessageBuild msg = (MessageBlock.MessageBuild) b;
                msg.message.setLength(0);
                msg.message.append(text);
            }
        }
    }
}
