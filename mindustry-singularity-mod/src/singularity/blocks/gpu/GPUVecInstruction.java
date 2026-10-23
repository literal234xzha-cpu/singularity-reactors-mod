package singularity.blocks.gpu;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.power.PowerGenerator;

/**
 * GPUVecInstruction — Instrucciones vectoriales que operan sobre múltiples bloques linkados.
 */
public class GPUVecInstruction {

    public enum VecCategory { COMBAT, POWER, CONTROL, QUERY }

    public final String id, name, description, example;
    public final VecCategory category;
    public final int tier;

    public GPUVecInstruction(String id, String name, String desc, String ex, VecCategory cat, int tier) {
        this.id = id;
        this.name = name;
        this.description = desc;
        this.example = ex;
        this.category = cat;
        this.tier = tier;
    }

    public double execute(LogicBuild build, String[] args) {
        Seq<Building> targets = linked(build);
        
        if ("turrets.enable".equals(id)) {
            boolean en = args.length > 0 && "true".equals(args[0]);
            int n = 0;
            for (Building b : targets) if (b.block instanceof Turret) { b.enabled = en; n++; }
            return n;
        }
        
        if ("generators.balance".equals(id)) {
            float prod = 0f, cons = 0f;
            for (Building b : targets) {
                if (b.power != null && b.power.graph != null) {
                    prod = b.power.graph.getPowerProduced() * 60f;
                    cons = b.power.graph.getPowerNeeded() * 60f;
                    break;
                }
            }
            float ratio = cons > 0 ? prod / cons : 1f;
            int n = 0;
            for (Building b : targets) {
                if (b.block instanceof PowerGenerator) {
                    if (ratio > 1.2f) b.enabled = false;
                    else if (ratio < 0.9f) b.enabled = true;
                    n++;
                }
            }
            return n;
        }
        
        if ("blocks.enable.all".equals(id))  { for (Building b : targets) b.enabled = true;       return targets.size; }
        if ("blocks.disable.all".equals(id)) { for (Building b : targets) b.enabled = false;      return targets.size; }
        if ("blocks.toggle".equals(id))      { for (Building b : targets) b.enabled = !b.enabled; return targets.size; }
        
        if ("query.health.min".equals(id)) {
            float min = 1f;
            for (Building b : targets) {
                if (b.maxHealth() > 0) min = Math.min(min, b.health / b.maxHealth());
            }
            return min * 100f;
        }
        
        if ("query.enemies.count".equals(id)) {
            float r = args.length > 0 ? Float.parseFloat(args[0]) : 30f;
            int n = 0;
            for (Unit u : Groups.unit) {
                if (u.team != build.team && u.within(build.x, build.y, r * Vars.tilesize)) n++;
            }
            return n;
        }
        
        return 0;
    }

    private Seq<Building> linked(LogicBuild build) {
        Seq<Building> r = new Seq<>();
        for (int i = 0; i < build.links.size; i++) {
            if (build.links.get(i).lastBuild != null) {
                r.add(build.links.get(i).lastBuild);
            }
        }
        return r;
    }

    // Lazy init - sin static initializer problemático
    private static Seq<GPUVecInstruction> all = null;

    public static Seq<GPUVecInstruction> getAll() {
        if (all == null) {
            all = new Seq<>();
            all.add(new GPUVecInstruction("turrets.enable",     "Torretas ON/OFF",    "Activa/desactiva torretas",    "gpuvec turrets.enable true", VecCategory.COMBAT,  1));
            all.add(new GPUVecInstruction("generators.balance", "Balancear generadores", "Balancea según demanda",    "gpuvec generators.balance",  VecCategory.POWER,   1));
            all.add(new GPUVecInstruction("blocks.enable.all",  "Activar todo",       "Habilita todos los bloques", "gpuvec blocks.enable.all",   VecCategory.CONTROL, 0));
            all.add(new GPUVecInstruction("blocks.disable.all", "Desactivar todo",    "Deshabilita todos",          "gpuvec blocks.disable.all",  VecCategory.CONTROL, 0));
            all.add(new GPUVecInstruction("blocks.toggle",      "Invertir estado",    "Invierte ON/OFF",            "gpuvec blocks.toggle",       VecCategory.CONTROL, 0));
            all.add(new GPUVecInstruction("query.health.min",   "HP mínimo",          "% HP del bloque más dañado", "query.health.min",           VecCategory.QUERY,   0));
            all.add(new GPUVecInstruction("query.enemies.count","Contar enemigos",    "Enemigos en X tiles",        "query.enemies.count 30",     VecCategory.QUERY,   0));
        }
        return all;
    }
}
