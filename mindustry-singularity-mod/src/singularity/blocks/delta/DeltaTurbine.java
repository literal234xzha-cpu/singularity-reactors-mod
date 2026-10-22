package singularity.blocks.delta;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.power.PowerGenerator;

/**
 * DeltaTurbine -- Convierte el calor del DeltaReactor en electricidad.
 *
 * Busca DeltaReactors en rango corto y consume su heatOutput.
 * RPM sube con mas calor disponible.
 * Desgaste progresivo: a 0% necesita mantenimiento (se repara con items).
 */
public class DeltaTurbine extends PowerGenerator {

    public int reactorRange  = 7;   // tiles para detectar reactores
    public float maxRPM      = 3600f;
    public float rpmAccel    = 20f;  // aceleracion de RPM por tick
    public float rpmDecel    = 10f;  // deceleracion de RPM por tick
    public float wearRate    = 0.002f; // desgaste por tick a max RPM
    public float maxPowerOut = 60000f; // produccion maxima en /s

    public DeltaTurbine(String name) {
        super(name);
        size = 3;
        update = true;
        solid = true;
        hasPower = true;
        outputsPower = true;
        configurable = true;
        saveConfig = false;
        category = Category.power;
        health = 3000;
        powerProduction = maxPowerOut / 60f;
        buildType = DeltaTurbineBuild::new;
    }

    // =========================================================================
    // BUILD
    // =========================================================================

    public class DeltaTurbineBuild extends GeneratorBuild {

        public float rpm          = 0f;
        public float wear         = 1f;    // 1.0 = nuevo, 0.0 = requiere mantenimiento
        public float heatReceived = 0f;
        public float targetRPM    = 0f;

        private int scanTimer = 0;
        private DeltaReactor.DeltaReactorBuild linkedReactor = null;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            wear = 1f;
            return this;
        }

        @Override
        public void updateTile() {
            // Buscar reactor cada 60 ticks
            scanTimer++;
            if (scanTimer >= 60) { scanTimer = 0; findReactor(); }

            // Obtener calor del reactor conectado
            heatReceived = 0f;
            if (linkedReactor != null && linkedReactor.isAdded() && !linkedReactor.scrammed) {
                heatReceived = linkedReactor.heatOutput;
            }

            // Calcular RPM objetivo segun calor recibido
            float heatRatio = Mathf.clamp(heatReceived / linkedReactor_maxHeat());
            targetRPM = heatRatio * maxRPM * wear;

            // Acelerar/desacelerar suavemente
            if (rpm < targetRPM) {
                rpm = Math.min(rpm + rpmAccel * Time.delta, targetRPM);
            } else {
                rpm = Math.max(rpm - rpmDecel * Time.delta, targetRPM);
            }

            // Eficiencia segun RPM
            productionEfficiency = Mathf.clamp(rpm / maxRPM) * wear;

            // Desgaste
            if (rpm > maxRPM * 0.5f) {
                wear -= wearRate * (rpm / maxRPM) * Time.delta;
                wear = Math.max(0f, wear);
            }
        }

        private void findReactor() {
            linkedReactor = null;
            int tx = tileX(), ty = tileY();
            for (int dx = -reactorRange; dx <= reactorRange; dx++) {
                for (int dy = -reactorRange; dy <= reactorRange; dy++) {
                    Building b = Vars.world.build(tx + dx, ty + dy);
                    if (b instanceof DeltaReactor.DeltaReactorBuild && b.team == team) {
                        linkedReactor = (DeltaReactor.DeltaReactorBuild) b;
                        return;
                    }
                }
            }
        }

        private float linkedReactor_maxHeat() {
            if (linkedReactor == null) return 1f;
            return linkedReactor.block instanceof DeltaReactor
                ? ((DeltaReactor)linkedReactor.block).maxHeatOutput
                : 1f;
        }

        @Override
        public float getPowerProduction() {
            return (maxPowerOut / 60f) * productionEfficiency;
        }

        // =========================================================================
        // PANEL UNICO - Mecanico
        // =========================================================================

