package singularity;

import arc.util.*;
import mindustry.mod.*;
import singularity.content.*;

public class SingularityMod extends Mod {

    public SingularityMod() {
        Log.info("[Singularity] Singularity Reactors v" + UpdateChecker.CURRENT_VERSION + " loaded");
    }

    @Override
    public void loadContent() {
        Log.info("[Singularity] Cargando contenido...");
        SingularityBlocks.load();
        Log.info("[Singularity] Contenido cargado");
    }

    @Override
    public void init() {
        UpdateChecker.checkAsync();
    }
}
