package singularity.blocks.gpu;

import arc.scene.ui.layout.Table;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import mindustry.world.blocks.logic.MessageBlock;

/**
 * Clase base para funciones precargadas de las GPUs.
 * Usa LogicBuild como tipo base para ser compatible con
 * MicroGPU, GPU y HiperGPU sin herencia de clases internas.
 */
public abstract class GPUFunction {

    public final String name;
    public final String description;
    public final String icon;
    public final int    tier;

    public GPUFunction(String name, String description, String icon, int tier) {
        this.name = name; this.description = description;
        this.icon = icon; this.tier = tier;
    }

    /** Se ejecuta cada tick cuando está activa */
    public abstract void update(LogicBuild build);

    /** Controles de configuración en la UI */
    public abstract void buildConfig(Table table, LogicBuild build);

    /** Código equivalente en Mindustry Logic */
    public abstract String toCode();

    // ── Utilidades ───────────────────────────────────────────────────────────

    public static String formatNum(double val) {
        if (val >= 1_000_000) return String.format("%.1fM", val / 1_000_000);
        if (val >= 1_000)     return String.format("%.1fk", val / 1_000);
        return String.format("%.0f", val);
    }

    /** Escribe texto en todos los MessageBlock conectados */
    public static void printToDisplays(LogicBuild build, String text) {
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
