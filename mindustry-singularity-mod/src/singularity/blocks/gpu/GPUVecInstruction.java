package singularity.blocks.gpu;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.logic.LogicBlock.LogicLink;
import mindustry.world.blocks.power.PowerGenerator;

/**
 * GPUVecInstruction — Instrucciones vectoriales.
 * Una instrucción opera sobre TODOS los bloques conectados a la vez.
 */
public class GPUVecInstruction {

    public enum VecCategory { COMBAT, POWER, CONTROL, QUERY }

    public final String id, name, description, example;
    public final VecCategory category;
    public final int tier; // 0=MicroGPU, 1=GPU, 2=HiperGPU

    public GPUVecInstruction(String id, String name, String description,
                              String example, VecCategory category, int tier) {
        this.id = id; this.name = name;
        this.description = description; this.example = example;
        this.category = category; this.tier = tier;
    }

    public double execute(MicroGPU.MicroGPUBuild build, String[] args) {
        Seq<Building> targets = linked(build);
        switch (id) {

            // COMBAT
            case "turrets.enable": {
                boolean en = args.length > 0 && args[0].equals("true");
                int n = 0;
                for (Building b : targets) if (b.block instanceof Turret) { b.enabled = en; n++; }
                return n;
            }
            case "turrets.aim": {
                Unit enemy = closestEnemy(build);
                if (enemy == null) return 0;
                int n = 0;
                for (Building b : targets)
                    if (b.block instanceof Turret) {
                        b.enabled = b.dst(enemy) < ((Turret)b.block).range * Vars.tilesize * 1.2f;
                        n++;
                    }
                return n;
            }
            case "turrets.prioritize.air": {
                boolean hasAir = false;
                for (Unit u : Groups.unit)
                    if (u.team != build.team && u.type.flying &&
                        u.within(build.x, build.y, 80f * Vars.tilesize)) { hasAir = true; break; }
                int n = 0;
                for (Building b : targets) if (b.block instanceof Turret) { b.enabled = hasAir; n++; }
                return hasAir ? n : 0;
            }

            // POWER
            case "generators.balance": {
                float prod = 0f, cons = 0f;
                for (Building b : targets)
                    if (b.power != null && b.power.graph != null) {
                        prod = b.power.graph.getPowerProduced() * 60f;
                        cons = b.power.graph.getPowerNeeded() * 60f;
                        break;
                    }
                float ratio = cons > 0 ? prod / cons : 1f;
                int n = 0;
                for (Building b : targets)
                    if (b.block instanceof PowerGenerator) {
                        if (ratio > 1.2f) b.enabled = false;
                        else if (ratio < 0.9f) b.enabled = true;
                        n++;
                    }
                return n;
            }
            case "generators.enable.all": {
                int n = 0;
                for (Building b : targets) if (b.block instanceof PowerGenerator) { b.enabled = true; n++; }
                return n;
            }
            case "generators.disable.all": {
                int n = 0;
                for (Building b : targets) if (b.block instanceof PowerGenerator) { b.enabled = false; n++; }
                return n;
            }

            // CONTROL
            case "blocks.enable.all":  { for (Building b : targets) b.enabled = true;          return targets.size; }
            case "blocks.disable.all": { for (Building b : targets) b.enabled = false;         return targets.size; }
            case "blocks.toggle":      { for (Building b : targets) b.enabled = !b.enabled;    return targets.size; }

            // QUERY
            case "query.health.min": {
                float min = 1f;
                for (Building b : targets)
                    if (b.maxHealth() > 0) min = Math.min(min, b.health / b.maxHealth());
                return min * 100f;
            }
            case "query.health.avg": {
                float sum = 0f; int n = 0;
                for (Building b : targets)
                    if (b.maxHealth() > 0) { sum += b.health / b.maxHealth(); n++; }
                return n > 0 ? sum / n * 100f : 100f;
            }
            case "query.power.coverage": {
                for (Building b : targets)
                    if (b.power != null && b.power.graph != null) {
                        float p = b.power.graph.getPowerProduced();
                        float c = b.power.graph.getPowerNeeded();
                        return c > 0 ? p / c * 100f : 100f;
                    }
                return 100f;
            }
            case "query.enemies.count": {
                float r = args.length > 0 ? Float.parseFloat(args[0]) : 30f;
                int n = 0;
                for (Unit u : Groups.unit)
                    if (u.team != build.team && u.within(build.x, build.y, r * Vars.tilesize)) n++;
                return n;
            }
            case "query.linked.count": return targets.size;
        }
        return 0;
    }

