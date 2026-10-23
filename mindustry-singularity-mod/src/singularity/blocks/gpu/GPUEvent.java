package singularity.blocks.gpu;

import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * GPUEvent — Sistema de ejecución basada en eventos.
 */
public class GPUEvent {

    public enum EventType {
        ENEMY_ENTER, ENEMY_EXIT, POWER_LOW, POWER_RESTORED,
        HEALTH_LOW, WAVE_START, WAVE_END, TIMER
    }

    public final EventType type;
    public final String name, description, example;
    public final int tier;

    public float   threshold  = 50f;
    public boolean active     = true;
    public float   cooldown   = 120f;
    public final Seq<String> actions = new Seq<>();

    private float   cooldownTimer = 0f;
    private boolean lastState     = false;

    public GPUEvent(EventType type, String name, String desc, String ex, int tier) {
        this.type = type;
        this.name = name;
        this.description = desc;
        this.example = ex;
        this.tier = tier;
    }

    public boolean evaluate(LogicBuild build) {
        if (!active) return false;
        cooldownTimer -= Time.delta;
        boolean cur = checkCondition(build);
        boolean fired = false;

        switch (type) {
            case ENEMY_ENTER:
            case POWER_LOW:
            case HEALTH_LOW:
            case WAVE_START:
                fired = cur && !lastState && cooldownTimer <= 0f;
                break;
            case ENEMY_EXIT:
            case POWER_RESTORED:
            case WAVE_END:
                fired = !cur && lastState && cooldownTimer <= 0f;
                break;
            case TIMER:
                fired = cooldownTimer <= 0f;
                break;
        }

        lastState = cur;
        if (fired) cooldownTimer = cooldown;
        return fired;
    }

    private boolean checkCondition(LogicBuild build) {
        switch (type) {
            case ENEMY_ENTER:
            case ENEMY_EXIT:
                for (Unit u : Groups.unit) {
                    if (u.team != build.team && u.within(build.x, build.y, threshold * Vars.tilesize)) {
                        return true;
                    }
                }
                return false;
                
            case POWER_LOW:
            case POWER_RESTORED:
                for (int i = 0; i < build.links.size; i++) {
                    Building b = build.links.get(i).lastBuild;
                    if (b != null && b.power != null && b.power.graph != null) {
                        float p = b.power.graph.getPowerProduced();
                        float c = b.power.graph.getPowerNeeded();
                        return c > 0 ? p / c * 100f < threshold : false;
                    }
                }
                return false;
                
            case HEALTH_LOW:
                for (int i = 0; i < build.links.size; i++) {
                    Building b = build.links.get(i).lastBuild;
                    if (b != null && b.maxHealth() > 0 && b.health / b.maxHealth() * 100f < threshold) {
                        return true;
                    }
                }
                return false;
                
            case WAVE_START:
                return Vars.state.wave > 0 && Vars.state.wavetime < 10f;
                
            case WAVE_END:
                int enemyCount = 0;
                for (Unit u : Groups.unit) {
                    if (u.team != build.team) enemyCount++;
                }
                return enemyCount == 0;
                
            case TIMER:
                return true;
                
            default:
                return false;
        }
    }

    // Lazy init - sin static initializer problemático
    private static Seq<GPUEvent> templates = null;

    public static Seq<GPUEvent> getTemplates() {
        if (templates == null) {
            templates = new Seq<>();
            templates.add(new GPUEvent(EventType.ENEMY_ENTER,  "Enemigo detectado",  "Dispara cuando un enemigo entra en rango", "", 1));
            templates.add(new GPUEvent(EventType.POWER_LOW,    "Energía baja",       "Dispara cuando energía cae bajo umbral",   "", 0));
            templates.add(new GPUEvent(EventType.HEALTH_LOW,   "Bloque dañado",      "Dispara cuando HP baja del umbral",        "", 0));
            templates.add(new GPUEvent(EventType.WAVE_START,   "Inicio de oleada",   "Dispara al inicio de la oleada",           "", 1));
            templates.add(new GPUEvent(EventType.TIMER,        "Temporizador",       "Dispara cada X ticks",                     "", 0));
        }
        return templates;
    }
}
