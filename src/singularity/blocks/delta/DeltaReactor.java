package singularity.blocks.delta;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Liquids;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.graphics.Pal;
import mindustry.type.Category;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

/**
 * DeltaReactor -- Nucleo del Sistema Delta.
 *
 * Genera calor que las DeltaTurbines convierten en electricidad.
 * Temperatura operativa: 200-1200C
 * - < 200C: no genera calor util
 * - 200-800C: zona optima, eficiencia 100%
 * - 800-1000C: sobretemperatura, eficiencia baja, daño progresivo
 * - > 1000C: MELTDOWN en 30s
 *
 * Requiere agua/criofluido como refrigerante.
 * El calor generado se almacena en heatOutput para que DeltaTurbine lo lea.
 */
public class DeltaReactor extends Block {

    // Parametros termicos
    public float minTemp       = 200f;   // temperatura minima para generar
    public float optimalTemp   = 600f;   // temperatura optima
    public float maxSafeTemp   = 800f;   // inicio de sobretemperatura
    public float criticalTemp  = 1000f;  // inicio de meltdown
    public float absoluteMax   = 1200f;  // temperatura maxima absoluta

    public float heatGenRate   = 4.0f;    // calor generado por tick (equilibrio natural)
    public float coolRate      = 0.3f;    // consumo moderado de refrigerante
    public float passiveCool   = 0.15f;   // enfriamiento pasivo por tick
    public float heatRiseRate  = 1.5f;   // subida de temperatura por tick sin refrigerante
    public float maxHeatOutput = 5000f;  // calor maximo que puede exportar

    // Vida util
    public float maxHealth2    = 1000f;  // vida util interna (no HP)
    public float damageRate    = 0.1f;   // daño por tick sobre maxSafeTemp
    public float meltdownTime  = 60f * 30f; // 30 segundos para meltdown desde criticalTemp

    public TextureRegion rotatorRegion;
    public TextureRegion topRegion;

    public DeltaReactor(String name) {
        super(name);
        size = 5;
        update = true;
        solid = true;
        hasPower = false;
        hasLiquids = true;
        liquidCapacity = 100f;
        configurable = true;
        saveConfig = false;
        category = Category.power;
        health = 5000;
        buildType = DeltaReactorBuild::new;
    }

