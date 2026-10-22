package singularity.content;

import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.draw.*;
import singularity.blocks.*;
import singularity.blocks.delta.*;

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
    
    // Sistema Delta (central nuclear)
    public static Block deltaReactor, deltaTurbine, deltaCondenser, deltaControlPanel;
    public static Block deltaLoadRegulator, deltaBreaker, deltaScram, tempSensor;
    
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
        singularityReactorHyper = new SingularityReactor("singularity-reactor-hyper") {{
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
        singularityReactorImpact = new SingularityReactor("singularity-reactor-impact") {{
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
        singularityReactorNeodymium = new SingularityReactor("singularity-reactor-neodymium") {{
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
        singularityReactorImpactor = new SingularityReactor("singularity-reactor-impactor") {{
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

        // ============= SISTEMA DELTA (CENTRAL NUCLEAR) =============

        deltaReactor = new DeltaReactor("delta-reactor") {{
            requirements(Category.power, with(
                Items.thorium, 500,
                Items.surgeAlloy, 300,
                Items.silicon, 400,
                Items.phaseFabric, 200,
                Items.metaglass, 150
            ));
            size = 5;
            health = 2500;
            liquidCapacity = 100f;
        }};

        deltaTurbine = new DeltaTurbine("delta-turbine") {{
            requirements(Category.power, with(
                Items.titanium, 250,
                Items.silicon, 150,
                Items.thorium, 100,
                Items.plastanium, 80
            ));
            size = 3;
            health = 800;
        }};

        deltaCondenser = new DeltaCondenser("delta-condenser") {{
            requirements(Category.liquid, with(
                Items.titanium, 150,
                Items.metaglass, 100,
                Items.silicon, 80
            ));
            size = 3;
            health = 600;
            liquidCapacity = 120f;
        }};

        deltaControlPanel = new DeltaControlPanel("delta-control-panel") {{
            requirements(Category.power, with(
                Items.silicon, 250,
                Items.surgeAlloy, 150,
                Items.phaseFabric, 100,
                Items.plastanium, 120
            ));
            size = 4;
            health = 1000;
            consumePower(8f);
        }};

        deltaLoadRegulator = new DeltaLoadRegulator("delta-load-regulator") {{
            requirements(Category.power, with(
                Items.silicon, 120,
                Items.titanium, 80,
                Items.surgeAlloy, 60,
                Items.phaseFabric, 40
            ));
            size = 3;
            health = 600;
            consumePower(3f);
        }};

        deltaBreaker = new DeltaBreaker("delta-breaker") {{
            requirements(Category.power, with(
                Items.titanium, 20,
                Items.silicon, 15,
                Items.surgeAlloy, 10
            ));
            size = 1;
            health = 200;
        }};

        deltaScram = new DeltaScram("delta-scram") {{
            requirements(Category.power, with(
                Items.surgeAlloy, 80,
                Items.phaseFabric, 50,
                Items.silicon, 100,
                Items.plastanium, 60
            ));
            size = 2;
            health = 500;
            consumePower(2f);
        }};

        tempSensor = new TempSensor("temp-sensor") {{
            requirements(Category.power, with(
                Items.silicon, 15,
                Items.titanium, 10,
                Items.metaglass, 8
            ));
            size = 1;
            health = 100;
        }};
    }
}
