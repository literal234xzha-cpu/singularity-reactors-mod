package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 34: Combat AI
 *
 * Control inteligente de unidades en combate: targeting, formaciones y retirada.
 *
 * Disponible desde: HiperGPU (tier 2)
 */
public class CombatAI extends GPUFunction {

    private String title = "⚔ Combat AI";
    private float  timer    = 0f;
    private float  interval = 5f;

    private float retreatThreshold = 0.3f;
    private boolean autoTarget = true;
    private boolean autoRetreat = true;
    private boolean formations = true;

    public CombatAI() {
        super("Combat AI", "Control inteligente de unidades en combate: targeting, formaciones y retirada.", "⚔", 2);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        int unitCount = 0;
        int enemyCount = 0;

        try { unitCount = build.team.data().unitCount; } catch (Exception ignored) {}
        try {
            for (mindustry.gen.Unit u : mindustry.gen.Groups.unit) {
                if (u.team != build.team) enemyCount++;
            }
        } catch (Exception ignored) {}

        boolean shouldRetreat = autoRetreat && enemyCount > unitCount * 2;
        boolean shouldAttack = autoTarget && enemyCount > 0 && !shouldRetreat;

        sb.append("[white]Unidades: ").append(unitCount).append("[]\n");
        sb.append("[red]Enemigos: ").append(enemyCount).append("[]\n");

        if (shouldRetreat) {
            sb.append("[red]⚠ RETIRADA TACTICA[]\n");
        } else if (shouldAttack) {
            sb.append("[green]⚔ ATAQUE[]\n");
        } else {
            sb.append("[yellow]● patrol[]\n");
        }

        // Controlar torretas conectadas
        int turretsActive = 0;
        int totalTurrets = 0;
        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;

            totalTurrets++;

            if (autoTarget && !shouldRetreat) {
                b.enabled = true;
                turretsActive++;
            } else if (shouldRetreat) {
                b.enabled = false;
            }
            if (b.enabled) turretsActive++;
        }

        sb.append("[white]Torretas: ").append(turretsActive).append("/").append(totalTurrets).append("[]\n");

        // Formaciones
        if (formations && unitCount > 0) {
            String formationType = unitCount < 10 ? "Línea" :
                                   unitCount < 30 ? "Cuña" : "Círculo";
            sb.append("[accent]Formación: ").append(formationType).append("[]\n");
        }

        if (build.links.isEmpty() && unitCount == 0) {
            sb.append("[lightgray]Sin unidades conectadas[]");
        }

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]⚔ Combat AI[]").row();
        table.add("[lightgray]Control inteligente de unidades en combate: targeting, formaciones y retirada.[]").width(180f).wrap(true).row();
        table.row();

        table.add("[white]Umbral de retirada (%):[]").row();
        table.slider(10f, 80f, 5f, retreatThreshold * 100f, v -> retreatThreshold = v / 100f).width(160f).row();
        table.label(() -> "[white]" + (int)(retreatThreshold * 100) + "%[]").row();
        table.row();

        table.check("[white]Targeting automático[]", autoTarget, v -> autoTarget = v).row();
        table.check("[white]Retirada automática[]", autoRetreat, v -> autoRetreat = v).row();
        table.check("[white]Formaciones[]", formations, v -> formations = v).row();
        table.row();

        table.add("[white]Verificar cada (ticks):[]").row();
        table.slider(1f, 30f, 1f, interval, v -> interval = v).width(160f).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(24).row();
    }

    @Override
    public String toCode() {
        return "# Combat AI - generado por HiperGPU\n" +
               "# Control de combate inteligente\n" +
               "sensor units @unitCount\n" +
               "sensor enemies @enemies\n" +
               "op greaterThan retreat enemies units\n" +
               "control enabled turret1 1 0 0 0\n" +
               "printflush display1\n";
    }
}
