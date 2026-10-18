package singularity;

import arc.*;
import arc.util.*;
import mindustry.mod.*;
import singularity.content.*;

public class SingularityMod extends Mod {

    public SingularityMod() {
        Log.info("╔══════════════════════════════════════════╗");
        Log.info("║  Singularity Reactors Mod - Loaded      ║");
        Log.info("║  Sistema eléctrico realista con         ║");
        Log.info("║  matemáticas de física aplicada         ║");
        Log.info("╚══════════════════════════════════════════╝");
    }

    @Override
    public void loadContent() {
        Log.info("[Singularity] Cargando contenido del mod...");

        // Cargar bloques en orden de dependencia
        SingularityBlocks.load();

        Log.info("[Singularity] ✓ Contenido cargado exitosamente");
    }

    @Override
    public void init() {
        // Chequear actualizaciones en segundo plano (no bloquea el juego)
        Log.info("[Singularity] Verificando actualizaciones...");
        UpdateChecker.checkAsync();
    }
}
