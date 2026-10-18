package singularity;

import arc.*;
import arc.util.*;
import mindustry.ui.dialogs.*;

import java.io.*;
import java.net.*;

/**
 * UpdateChecker — Comprueba si hay una nueva versión del mod disponible.
 *
 * Se ejecuta en un hilo separado al arrancar el mod para no bloquear el juego.
 * Compara la versión del mod.hjson con la última release de GitHub.
 * Si hay una versión más nueva, muestra una notificación en el log y
 * opcionalmente un diálogo en pantalla (solo en modo cliente, no servidor).
 *
 * URL de releases: https://github.com/LOLOCORD/singularity-reactors/releases/latest
 */
public class UpdateChecker {

    /** Versión actual del mod (debe coincidir con mod.hjson). */
    public static final String CURRENT_VERSION = "2.0.0";

    /**
     * URL de la API de GitHub que devuelve JSON con el campo "tag_name".
     * Formato: {"tag_name": "v2.1.0", ...}
     * Ajusta el repo cuando lo subas a GitHub.
     */
    private static final String API_URL =
        "https://api.github.com/repos/literal234xzha-cpu/singularity-reactors-mod/releases/latest";

    private static volatile String latestVersion = null;
    private static volatile boolean checked = false;

    /**
     * Lanza el chequeo en un hilo daemon (no bloquea el juego).
     * Llama a onResult en el hilo principal de Arc cuando termina.
     */
    public static void checkAsync() {
        Thread t = new Thread(() -> {
            try {
                String tag = fetchLatestTag();
                if (tag != null) {
                    // Normalizar: quitar "v" inicial si hay
                    latestVersion = tag.startsWith("v") ? tag.substring(1) : tag;
                }
            } catch (Exception e) {
                Log.warn("[Singularity] UpdateChecker: no se pudo verificar (" + e.getMessage() + ")");
            } finally {
                checked = true;
                // Volver al hilo principal de Arc para mostrar el resultado
                Core.app.post(UpdateChecker::onCheckDone);
            }
        }, "singularity-update-checker");
        t.setDaemon(true);
        t.start();
    }

    /** Descarga el JSON de la API y extrae el tag_name. */
    private static String fetchLatestTag() throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setRequestProperty("User-Agent", "singularity-mod-updater");

        int code = conn.getResponseCode();
        if (code != 200) {
            Log.warn("[Singularity] UpdateChecker: HTTP " + code);
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        String json = sb.toString();
        // Extracción simple sin dependencias extra
        int idx = json.indexOf("\"tag_name\"");
        if (idx == -1) return null;

        int start = json.indexOf('"', idx + 10) + 1;
        int end   = json.indexOf('"', start);
        return json.substring(start, end);
    }

    /** Se llama en el hilo principal cuando el chequeo termina. */
    private static void onCheckDone() {
        if (latestVersion == null) return;

        if (isNewer(latestVersion, CURRENT_VERSION)) {
            Log.info("[Singularity] ╔══════════════════════════════════════════╗");
            Log.info("[Singularity] ║  ⬆ ACTUALIZACIÓN DISPONIBLE             ║");
            Log.info("[Singularity] ║  Versión actual : v" + CURRENT_VERSION + "              ║");
            Log.info("[Singularity] ║  Nueva versión  : v" + latestVersion + "              ║");
            Log.info("[Singularity] ║  https://github.com/LOLOCORD/            ║");
            Log.info("[Singularity] ║  singularity-reactors/releases           ║");
            Log.info("[Singularity] ╚══════════════════════════════════════════╝");

            // Mostrar diálogo en pantalla (solo si hay UI disponible)
            try {
                if (Core.scene != null) {
                    showUpdateDialog(latestVersion);
                }
            } catch (Exception ignored) {
                // En servidores dedicados no hay UI
            }
        } else {
            Log.info("[Singularity] UpdateChecker: mod al día (v" + CURRENT_VERSION + ")");
        }
    }

    /** Compara versiones semánticas. Devuelve true si candidate > current. */
    private static boolean isNewer(String candidate, String current) {
        try {
            int[] cand = parseVersion(candidate);
            int[] cur  = parseVersion(current);
            for (int i = 0; i < Math.min(cand.length, cur.length); i++) {
                if (cand[i] > cur[i]) return true;
                if (cand[i] < cur[i]) return false;
            }
            return cand.length > cur.length;
        } catch (Exception e) {
            return false;
        }
    }

    private static int[] parseVersion(String v) {
        String[] parts = v.split("\\.");
        int[] nums = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            nums[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
        }
        return nums;
    }

    /** Muestra un diálogo informativo en pantalla. */
    private static void showUpdateDialog(String newVersion) {
        BaseDialog dialog = new BaseDialog("⬆ Actualización disponible");
        dialog.cont.add(
            "[accent]Singularity Reactors[] tiene una nueva versión.\n\n" +
            "[lightgray]Versión actual: [white]v" + CURRENT_VERSION + "[]\n" +
            "[lightgray]Nueva versión:  [green]v" + newVersion + "[]\n\n" +
            "[gray]Descárgala en:\n" +
            "[cyan]github.com/LOLOCORD/singularity-reactors/releases[]"
        ).center().pad(16f).row();
        dialog.buttons.button("[green]OK[]", dialog::hide).size(100f, 40f);
        dialog.show();
    }
}
