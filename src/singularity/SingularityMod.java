package singularity;

import arc.util.*;
import mindustry.mod.*;
import singularity.content.*;

public class SingularityMod extends Mod {

    public SingularityMod() {
        Log.info("[Singularity] Singularity Reactors v2.4.0 loaded");
    }

    @Override
    public void loadContent() {
        Log.info("[Singularity] Cargando contenido...");
        SingularityBlocks.load();
        Log.info("[Singularity] Contenido cargado");
    }

    @Override
    public void init() {
        // UpdateChecker deshabilitado temporalmente para debug Android
        UpdateChecker.checkAsync();
        Log.info("[Singularity] Mod inicializado correctamente");
    }
}
