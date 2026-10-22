package singularity.blocks;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

/**
 * PowerMonitor - Bloque de monitoreo de red eléctrica.
 *
 * Muestra en tiempo real:
 *  - Producción total de la red
 *  - Consumo total de la red
 *  - Balance (superávit / déficit)
 *  - Satisfacción de la red (%)
 *  - Historial de los últimos 120 ticks (2 segundos)
 *  - Alerta visual si hay déficit
 */
public class PowerMonitor extends Block {

    /** Rango de tiles que aparece en el stat de información. */
    public int monitorRange = 20;

    // Colores estáticos compartidos
    public static final Color colorProduction  = Pal.accent.cpy();
    public static final Color colorConsumption = Color.valueOf("f25555");
    public static final Color colorSurplus     = Color.valueOf("84f28f");
    public static final Color colorDeficit     = Color.valueOf("f25555");
    public static final Color colorNeutral     = Color.gray.cpy();

    /** Cuántos ticks de historial se guardan (120 = 2 segundos a 60tps). */
    public int historySize = 120;

    public PowerMonitor(String name) {
        super(name);

        size = 2;
        update = true;
        solid = true;
        hasItems = false;
        hasPower = true;
        consumesPower = false;
        outputsPower = false;
        configurable = true;
        saveConfig = false;

        category = Category.power;
        health = 200;

        buildType = PowerMonitorBuild::new;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, monitorRange, StatUnit.blocks);
    }

    // =========================================================================
    // BUILD (instancia del bloque en el mundo)
    // =========================================================================

    public class PowerMonitorBuild extends Building {

        // Historial circular de balance (producción - consumo) por tick
        public float[] balanceHistory;
        public int historyIndex = 0;
        public int historyCount = 0;

        // Stats calculados cada tick
        public float lastProduction   = 0f;
        public float lastConsumption  = 0f;
        public float lastBalance      = 0f;
        public float lastSatisfaction = 1f;

        // Alerta de déficit
        public float alertTimer = 0f;
        public boolean inDeficit = false;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            balanceHistory = new float[historySize];
            return this;
        }

        @Override
        public void updateTile() {
            if (power == null || power.graph == null) return;

            PowerGraph graph = power.graph;

            // Valores en /tick -> convertir a /segundo (* 60)
            lastProduction   = graph.getPowerProduced()  * 60f;
            lastConsumption  = graph.getPowerNeeded()    * 60f;
            lastBalance      = lastProduction - lastConsumption;
            lastSatisfaction = graph.getSatisfaction();

            inDeficit = lastBalance < -0.5f;

            // Guardar en historial circular
            balanceHistory[historyIndex] = lastBalance;
            historyIndex = (historyIndex + 1) % historySize;
            if (historyCount < historySize) historyCount++;

            if (inDeficit) {
                alertTimer += Time.delta;
            } else {
                alertTimer = 0f;
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            buildUI(table);
        }

        private void buildUI(Table t) {
            t.defaults().pad(4f).left();

            // Título
            t.add("[accent]~ POWER MONITOR[]").center().colspan(2).row();
            t.image().color(Pal.accent).height(2f).growX().colspan(2).pad(2f).row();

            // Producción
            t.add("[lightgray]Producción:[]").left();
            t.label(() -> coloredPower(lastProduction, colorProduction)).right().row();

            // Consumo
            t.add("[lightgray]Consumo:[]").left();
            t.label(() -> coloredPower(lastConsumption, colorConsumption)).right().row();

            // Balance
            t.add("[lightgray]Balance:[]").left();
            t.label(() -> {
                String sign = lastBalance >= 0 ? "+" : "";
                Color c = lastBalance >= 0 ? colorSurplus : colorDeficit;
                return hexColor(c) + sign + formatNum(lastBalance) + "/s[]";
            }).right().row();

            // Satisfacción
            t.add("[lightgray]Satisfacción:[]").left();
            t.label(() -> {
                int pct = (int)(lastSatisfaction * 100f);
                Color c = lastSatisfaction >= 0.99f ? colorSurplus
                        : lastSatisfaction >= 0.5f  ? Color.yellow
                        : colorDeficit;
                return hexColor(c) + pct + "%[]";
            }).right().row();

            t.image().color(colorNeutral).height(1f).growX().colspan(2).pad(2f).row();

            // Estado
            t.label(() -> {
                if (inDeficit) {
                    boolean blink = ((int)(alertTimer / 30f) % 2) == 0;
                    return blink ? "[red]! DÉFICIT DE ENERGÍA[]" : "[scarlet]! DÉFICIT[]";
                } else if (lastSatisfaction >= 0.99f) {
                    return "[green]OK Red estable[]";
                } else {
                    return "[yellow]~ Energía parcial[]";
                }
            }).colspan(2).center().row();

            t.image().color(colorNeutral).height(1f).growX().colspan(2).pad(2f).row();

            // Gráfica de historial
            t.add("[lightgray]Historial (2s):[]").colspan(2).left().row();
            t.add(new HistoryChart(this)).size(200f, 60f).colspan(2).center().pad(4f).row();
        }

        private String coloredPower(float value, Color color) {
            return hexColor(color) + formatNum(value) + "/s[]";
        }

        private String hexColor(Color c) {
            return "[#" + c.toString().substring(0, 6) + "]";
        }

        private String formatNum(float value) {
            float abs = Math.abs(value);
            if (abs >= 1_000_000f) return Strings.fixed(value / 1_000_000f, 2) + "M";
            if (abs >= 1_000f)     return Strings.fixed(value / 1_000f, 1) + "k";
            return Strings.fixed(value, 0);
        }

        @Override
        public boolean shouldConsume() {
            return false;
        }
    }

    // =========================================================================
    // GRÁFICA DE HISTORIAL - clase estática top-level para Table.add()
    // =========================================================================

    public static class HistoryChart extends Element {

        private final PowerMonitorBuild build;

        public HistoryChart(PowerMonitorBuild build) {
            this.build = build;
            setSize(200f, 60f);
        }

        @Override
        public void draw() {
            if (build.historyCount < 2) return;

            // En Arc, Element tiene campos públicos x, y, width, height
            float ex = x, ey = y, ew = width, eh = height;

            // Fondo semitransparente
            Draw.color(0f, 0f, 0f, 0.5f);
            Fill.rect(ex + ew / 2f, ey + eh / 2f, ew, eh);

            // Escala máxima del historial
            float maxAbs = 1f;
            for (int i = 0; i < build.historyCount; i++) {
                float abs = Math.abs(build.balanceHistory[i]);
                if (abs > maxAbs) maxAbs = abs;
            }

            // Línea cero
            Draw.color(Color.gray, 0.5f);
            Lines.line(ex, ey + eh / 2f, ex + ew, ey + eh / 2f);

            int count  = build.historyCount;
            int bufLen = build.balanceHistory.length;
            float stepX = ew / (float)(bufLen - 1);

            for (int i = 0; i < count - 1; i++) {
                int idxA = (build.historyIndex - count + i     + bufLen) % bufLen;
                int idxB = (build.historyIndex - count + i + 1 + bufLen) % bufLen;

                float valA = build.balanceHistory[idxA];
                float valB = build.balanceHistory[idxB];

                float px1 = ex + i * stepX;
                float py1 = ey + eh / 2f + (valA / maxAbs) * (eh / 2f - 2f);
                float px2 = ex + (i + 1) * stepX;
                float py2 = ey + eh / 2f + (valB / maxAbs) * (eh / 2f - 2f);

                Color lineColor = (valA >= 0 && valB >= 0) ? colorSurplus
                                : (valA <  0 && valB <  0) ? colorDeficit
                                : Color.yellow;

                Draw.color(lineColor);
                Lines.line(px1, py1, px2, py2);
            }

            Draw.color(Color.white);
        }
    }
}
