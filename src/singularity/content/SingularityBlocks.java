package singularity.content;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;
import mindustry.world.draw.*;

import static mindustry.type.ItemStack.*;

public class SingularityBlocks {
    
    // Cables mejorados
    public static Block resistivePowerNode, heavyPowerNode, superConductorNode;
    
    // Reactores de Singularidad (4 variantes)
    public static Block singularityReactorHyper, singularityReactorImpact, 
                        singularityReactorNeodymium, singularityReactorImpactor;
    
    public static void load() {
        
        // ============= CABLES MEJORADOS =============
        
        resistivePowerNode = new PowerNode("resistive-power-node") {{
            requirements(Category.power, with(
                Items.copper, 3,
                Items.lead, 2
            ));
            size = 1;
            maxNodes = 6;
            laserRange = 8;
            health = 80;
        }};
        
        heavyPowerNode = new PowerNode("heavy-power-node") {{
            requirements(Category.power, with(
                Items.titanium, 5,
                Items.silicon, 3,
                Items.copper, 8
            ));
            size = 2;
            maxNodes = 15;
            laserRange = 20;
            health = 280;
        }};
        
        superConductorNode = new PowerNode("superconductor-node") {{
            requirements(Category.power, with(
                Items.phaseFabric, 2,
                Items.surgeAlloy, 4,
                Items.silicon, 10
            ));
            size = 2;
            maxNodes = 25;
            laserRange = 40;
            health = 450;
            consumePower(500f / 60f);
        }};
        
        // ============= REACTORES DE SINGULARIDAD =============
        
        // Reactor 1: Hiper-Impacto (Más estable, menor producción)
        singularityReactorHyper = new ConsumeGenerator("singularity-reactor-hyper") {{
            requirements(Category.power, with(
                Items.plastanium, 800,
                Items.thorium, 600,
                Items.titanium, 1000,
                Items.surgeAlloy, 400,
                Items.silicon, 600
            ));
            size = 5;
            health = 3000;
            
            // Producción: 50k/s neto (833 por tick)
            powerProduction = 833.33f;
            
            // Consumo de líquidos (crítico)
            consumeLiquid(Liquids.water, 2.5f / 60f);
            consumeLiquid(Liquids.cryofluid, 1.5f / 60f);
            
            // Consumo de items MÚLTIPLES
            consumeItems(with(
                Items.plastanium, 8,
                Items.thorium, 6
            ));
            
            itemDuration = 60f;
            
            drawer = new DrawMulti(
                new DrawDefault(),
                new DrawGlowRegion(){{
                    alpha = 0.8f;
                    glowScale = 6f;
                }}
            );
            
            ambientSound = Sounds.loopPulse;
            ambientSoundVolume = 0.07f;
        }};
        
        // Reactor 2: Impacto Singularidad (Balanceado)
        singularityReactorImpact = new ConsumeGenerator("singularity-reactor-impact") {{
            requirements(Category.power, with(
                Items.plastanium, 1200,
                Items.thorium, 900,
                Items.titanium, 1500,
                Items.surgeAlloy, 700,
                Items.silicon, 800,
                Items.phaseFabric, 200
            ));
            size = 6;
            health = 4500;
            
            // Producción: 80k/s neto (1333 por tick)
            powerProduction = 1333.33f;
            
            consumeLiquid(Liquids.water, 4f / 60f);
            consumeLiquid(Liquids.cryofluid, 2.5f / 60f);
            
            consumeItems(with(
                Items.plastanium, 12,
                Items.thorium, 10
            ));
            
            itemDuration = 60f;
            
            drawer = new DrawMulti(
                new DrawDefault(),
                new DrawGlowRegion(){{
                    alpha = 0.9f;
                    glowScale = 7f;
                }}
            );
            
            ambientSound = Sounds.loopPulse;
            ambientSoundVolume = 0.09f;
        }};
        
        // Reactor 3: Neodimio (Alta eficiencia, muy comilón)
        singularityReactorNeodymium = new ConsumeGenerator("singularity-reactor-neodymium") {{
            requirements(Category.power, with(
                Items.plastanium, 1800,
                Items.thorium, 1400,
                Items.titanium, 2000,
                Items.surgeAlloy, 1200,
                Items.silicon, 1000,
                Items.phaseFabric, 500
            ));
            size = 7;
            health = 6000;
            
            // Producción: 150k/s neto (2500 por tick)
            powerProduction = 2500f;
            
            // MUY COMILÓN
            consumeLiquid(Liquids.water, 8f / 60f);
            consumeLiquid(Liquids.cryofluid, 5f / 60f);
            consumeLiquid(Liquids.slag, 2f / 60f);
            
            consumeItems(with(
                Items.plastanium, 20,
                Items.thorium, 18
            ));
            
            itemDuration = 60f;
            
            drawer = new DrawMulti(
                new DrawDefault(),
                new DrawGlowRegion(){{
                    alpha = 1f;
                    glowScale = 8f;
                }}
            );
            
            ambientSound = Sounds.loopThoriumReactor;
            ambientSoundVolume = 0.12f;
        }};
        
        // Reactor 4: Impactador (El más peligroso y potente)
        singularityReactorImpactor = new ConsumeGenerator("singularity-reactor-impactor") {{
            requirements(Category.power, with(
                Items.plastanium, 3000,
                Items.thorium, 2500,
                Items.titanium, 3500,
                Items.surgeAlloy, 2000,
                Items.silicon, 1500,
                Items.phaseFabric, 1000
            ));
            size = 8;
            health = 8000;
            
            // Producción: 250k/s neto (4166 por tick)
            powerProduction = 4166.67f;
            
            // BRUTALMENTE COMILÓN
            consumeLiquid(Liquids.water, 15f / 60f);
            consumeLiquid(Liquids.cryofluid, 10f / 60f);
            consumeLiquid(Liquids.slag, 5f / 60f);
            consumeLiquid(Liquids.oil, 3f / 60f);
            
            consumeItems(with(
                Items.plastanium, 35,
                Items.thorium, 30
            ));
            
            itemDuration = 60f;
            
            drawer = new DrawMulti(
                new DrawDefault(),
                new DrawGlowRegion(){{
                    alpha = 1.2f;
                    glowScale = 10f;
                }}
            );
            
            ambientSound = Sounds.loopThoriumReactor;
            ambientSoundVolume = 0.15f;
        }};
    }
}