        @Override
        public void buildConfiguration(Table t) {
            t.defaults().pad(3f);

            t.add(gradient("TURBINA DELTA", 0x0066FF, 0x00CCFF)).center().colspan(2).row();
            t.image().color(Color.valueOf("0088CC")).height(3f).growX().colspan(2).pad(2f).row();

            // Barra RPM
            t.add("[lightgray]RPM:[]").left();
            t.label(() -> {
                String col = rpm > maxRPM*0.8f ? "green" : rpm > maxRPM*0.4f ? "yellow" : "gray";
                return "[" + col + "]" + (int)rpm + " rpm[]";
            }).right().row();

            t.label(() -> buildRPMBar()).colspan(2).center().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.add("[lightgray]Eficiencia:[]").left();
            t.label(() -> {
                int pct = (int)(productionEfficiency * 100f);
                String col = pct > 75 ? "green" : pct > 40 ? "yellow" : "red";
                return "[" + col + "]" + pct + "%[]";
            }).right().row();

            t.add("[lightgray]Generando:[]").left();
            t.label(() -> "[accent]" + fmt(getPowerProduction() * 60f) + "/s[]").right().row();

            t.add("[lightgray]Calor entrada:[]").left();
            t.label(() -> heatReceived > 0
                ? "[orange]" + fmt(heatReceived) + " kJ[]"
                : "[gray]Sin calor[]").right().row();

            t.add("[lightgray]Desgaste:[]").left();
            t.label(() -> {
                int pct = (int)(wear * 100f);
                String col = pct > 60 ? "green" : pct > 30 ? "yellow" : "red";
                return "[" + col + "]" + pct + "%[]";
            }).right().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            t.add("[lightgray]Reactor vinculado:[]").left();
            t.label(() -> linkedReactor != null && linkedReactor.isAdded()
                ? "[green]Conectado[]" : "[red]Sin reactor[]").right().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            // Estado general
            t.label(() -> {
                if (wear < 0.1f) return "[red]REQUIERE MANTENIMIENTO[]";
                if (rpm < maxRPM * 0.1f) return "[gray]En espera[]";
                if (rpm > maxRPM * 0.9f) return "[green]PLENA POTENCIA[]";
                return "[yellow]Operando al " + (int)(productionEfficiency*100f) + "%[]";
            }).colspan(2).center().row();
        }

        private String buildRPMBar() {
            int filled = (int)(Mathf.clamp(rpm / maxRPM) * 20f);
            String col = rpm > maxRPM*0.8f ? "green" : rpm > maxRPM*0.4f ? "yellow" : "gray";
            StringBuilder sb = new StringBuilder("[" + col + "]");
            for (int i = 0; i < 20; i++) sb.append(i < filled ? "|" : ".");
            return sb + "[]";
        }

        private String gradient(String text, int c1, int c2) {
            int r1=(c1>>16)&0xFF,g1=(c1>>8)&0xFF,b1=c1&0xFF;
            int r2=(c2>>16)&0xFF,g2=(c2>>8)&0xFF,b2=c2&0xFF;
            char[] chars=text.toCharArray(); int n=Math.max(chars.length-1,1),ci=0;
            StringBuilder sb=new StringBuilder();
            for (char c : chars) {
                if (c==' ') { sb.append(' '); continue; }
                float tv=ci/(float)n; ci++;
                sb.append(String.format("[#%02X%02X%02X]%c",
                    (int)(r1+(r2-r1)*tv),(int)(g1+(g2-g1)*tv),(int)(b1+(b2-b1)*tv),c));
            }
            return sb+"[]";
        }

        private String fmt(float v) {
            if (v>=1000000f) return Strings.fixed(v/1000000f,2)+"M";
            if (v>=1000f)    return Strings.fixed(v/1000f,1)+"k";
            return Strings.fixed(v,0);
        }

        @Override
        public void write(Writes w) { super.write(w); w.f(rpm); w.f(wear); }

        @Override
        public void read(Reads r, byte rev) { super.read(r,rev); rpm=r.f(); wear=r.f(); }
    }
}