    @Override
    public void load() {
        super.load();
        rotatorRegion = Core.atlas.find(name + "-rotator");
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void setStats() {
        super.setStats();
    }

    // =========================================================================
    // BUILD
    // =========================================================================

    public class DeltaReactorBuild extends Building {

        public float temperature  = 20f;   // temperatura actual (Celsius)
        public float heatOutput   = 0f;    // calor disponible para turbinas este tick
        public float internalLife = 1000f; // vida util interna
        public float meltTimer    = 0f;    // contador hasta meltdown
        public boolean scrammed   = false; // apagado de emergencia manual
        public boolean autoMode   = true;  // modo automatico

        // Stats para el panel
        public float coolantFlow  = 0f;
        public float heatGenerated = 0f;

        public float coreSpin = 0f;

        @Override
        public Building init(Tile tile, Team team, boolean lastConfig, int rotation) {
            super.init(tile, team, lastConfig, rotation);
            temperature = 20f;
            internalLife = maxHealth2;
            return this;
        }

        @Override
        public void updateTile() {
            if (scrammed || !enabled) {
                // Enfriamiento pasivo y con refrigerante cuando esta apagado
                coolantFlow = 0f;
                if (liquids != null && liquids.currentAmount() > 0.01f) {
                    Liquid coolant = liquids.current();
                    float consumed = Math.min(liquids.currentAmount(), 0.2f * Time.delta);
                    liquids.remove(coolant, consumed);
                    coolantFlow = consumed * 60f;
                    temperature = Math.max(20f, temperature - 6f * Time.delta);
                } else {
                    temperature = Math.max(20f, temperature - passiveCool * 2f * Time.delta);
                }
                heatOutput = 0f;
                meltTimer = 0f;
                heatGenerated = 0f;
                return;
            }

            // Generacion de calor continuo del combustible
            float fuelEff = shouldConsume() ? 1f : 0f;
            if (fuelEff > 0.01f) {
                temperature += heatGenRate * fuelEff * Time.delta;
                heatGenerated = heatGenRate * fuelEff * 60f * 20f;
            } else {
                heatGenerated = 0f;
            }

            // Refrigeracion con liquidos
            coolantFlow = 0f;
            if (liquids != null) {
                Liquid coolant = liquids.current();
                float amount = liquids.currentAmount();
                if (amount > 0.01f && (coolant == Liquids.water || coolant == Liquids.cryofluid)) {
                    // Temperatura objetivo de equilibrio con este refrigerante:
                    // Agua: equilibrio ideal a 640C (zona optima 600-750C)
                    // Criofluido: mas frio y ultra-estable a 480C
                    float targetTemp = (coolant == Liquids.cryofluid) ? 480f : 640f;

                    // Si el nucleo esta por debajo de 100C (punto de ebullicion),
                    // el refrigerante no absorbe calor excesivo para permitir arranque rapido.
                    if (temperature > 100f) {
                        float coolMultiplier = (coolant == Liquids.cryofluid) ? 2.0f : 1.0f;
                        float coolingEffect;

                        if (temperature <= targetTemp) {
                            // Subiendo hacia el equilibrio optimo:
                            // La extraccion termica es menor que la generacion para que el reactor
                            // logre subir y asentarse en la temperatura operativa ideal.
                            float ratio = (temperature - 100f) / (targetTemp - 100f);
                            coolingEffect = heatGenRate * ratio * 0.95f;
                        } else {
                            // Sobre la temperatura de equilibrio:
                            // El refrigerante disipa mas calor del que genera, forzando la estabilizacion
                            coolingEffect = heatGenRate + (temperature - targetTemp) * 0.05f * coolMultiplier;
                        }

                        temperature -= coolingEffect * Time.delta;

                        // Consumo continuo de refrigerante proporcional al calor disipado
                        float useBase = (coolant == Liquids.cryofluid) ? 0.12f : 0.22f;
                        float useMax = (coolant == Liquids.cryofluid) ? 0.28f : 0.45f;
                        float flowRate = Mathf.lerp(useBase, useMax, Mathf.clamp((temperature - 100f) / 700f));
                        float consumed = Math.min(amount, flowRate * Time.delta);
                        liquids.remove(coolant, consumed);
                        coolantFlow = consumed * 60f;
                    }
                }
            }

            // Enfriamiento pasivo ambiental minimo
            if (temperature > 20f) {
                temperature -= passiveCool * Time.delta;
            }
            temperature = Math.max(20f, temperature);

            // Calor exportable a turbinas (a partir de minTemp=200C)
            if (temperature >= minTemp) {
                float ratio = Mathf.clamp((temperature - minTemp) / (optimalTemp - minTemp));
                heatOutput = ratio * maxHeatOutput * fuelEff;
            } else {
                heatOutput = 0f;
            }

            // Sobretemperatura: daño a vida util
            if (temperature > maxSafeTemp) {
                float excess = (temperature - maxSafeTemp) / (criticalTemp - maxSafeTemp);
                internalLife -= damageRate * excess * Time.delta;
                internalLife = Math.max(0f, internalLife);
            }

            // Temperatura critica: cuenta regresiva para meltdown
            if (temperature >= criticalTemp) {
                meltTimer += Time.delta;
                if (meltTimer >= meltdownTime) {
                    triggerMeltdown();
                }
            } else {
                meltTimer = Math.max(0f, meltTimer - Time.delta * 0.5f);
            }

            // Temperatura maxima absoluta
            temperature = Math.min(temperature, absoluteMax);
        }

        @Override
        public void draw() {
            Draw.rect(region, x, y);

            if (temperature > 50f) {
                float spinSpeed = Mathf.clamp((temperature - 20f) / 600f) * 1.5f;
                coreSpin += spinSpeed * Time.delta;
            }

            if (rotatorRegion != null && rotatorRegion.found()) {
                Draw.rect(rotatorRegion, x, y, coreSpin);
            }

            // Brillo termico incandescente del nucleo
            if (temperature > 200f) {
                float heatFrac = Mathf.clamp((temperature - 200f) / 800f);
                Draw.color(Color.valueOf("ff6a00"), Color.valueOf("ff2200"), heatFrac);
                Draw.alpha(heatFrac * 0.7f);
                if (rotatorRegion != null && rotatorRegion.found()) {
                    Draw.rect(rotatorRegion, x, y, -coreSpin * 0.5f);
                }
                Draw.color(); // Reset color
            }

            if (topRegion != null && topRegion.found()) {
                Draw.rect(topRegion, x, y);
            }
        }

        private void triggerMeltdown() {
            // Explosion y daño en area
            tile.getLinkedTiles(t -> {
                if (t != null) damage(9999f);
            });
            kill();
        }

        // =========================================================================
        // PANEL UNICO - Nucleo
        // =========================================================================

        @Override
        public void buildConfiguration(Table t) {
            t.defaults().pad(3f);

            // Titulo
            t.add(gradient("REACTOR DELTA", 0xFF4400, 0xFF8800)).center().colspan(2).row();
            t.image().color(tempColor()).height(3f).growX().colspan(2).pad(2f).row();

            // Barra de temperatura
            t.add("[lightgray]Temperatura:[]").left();
            t.label(() -> {
                String col = tempColorName(temperature);
                return "[" + col + "]" + (int)temperature + "C[]";
            }).right().row();

            // Barra visual de temperatura
            t.add("[lightgray]" + buildTempBar() + "[]").colspan(2).center().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            // Stats
            t.add("[lightgray]Calor generado:[]").left();
            t.label(() -> "[orange]" + fmt(heatGenerated) + " kJ/s[]").right().row();

            t.add("[lightgray]Refrigerante:[]").left();
            t.label(() -> coolantFlow > 0.01f
                ? "[cyan]" + fmt(coolantFlow) + " u/s[]"
                : "[red]SIN REFRIGERANTE[]").right().row();

            t.add("[lightgray]Vida util:[]").left();
            t.label(() -> {
                int pct = (int)(internalLife / maxHealth2 * 100f);
                String col = pct > 60 ? "green" : pct > 30 ? "yellow" : "red";
                return "[" + col + "]" + pct + "%[]";
            }).right().row();

            if (meltTimer > 0f) {
                t.label(() -> {
                    int sec = (int)((meltdownTime - meltTimer) / 60f);
                    boolean blink = ((int)(meltTimer / 20f) % 2) == 0;
                    return blink ? "[red]!! MELTDOWN EN " + sec + "s !![]" : "[scarlet]!! MELTDOWN EN " + sec + "s !![]";
                }).colspan(2).center().row();
            }

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            // Estado
            t.label(() -> {
                if (scrammed) return "[red]SCRAM ACTIVO[]";
                if (temperature < minTemp) return "[gray]Calentando...[]";
                if (temperature > criticalTemp) return "[red]!! CRITICO !![]";
                if (temperature > maxSafeTemp) return "[orange]SOBRETEMPERATURA[]";
                return "[green]OPERANDO[]";
            }).colspan(2).center().row();

            t.image().color(Color.gray).height(1f).growX().colspan(2).pad(2f).row();

            // Botones
            t.table(bt -> {
                bt.button(btn -> {
                    btn.clearChildren();
                    btn.add(new arc.scene.ui.Label(scrammed ? "[green]REACTIVAR[]" : "[red]SCRAM[]")).center();
                }, () -> {
                    scrammed = !scrammed;
                    if (!scrammed) meltTimer = 0f;
                }).size(90f, 32f).pad(3f);

                bt.button(btn -> {
                    btn.clearChildren();
                    btn.add(new arc.scene.ui.Label(autoMode ? "[accent]AUTO[]" : "[gray]MANUAL[]")).center();
                }, () -> autoMode = !autoMode).size(80f, 32f).pad(3f);
            }).colspan(2).center().row();
        }

        private String buildTempBar() {
            int filled = (int)(Mathf.clamp(temperature / absoluteMax) * 20f);
            StringBuilder sb = new StringBuilder("[");
            String col = tempColorName(temperature);
            sb.append("[").append(col).append("]");
            for (int i = 0; i < 20; i++) {
                sb.append(i < filled ? "|" : ".");
            }
            sb.append("[]");
            sb.append("]");
            return sb.toString();
        }

        private Color tempColor() {
            if (temperature > criticalTemp) return Color.red;
            if (temperature > maxSafeTemp)  return Color.orange;
            if (temperature > optimalTemp)  return Color.yellow;
            if (temperature > minTemp)      return Color.green;
            return Color.cyan;
        }

        private String tempColorName(float t) {
            if (t > criticalTemp) return "red";
            if (t > maxSafeTemp)  return "orange";
            if (t > optimalTemp)  return "yellow";
            if (t > minTemp)      return "green";
            return "cyan";
        }

        private String gradient(String text, int c1, int c2) {
            int r1=(c1>>16)&0xFF,g1=(c1>>8)&0xFF,b1=c1&0xFF;
            int r2=(c2>>16)&0xFF,g2=(c2>>8)&0xFF,b2=c2&0xFF;
            char[] chars=text.toCharArray(); int n=Math.max(chars.length-1,1),ci=0;
            StringBuilder sb=new StringBuilder();
            for (char c : chars) {
                if (c==' ') { sb.append(' '); continue; }
                float t=ci/(float)n; ci++;
                sb.append(String.format("[#%02X%02X%02X]%c",
                    (int)(r1+(r2-r1)*t),(int)(g1+(g2-g1)*t),(int)(b1+(b2-b1)*t),c));
            }
            return sb+"[]";
        }

        private String fmt(float v) {
            if (v>=1000000f) return arc.util.Strings.fixed(v/1000000f,2)+"M";
            if (v>=1000f)    return arc.util.Strings.fixed(v/1000f,1)+"k";
            return arc.util.Strings.fixed(v,0);
        }

        @Override
        public void write(Writes w) {
            super.write(w);
            w.f(temperature); w.f(internalLife);
            w.bool(scrammed); w.bool(autoMode);
        }

        @Override
        public void read(Reads r, byte rev) {
            super.read(r,rev);
            temperature=r.f(); internalLife=r.f();
            scrammed=r.bool(); autoMode=r.bool();
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return liquid == Liquids.water || liquid == Liquids.cryofluid;
        }
    }
}
