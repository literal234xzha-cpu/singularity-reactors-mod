package singularity.content;

import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.draw.*;
import singularity.blocks.*;

import static mindustry.type.ItemStack.*;

public class SingularityBlocks {
    
    // Cables mejorados
    public static Block resistivePowerNode, heavyPowerNode, superConductorNode;
    
    // Reactores de Singularidad (4 variantes)
    public static Block singularityReactorHyper, singularityReactorImpact, 
                        singularityReactorNeodymium, singularityReactorImpactor;

    // Sistema de gestión
    public static Block powerMonitor;
    public static Block powerControllerRoot;
    public static Block powerControllerNet;
    public static Block universalBlockManager;
    public static Block liquidController;

    // Nuevos monitores y controladores
    public static Block powerMonitorLocal;
    public static Block powerMonitorGlobal;
    public static Block batteryMonitor;
    public static Block itemController;
    
    public static void load() {
        
        // ============= NODOS DE ENERGÍA =============
        
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
        
        // Reactor Alpha - 50k/s
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
            itemCapacity = 30;
            liquidCapacity = 60f;

            // 50_000 /s → powerProduction es por tick (*60)
            powerProduction = 833.33f;

            consumeLiquid(Liquids.water, 12f / 60f);      // 12/s
            consumeLiquid(Liquids.cryofluid, 8f / 60f);  // 8/s

            consumeItems(with(
                Items.plastanium, 3,  // 3/s
                Items.thorium, 2     // 2/s
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
        
        // Reactor Beta - 80k/s
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
            itemCapacity = 40;
            liquidCapacity = 80f;

            powerProduction = 1333.33f;

            consumeLiquid(Liquids.water, 20f / 60f);     // 20/s
            consumeLiquid(Liquids.cryofluid, 12f / 60f); // 12/s

            consumeItems(with(
                Items.plastanium, 4,
                Items.thorium, 3
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
        
        // Reactor Gamma - 150k/s
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
            itemCapacity = 50;
            liquidCapacity = 100f;

            powerProduction = 2500f;

            consumeLiquid(Liquids.water, 40f / 60f);     // 40/s
            consumeLiquid(Liquids.cryofluid, 25f / 60f); // 25/s
            consumeLiquid(Liquids.slag, 10f / 60f);      // 10/s

            consumeItems(with(
                Items.plastanium, 5,
                Items.thorium, 4
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
        
        // Reactor Omega - 250k/s
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
            itemCapacity = 70;
            liquidCapacity = 140f;

            powerProduction = 4166.67f;

            consumeLiquid(Liquids.water, 75f / 60f);     // 75/s
            consumeLiquid(Liquids.cryofluid, 50f / 60f); // 50/s
            consumeLiquid(Liquids.slag, 25f / 60f);      // 25/s
            consumeLiquid(Liquids.oil, 15f / 60f);       // 15/s

            consumeItems(with(
                Items.plastanium, 7,
                Items.thorium, 5
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

        // ============= SISTEMA DE GESTIÓN =============

        powerMonitor = new PowerMonitor("power-monitor") {{
            requirements(Category.power, with(
                Items.silicon, 20,
                Items.titanium, 15,
                Items.surgeAlloy, 5
            ));
            size = 2;
            health = 150;
            monitorRange = 20;
        }};

        powerControllerRoot = new PowerControllerRoot("power-controller-root") {{
            requirements(Category.power, with(
                Items.silicon, 30,
                Items.titanium, 20,
                Items.surgeAlloy, 10,
                Items.phaseFabric, 5
            ));
            size = 2;
            health = 350;
            controlRange = 8;
            consumePower(2f);
        }};

        powerControllerNet = new PowerControllerNet("power-controller-net") {{
            requirements(Category.power, with(
                Items.silicon, 60,
                Items.titanium, 40,
                Items.surgeAlloy, 25,
                Items.phaseFabric, 15
            ));
            size = 3;
            health = 600;
            consumePower(5f);
        }};

        universalBlockManager = new UniversalBlockManager("universal-block-manager") {{
            requirements(Category.logic, with(
                Items.silicon, 80,
                Items.titanium, 50,
                Items.surgeAlloy, 30,
                Items.phaseFabric, 20
            ));
            size = 3;
            health = 500;
            manageRange = 15;
            consumePower(3f);
        }};

        liquidController = new LiquidController("liquid-controller") {{
            requirements(Category.liquid, with(
                Items.silicon, 25,
                Items.titanium, 20,
                Items.metaglass, 15,
                Items.surgeAlloy, 8
            ));
            size = 2;
            health = 250;
            liquidRange = 12;
            lowLevelThreshold = 0.20f;
            consumePower(1.5f);
        }};

        // ============= NUEVOS MONITORES =============

        powerMonitorLocal = new PowerMonitorLocal("power-monitor-local") {{
            requirements(Category.power, with(
                Items.silicon, 15,
                Items.titanium, 10,
                Items.copper, 20
            ));
            size = 2;
            health = 150;
            consumePower(1f);
        }};

        powerMonitorGlobal = new PowerMonitorGlobal("power-monitor-global") {{
            requirements(Category.power, with(
                Items.silicon, 40,
                Items.titanium, 30,
                Items.surgeAlloy, 15,
                Items.phaseFabric, 8
            ));
            size = 3;
            health = 400;
            scanRange = 40;
            consumePower(4f);
        }};

        batteryMonitor = new BatteryMonitor("battery-monitor") {{
            requirements(Category.power, with(
                Items.silicon, 20,
                Items.titanium, 15,
                Items.lead, 30
            ));
            size = 2;
            health = 200;
            monitorRange = 15;
            consumePower(1.5f);
        }};

        itemController = new ItemController("item-controller") {{
            requirements(Category.logic, with(
                Items.silicon, 30,
                Items.titanium, 20,
                Items.copper, 25
            ));
            size = 2;
            health = 250;
            controlRange = 15;
            consumePower(2f);
        }};
    }
}
