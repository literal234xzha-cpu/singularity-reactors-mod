package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 35: Full Base Autopilot
 *
 * Gestión completa y autónoma de energía, defensa, economía y unidades.
 *
 * Disponible desde: HiperGPU (tier 2)
 */
public class FullBaseAutopilot extends GPUFunction {

    private String title = "🤖 Full Base Autopilot";
    private float  timer    = 0f;
    private float  interval = 15f;

    private boolean managePower   = true;
    private boolean manageDefense = true;
    private boolean manageEconomy = true;
    private boolean manageUnits   = true;

    private float powerThreshold = 0.8f;
    private float tempThreshold  = 0.85f;
    private float storageMaxFill = 0.9f;

    public FullBaseAutopilot() {
        super("Full Base Autopilot", "Gestión completa y autónoma de energía, defensa, economía y unidades.", "🤖", 2);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        // Recopilar datos
        float totalProd = 0f, totalNeed = 0f;
        int reactors = 0, turrets = 0, factories = 0;
        int totalItems = 0;
        int unitCount = 0;
        int enemyCount = 0;

        try { unitCount = build.team.data().unitCount; } catch (Exception ignored) {}
        try {
            for (mindustry.gen.Unit u : mindustry.gen.Groups.unit) {
                if (u.team != build.team) enemyCount++;
            }
        } catch (Exception ignored) {}

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;

            reactors++;

            if (b.power != null && b.power.graph != null) {
                totalProd = b.power.graph.getPowerProduced() * 60f;
                totalNeed = b.power.graph.getPowerNeeded() * 60f;
            }

            if (b.items != null) {
                totalItems += b.items.total();
            }
        }

        // ── Gestión de energía ──
        if (managePower) {
            sb.append("[cyan]⚡ Prod: ").append(formatNum(totalProd)).append("/s Dem: ").append(formatNum(totalNeed)).append("/s[]\n");

            float ratio = totalNeed > 0 ? totalProd / totalNeed : 1f;
            for (int i = 0; i < build.links.size; i++) {
                LogicLink link = build.links.get(i);
                Building b = link.lastBuild;
                if (b == null) continue;
                if (ratio < powerThreshold) {
                    b.enabled = true;
                }
            }
            if (ratio > tempThreshold) {
                sb.append("[red]⚠ Energía crítica[]\n");
            }
        }

        // ── Gestión de defensa ──
        if (manageDefense) {
            boolean waveActive = enemyCount > 0;
            sb.append(waveActive ? "[red]⚔ Defensa activa (" + enemyCount + " enemigos)[]\n" :
                                   "[green]✓ Defensa en espera[]\n");

            for (int i = 0; i < build.links.size; i++) {
                LogicLink link = build.links.get(i);
                Building b = link.lastBuild;
                if (b == null) continue;
                b.enabled = waveActive;
            }
        }

        // ── Gestión de economía ──
        if (manageEconomy) {
            sb.append("[orange]📦 Items: ").append(totalItems).append("[]\n");

            float avgItems = reactors > 0 ? (float) totalItems / reactors : 0;
            boolean shouldPause = avgItems > 500 * storageMaxFill;

            for (int i = 0; i < build.links.size; i++) {
                LogicLink link = build.links.get(i);
                Building b = link.lastBuild;
                if (b == null) continue;
                if (shouldPause && !manageDefense) {
                    b.enabled = false;
                }
            }
            sb.append(shouldPause ? "[yellow]Producción pausada[]\n" : "[green]Producción activa[]\n");
        }

        // ── Gestión de unidades ──
        if (manageUnits) {
            sb.append("[accent]⚔ Unidades: ").append(unitCount).append("[]\n");
            boolean shouldRetreat = enemyCount > unitCount * 2;
            if (shouldRetreat) sb.append("[red]⚠ Retirada recomendada[]\n");
        }

        sb.append("[white]R:").append(reactors).append("[]\n");

        if (build.links.isEmpty()) {
            sb.append("[lightgray]Sin bloques conectados[]");
        }

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]🤖 Full Base Autopilot[]").row();
        table.add("[lightgray]Gestión completa y autónoma de energía, defensa, economía y unidades.[]").width(180f).wrap(true).row();
        table.row();

        table.add("[white]Módulos activos:[]").row();
        table.check("[cyan]⚡ Energía[]", managePower, v -> managePower = v).row();
        table.check("[red]🛡 Defensa[]", manageDefense, v -> manageDefense = v).row();
        table.check("[orange]📦 Economía[]", manageEconomy, v -> manageEconomy = v).row();
        table.check("[accent]⚔ Unidades[]", manageUnits, v -> manageUnits = v).row();
        table.row();

        table.add("[white]Umbral de energía (%):[]").row();
        table.slider(50f, 100f, 5f, powerThreshold * 100f, v -> powerThreshold = v / 100f).width(160f).row();
        table.row();

        table.add("[white]Umbral de temperatura (%):[]").row();
        table.slider(50f, 99f, 1f, tempThreshold * 100f, v -> tempThreshold = v / 100f).width(160f).row();
        table.row();

        table.add("[white]Máx. llenado de almacén (%):[]").row();
        table.slider(50f, 100f, 5f, storageMaxFill * 100f, v -> storageMaxFill = v / 100f).width(160f).row();
        table.row();

        table.add("[white]Actualizar cada (ticks):[]").row();
        table.slider(5f, 120f, 5f, interval, v -> interval = v).width(160f).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(24).row();
    }

    @Override
    public String toCode() {
        return "# Full Base Autopilot - generado por HiperGPU\n" +
               "# Gestión autónoma completa\n" +
               "# Energía\n" +
               "sensor power power1 @stored\n" +
               "# Defensa\n" +
               "sensor enemies @enemies\n" +
               "control enabled turret1 enemies 0 0 0\n" +
               "# Economía\n" +
               "sensor items vault1 @totalItems\n" +
               "control enabled factory1 items 0 0 0\n" +
               "printflush display1\n";
    }
}
