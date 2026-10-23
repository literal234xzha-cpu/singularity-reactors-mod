package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import singularity.blocks.gpu.MicroGPU;

/**
 * Función 30: Wave Defender
 *
 * Activa defensa completa automáticamente al detectar inicio de oleada.
 *
 * Disponible desde: HiperGPU (tier 2)
 */
public class WaveDefender extends GPUFunction {

    private String title = "🛡 Wave Defender";
    private float  timer    = 0f;
    private float  interval = 10f;

    private boolean autoActivate = true;
    private boolean lastWaveActive = false;

    public WaveDefender() {
        super("Wave Defender", "Activa defensa completa automáticamente al detectar inicio de oleada.", "🛡", 2);
    }

    @Override
    public void update(MicroGPU.MicroGPUBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        // Detectar oleada usando Vars.state
        boolean waveActive = false;
        try {
            waveActive = Vars.state.wave > 0;
            if (waveActive) {
                // Verificar si hay enemigos reales en el mapa
                int count = 0;
                for (mindustry.gen.Unit u : mindustry.gen.Groups.unit) {
                    if (u.team != build.team) { count++; break; }
                }
                waveActive = count > 0;
            }
        } catch (Exception ignored) {}

        boolean waveJustStarted = waveActive && !lastWaveActive;
        boolean waveJustEnded = !waveActive && lastWaveActive;

        if (waveJustStarted) {
            sb.append("[red]⚠ OLEADA DETECTADA![]\n");
            sb.append("[yellow]Activando defensas...[]\n");
        } else if (waveActive) {
            sb.append("[orange]⚔ Oleada en curso[]\n");
        } else {
            sb.append("[green]✓ Sin oleada[]\n");
        }

        int turretsActivated = 0;
        int totalTurrets = 0;

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;

            totalTurrets++;

            if (autoActivate) {
                if (waveActive) {
                    b.enabled = true;
                    turretsActivated++;
                } else if (waveJustEnded) {
                    b.enabled = false;
                }
            } else {
                if (b.enabled) turretsActivated++;
            }
        }

        sb.append("[white]Torretas: ").append(turretsActivated).append("/").append(totalTurrets).append("[]\n");

        int enemyCount = 0;
        try {
            for (mindustry.gen.Unit u : mindustry.gen.Groups.unit) {
                if (u.team != build.team) enemyCount++;
            }
        } catch (Exception ignored) {}
        sb.append("[red]Enemigos: ").append(enemyCount).append("[]\n");

        if (build.links.isEmpty()) {
            sb.append("[lightgray]Sin bloques conectados[]");
        }

        lastWaveActive = waveActive;
        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, MicroGPU.MicroGPUBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]🛡 Wave Defender[]").row();
        table.add("[lightgray]Activa defensa completa automáticamente al detectar inicio de oleada.[]").width(180f).wrap(true).row();
        table.row();

        table.check("[white]Activación automática[]", autoActivate, v -> autoActivate = v).row();
        table.row();

        table.add("[white]Verificar cada (ticks):[]").row();
        table.slider(1f, 60f, 1f, interval, v -> interval = v).width(160f).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(24).row();
    }

    @Override
    public String toCode() {
        return "# Wave Defender - generado por HiperGPU\n" +
               "# Activa defensas al detectar oleada\n" +
               "sensor wavecounter @wave\n" +
               "sensor enemies @enemies\n" +
               "op greaterThan wave enemies 0\n" +
               "control enabled turret1 1 0 0 0\n" +
               "printflush display1\n";
    }
}