    private Seq<Building> linked(MicroGPU.MicroGPUBuild build) {
        Seq<Building> r = new Seq<>();
        for (int i = 0; i < build.links.size; i++) {
            LogicLink l = build.links.get(i);
            if (l.lastBuild != null) r.add(l.lastBuild);
        }
        return r;
    }

    private Unit closestEnemy(MicroGPU.MicroGPUBuild build) {
        Unit closest = null; float minD = Float.MAX_VALUE;
        for (Unit u : Groups.unit) {
            if (u.team == build.team) continue;
            float d = u.dst(build.x, build.y);
            if (d < minD) { minD = d; closest = u; }
        }
        return closest;
    }

    // ── Registro global ──────────────────────────────────────────────────────
    public static final Seq<GPUVecInstruction> ALL = new Seq<>();
    static {
        r("turrets.enable",         "Torretas ON/OFF",          "Activa o desactiva todas las torretas conectadas",
          "gpuvec turrets.enable true",          VecCategory.COMBAT,  1);
        r("turrets.aim",            "Apuntar al enemigo",       "Activa torretas en rango del enemigo más cercano",
          "gpuvec turrets.aim",                  VecCategory.COMBAT,  1);
        r("turrets.prioritize.air", "Priorizar aéreos",         "Activa torretas si hay unidades aéreas enemigas",
          "gpuvec turrets.prioritize.air",       VecCategory.COMBAT,  1);
        r("generators.balance",     "Balancear generadores",    "Enciende/apaga generadores según ratio prod/cons",
          "gpuvec generators.balance",           VecCategory.POWER,   1);
        r("generators.enable.all",  "Activar generadores",      "Enciende todos los generadores conectados",
          "gpuvec generators.enable.all",        VecCategory.POWER,   1);
        r("generators.disable.all", "Apagar generadores",       "Apaga todos los generadores conectados",
          "gpuvec generators.disable.all",       VecCategory.POWER,   1);
        r("blocks.enable.all",      "Activar todo",             "Habilita todos los bloques conectados",
          "gpuvec blocks.enable.all",            VecCategory.CONTROL, 0);
        r("blocks.disable.all",     "Desactivar todo",          "Deshabilita todos los bloques conectados",
          "gpuvec blocks.disable.all",           VecCategory.CONTROL, 0);
        r("blocks.toggle",          "Invertir estado",          "Invierte ON/OFF de todos los bloques conectados",
          "gpuvec blocks.toggle",                VecCategory.CONTROL, 0);
        r("query.health.min",       "HP mínimo",                "% HP del bloque más dañado de los conectados",
          "set minHP (gpuvec query.health.min)", VecCategory.QUERY,   0);
        r("query.health.avg",       "HP promedio",              "Promedio de HP de todos los bloques conectados",
          "set avg (gpuvec query.health.avg)",   VecCategory.QUERY,   0);
        r("query.power.coverage",   "Cobertura eléctrica",      "% de cobertura de la red (prod/cons×100)",
          "set cov (gpuvec query.power.coverage)",VecCategory.QUERY,  0);
        r("query.enemies.count",    "Contar enemigos",          "Enemigos en X tiles de radio desde la GPU",
          "set n (gpuvec query.enemies.count 30)",VecCategory.QUERY,  0);
        r("query.linked.count",     "Contar bloques",           "Cuántos bloques están conectados a esta GPU",
          "set n (gpuvec query.linked.count)",   VecCategory.QUERY,   0);
    }
    private static void r(String id, String name, String desc, String ex, VecCategory cat, int tier) {
        ALL.add(new GPUVecInstruction(id, name, desc, ex, cat, tier));
    }
}
