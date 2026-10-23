package singularity.blocks.gpu.functions;

import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import singularity.blocks.gpu.GPUFunction;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;

/**
 * Función 5: Unit Counter
 *
 * Cuenta unidades aliadas y enemigas en rango.
 * Muestra totales por tipo: ground, air, boss.
 * Alerta si hay enemigos en rango.
 */
public class UnitCounter extends GPUFunction {

    private float  watchRadius   = 40f;  // tiles
    private boolean countAllies  = true;
    private boolean countEnemies = true;
    private boolean alertEnemies = true;
    private String  title        = "⚔ UNITS";

    private float timer    = 0f;
    private float interval = 60f;

    public UnitCounter() {
        super("Unit Counter",
              "Cuenta unidades aliadas y enemigas en rango",
              "⚔", 0);
    }

    @Override
    public void update(mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        timer += Time.delta;
        if (timer < interval) return;
        timer = 0f;

        float wx = build.x;
        float wy = build.y;
        float r  = watchRadius * Vars.tilesize;

        int alliesGround = 0, alliesAir = 0;
        int enemyGround  = 0, enemyAir  = 0;

        for (Unit u : Groups.unit) {
            if (!u.within(wx, wy, r)) continue;
            boolean ally = u.team == build.team;
            if (ally && countAllies) {
                if (u.type.flying) alliesAir++;
                else               alliesGround++;
            } else if (!ally && countEnemies) {
                if (u.type.flying) enemyAir++;
                else               enemyGround++;
            }
        }

        int totalEnemies = enemyGround + enemyAir;
        boolean danger   = alertEnemies && totalEnemies > 0;

        StringBuilder sb = new StringBuilder();
        sb.append(danger ? "[scarlet]" : "[white]").append(title).append("[]\n");

        if (countAllies) {
            sb.append("[green]ALIADOS:[]\n");
            sb.append("[green] ▣ ").append(alliesGround).append(" terrestre[]\n");
            sb.append("[green] ✈ ").append(alliesAir).append(" aéreo[]\n");
        }

        if (countEnemies) {
            String ec = totalEnemies > 0 ? "scarlet" : "gray";
            sb.append("[").append(ec).append("]ENEMIGOS:[]\n");
            sb.append("[").append(ec).append("] ▣ ").append(enemyGround).append(" terrestre[]\n");
            sb.append("[").append(ec).append("] ✈ ").append(enemyAir).append(" aéreo[]\n");
        }

        if (danger)
            sb.append("\n[scarlet]⚠ CONTACTO ENEMIGO[]");

        printToDisplays(build, sb.toString());
    }

    @Override
    public void buildConfig(Table table, mindustry.world.blocks.logic.LogicBlock.LogicBuild build) {
        table.defaults().left().padBottom(4f);
        table.add("[cyan]⚔ Unit Counter[]").row();
        table.add("[lightgray]" + description + "[]").row();
        table.row();

        table.add("[white]Radio de detección (tiles):[]").row();
        table.slider(5f, 100f, 5f, watchRadius, v -> watchRadius = v).width(160f).row();
        table.label(() -> "[white]" + (int)watchRadius + " tiles[]").row();
        table.row();

        table.check("[green]Contar aliados[]",  countAllies,  v -> countAllies  = v).row();
        table.check("[red]Contar enemigos[]",   countEnemies, v -> countEnemies = v).row();
        table.check("[scarlet]Alerta enemigos[]", alertEnemies, v -> alertEnemies = v).row();
        table.row();

        table.add("[lightgray]Título:[]").row();
        table.field(title, v -> title = v).width(160f).maxTextLength(20).row();
    }

    @Override
    public String toCode() {
        return
            "# Unit Counter - generado por GPU\n" +
            "uradar ally any any any 0 health allies\n" +
            "uradar enemy any any any 0 health enemies\n" +
            "print \"" + title + "\\n\"\n" +
            "print \"Aliados: \" allies \"\\n\"\n" +
            "print \"Enemigos: \" enemies \"\\n\"\n" +
            "printflush display1\n";
    }
}
