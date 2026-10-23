package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 29: PID Controller
 *
 * Control proporcional-integral-derivativo de temperatura del Delta Reactor.
 *
 * Disponible desde: HiperGPU (tier 2)
 */
public class PIDController extends GPUFunction {

    private String title = "🎛 PID Controller";
    private float  timer    = 0f;
    private float  interval = 5f;

    private float kp = 2.0f;
    private float ki = 0.5f;
    private float kd = 1.0f;
    private float setpoint = 0.7f;

    private float integral = 0f;
    private float prevError = 0f;

    public PIDController() {
        super("PID Controller", "Control proporcional-integral-derivativo de temperatura del Delta Reactor.", "🎛", 2);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append("[lightgray]Target: ").append((int)(setpoint * 100)).append("%[]\n");

        for (int i = 0; i < build.links.size; i++) {
            LogicLink link = build.links.get(i);
            Building b = link.lastBuild;
            if (b == null) continue;

            // Aproximar temperatura con el ratio de producción/consumo de energía
            float currentTemp = getTemp(b);
            if (currentTemp < 0) continue;

            // Calcular error
            float error = setpoint - currentTemp;

            // PID
            float p = kp * error;
            integral += error * (interval / 60f);
            float maxIntegral = 1f / Math.max(ki, 0.01f);
            integral = Math.max(-maxIntegral, Math.min(maxIntegral, integral));
            float in = ki * integral;
            float derivative = (error - prevError) / Math.max(interval / 60f, 0.01f);
            float d = kd * derivative;
            prevError = error;

            float output = p + in + d;
            output = Math.max(0f, Math.min(1f, output));

            // Aplicar control
            b.enabled = output > 0.5f;

            sb.append("[white]").append(link.name).append(": ")
              .append((int)(currentTemp * 100)).append("% -> ")
              .append("[cyan]").append((int)(output * 100)).append("[]\n");
        }

        if (build.links.isEmpty()) {
            sb.append("[lightgray]Sin reactores conectados[]");
        }

        printToDisplays(build, sb.toString());
    }

    private float getTemp(Building b) {
        try {
            if (b.power != null && b.power.graph != null) {
                float prod = b.power.graph.getPowerProduced();
                float need = b.power.graph.getPowerNeeded();
                if (need > 0) return Math.min(1f, prod / need);
            }
        } catch (Exception ignored) {}
        return -1f;
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]🎛 PID Controller[]").row();
        table.add("[lightgray]Control proporcional-integral-derivativo de temperatura del Delta Reactor.[]").width(180f).wrap(true).row();
        table.row();

        table.add("[white]Setpoint (%):[]").row();
        table.slider(20f, 95f, 1f, setpoint * 100f, v -> setpoint = v / 100f).width(160f).row();
        table.label(() -> "[white]" + (int)(setpoint * 100) + "%[]").row();
        table.row();

        table.add("[white]Kp (Proporcional):[]").row();
        table.slider(0f, 10f, 0.1f, kp, v -> kp = v).width(160f).row();
        table.row();

        table.add("[white]Ki (Integral):[]").row();
        table.slider(0f, 5f, 0.1f, ki, v -> ki = v).width(160f).row();
        table.row();

        table.add("[white]Kd (Derivativo):[]").row();
        table.slider(0f, 10f, 0.1f, kd, v -> kd = v).width(160f).row();
        table.row();

        table.add("[white]Verificar cada (ticks):[]").row();
        table.slider(1f, 30f, 1f, interval, v -> interval = v).width(160f).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(24).row();
    }

    @Override
    public String toCode() {
        return "# PID Controller - generado por HiperGPU\n" +
               "# Control PID de temperatura\n" +
               "sensor temp reactor1 @heat\n" +
               "op sub error 0.7 temp\n" +
               "op mul output error 2.0\n" +
               "control enabled reactor1 output 0 0 0\n" +
               "printflush display1\n";
    }
}
