package singularity.blocks.gpu;

import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;

/**
 * GPUEvent — Ejecución basada en eventos.
 *
 * En vez de un bucle continuo que revisa todo cada tick,
 * los eventos SOLO disparan cuando algo cambia.
 * Esto es mucho más eficiente y fácil de entender.
 *
 * TIPOS DE EVENTO:
 * ─────────────────────────────────────────────────────────────
 * ENEMY_ENTER    — cuando un enemigo entra en rango
 * ENEMY_EXIT     — cuando todos los enemigos salen del rango
 * POWER_LOW      — cuando la energía cae bajo el umbral
 * POWER_RESTORED — cuando la energía se recupera
 * HEALTH_LOW     — cuando algún bloque conectado tiene HP bajo
 * ITEM_LOW       — cuando un item baja del mínimo
 * WAVE_START     — cuando comienza una oleada
 * WAVE_END       — cuando termina una oleada
 * TIMER          — cada X ticks (evento periódico)
 *
 * USO:
 *   Al dispararse un evento, ejecuta las funciones/vecInstructions asignadas.
 */
public class GPUEvent {

    public enum EventType {
        ENEMY_ENTER, ENEMY_EXIT,
        POWER_LOW, POWER_RESTORED,
        HEALTH_LOW,
        ITEM_LOW,
        WAVE_START, WAVE_END,
        TIMER
    }

    public final EventType type;
    public final String    name;
    public final String    description;
    public final String    example;
    public final int       tier;

    // Configuración del evento
    public float threshold    = 50f;   // umbral genérico (%, tiles, ticks)
    public boolean active     = true;  // si este evento está activo
    public float   cooldown   = 120f;  // ticks entre disparos del mismo evento

    // Estado interno
    private float  cooldownTimer = 0f;
    private boolean lastState    = false; // para detectar transición

    // Acciones a ejecutar cuando dispara
    public final Seq<String> actions = new Seq<>(); // IDs de GPUVecInstruction

    public GPUEvent(EventType type, String name, String description,
                    String example, int tier) {
        this.type = type; this.name = name;
        this.description = description; this.example = example;
        this.tier = tier;
    }

    /**
     * Evalúa si el evento debe dispararse.
     * @return true si el evento se disparó en este tick
     */
    public boolean evaluate(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        if (!active) return false;

        cooldownTimer -= Time.delta;

        boolean currentState = checkCondition(build);
        boolean fired = false;

        switch (type) {
            // Dispara en la transición false→true
            case ENEMY_ENTER:
            case POWER_LOW:
            case HEALTH_LOW:
            case ITEM_LOW:
            case WAVE_START:
                fired = currentState && !lastState && cooldownTimer <= 0f;
                break;

            // Dispara en la transición true→false
            case ENEMY_EXIT:
            case POWER_RESTORED:
            case WAVE_END:
                fired = !currentState && lastState && cooldownTimer <= 0f;
                break;

            // Dispara periódicamente mientras se cumple la condición
            case TIMER:
                fired = cooldownTimer <= 0f;
                break;
        }

        lastState = currentState;
        if (fired) cooldownTimer = cooldown;
        return fired;
    }

    private boolean checkCondition(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        switch (type) {
            case ENEMY_ENTER:
            case ENEMY_EXIT:
                for (Unit u : Groups.unit)
                    if (u.team != build.team &&
                        u.within(build.x, build.y, threshold * Vars.tilesize)) return true;
                return false;

            case POWER_LOW:
            case POWER_RESTORED:
                for (int i = 0; i < build.links.size; i++) {
                    LogicLink link = build.links.get(i);
                    Building b = link.lastBuild;
                    if (b != null && b.power != null && b.power.graph != null) {
                        float p = b.power.graph.getPowerProduced();
                        float c = b.power.graph.getPowerNeeded();
                        float cov = c > 0 ? p / c * 100f : 100f;
                        return cov < threshold;
                    }
                }
                return false;

            case HEALTH_LOW:
                for (int i = 0; i < build.links.size; i++) {
                    Building b = build.links.get(i).lastBuild;
                    if (b != null && b.maxHealth() > 0 &&
                        b.health / b.maxHealth() * 100f < threshold) return true;
                }
                return false;

            case WAVE_START:
                return Vars.state.wave > 0 && Vars.state.wavetime < 10f;

            case WAVE_END:
                for (Unit u : Groups.unit)
                    if (u.team != build.team) return false;
                return true;

            case TIMER:
                return true; // siempre activo, controlado por cooldown

            default: return false;
        }
    }

    // ── Registro global ──────────────────────────────────────────────────────
    public static final Seq<GPUEvent> TEMPLATES = new Seq<>();
    static {
        t(EventType.ENEMY_ENTER,    "Enemigo detectado",    "Dispara cuando un enemigo entra en X tiles de la GPU",
          "evento: activar torretas al detectar enemigo", 1);
        t(EventType.ENEMY_EXIT,     "Área despejada",       "Dispara cuando no quedan enemigos en rango",
          "evento: desactivar torretas al despejar área", 1);
        t(EventType.POWER_LOW,      "Energía baja",         "Dispara cuando la cobertura de energía cae bajo el umbral %",
          "evento: apagar fábricas si energía < 60%",     0);
        t(EventType.POWER_RESTORED, "Energía restaurada",   "Dispara cuando la energía vuelve a superar el umbral",
          "evento: reactivar fábricas al recuperar energía", 0);
        t(EventType.HEALTH_LOW,     "Bloque dañado",        "Dispara cuando algún bloque conectado tiene HP bajo el umbral",
          "evento: alerta cuando un bloque está siendo atacado", 0);
        t(EventType.WAVE_START,     "Inicio de oleada",     "Dispara al comienzo de cada oleada enemiga",
          "evento: activar defensa al iniciar oleada",     1);
        t(EventType.WAVE_END,       "Oleada completada",    "Dispara cuando no quedan enemigos vivos",
          "evento: reactivar economía tras la oleada",     1);
        t(EventType.TIMER,          "Temporizador",         "Dispara cada X ticks (configurable). Útil para tareas periódicas",
          "evento: verificar items cada 5 segundos",       0);
    }
    private static void t(EventType type, String name, String desc, String ex, int tier) {
        TEMPLATES.add(new GPUEvent(type, name, desc, ex, tier));
    }
}
