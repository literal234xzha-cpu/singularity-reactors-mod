package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 7: Timer Display
 *
 * Cronómetro configurable que muestra tiempo en displays.
 * Modos: cuenta ascendente, cuenta regresiva, cronómetro de oleada.
 * Puede disparar alertas al llegar a X tiempo.
 */
public class TimerDisplay extends GPUFunction {

    public enum TimerMode { COUNT_UP, COUNT_DOWN, WAVE_TIMER }

    private TimerMode mode         = TimerMode.COUNT_UP;
    private float     countdownFrom = 300f; // segundos para cuenta regresiva
    private boolean   showSeconds  = true;
    private boolean   showMinutes  = true;
    private boolean   alertOnEnd   = true;
    private String    title        = "⏱ TIMER";

    // Estado interno
    private float elapsed   = 0f;
    private boolean running = true;

    private float timer    = 0f;
    private float interval = 20f;

    public TimerDisplay() {
        super("Timer Display",
              "Cronómetro con cuenta ascendente, regresiva o de oleada",
              "⏱", 0);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        if (running) elapsed += Time.delta / 60f; // convertir ticks a segundos
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");

        float display;
        boolean alert = false;

        if (mode == TimerMode.COUNT_DOWN) {
            display = Math.max(0f, countdownFrom - elapsed);
            alert   = alertOnEnd && display <= 10f;
            if (display <= 0f) {
                elapsed = 0f; // reset automático
                sb.append("[scarlet]⚠ TIEMPO AGOTADO[]\n");
            }
        } else {
            display = elapsed;
        }

        int totalSecs = (int)display;
        int mins = totalSecs / 60;
        int secs = totalSecs % 60;

        String timeStr = "";
        if (showMinutes) timeStr += String.format("%02d:", mins);
        if (showSeconds) timeStr += String.format("%02d", secs);
        else             timeStr  = timeStr.replaceAll(":$", "");

        String color = alert ? "scarlet" : (mode == TimerMode.COUNT_DOWN && display < 30f ? "yellow" : "white");
        sb.append("[").append(color).append("]⏱ ").append(timeStr).append("[]\n");

        if (mode == TimerMode.COUNT_UP) {
            // Mostrar también ticks
            sb.append("[gray]Ticks: ").append((int)(elapsed * 60f)).append("[]\n");
        }

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]⏱ Timer Display[]").row();
        table.add("[lightgray]" + description + "[]").row();
        table.row();

        // Modo
        table.add("[white]Modo:[]").row();
        table.button("▲ Ascendente",  () -> mode = TimerMode.COUNT_UP).width(140f).height(28f)
            .checked(b -> mode == TimerMode.COUNT_UP).row();
        table.button("▼ Regresiva",   () -> mode = TimerMode.COUNT_DOWN).width(140f).height(28f)
            .checked(b -> mode == TimerMode.COUNT_DOWN).row();
        table.row();

        // Tiempo para cuenta regresiva
        table.add("[white]Cuenta regresiva desde (s):[]").row();
        table.slider(10f, 3600f, 10f, countdownFrom, v -> countdownFrom = v).width(160f).row();
        table.label(() -> "[white]" + (int)countdownFrom + "s (" + (int)(countdownFrom/60) + "min)[]").row();
        table.row();

        // Controles del timer
        table.table(ctrl -> {
            ctrl.button("[green]▶ Start[]", () -> running = true).size(70f, 28f).padRight(4f);
            ctrl.button("[red]■ Stop[]",  () -> running = false).size(70f, 28f).padRight(4f);
            ctrl.button("[yellow]↺ Reset[]", () -> elapsed = 0f).size(70f, 28f);
        }).row();
        table.row();

        table.check("[white]Mostrar segundos[]", showSeconds, v -> showSeconds = v).row();
        table.check("[white]Mostrar minutos[]",  showMinutes, v -> showMinutes = v).row();
        table.check("[scarlet]Alerta al terminar[]", alertOnEnd, v -> alertOnEnd = v).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(20).row();
    }

    @Override
    public String toCode() {
        return
            "# Timer Display - generado por GPU\n" +
            "op add elapsed elapsed 1\n" +
            "op idiv mins elapsed 3600\n" +
            "op mod secs elapsed 60\n" +
            "print \"" + title + "\\n\"\n" +
            "print mins \":\" secs \"\\n\"\n" +
            "printflush display1\n";
    }
}
