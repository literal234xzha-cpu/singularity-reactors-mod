package singularity;

import arc.*;
import arc.util.*;
import mindustry.ui.dialogs.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Comprueba si hay una nueva versión del mod en GitHub Releases.
 */
public class UpdateChecker {

    /** Debe coincidir con mod.hjson → version */
    public static final String CURRENT_VERSION = "2.2.4";

    private static final String REPO = "literal234xzha-cpu/singularity-reactors-mod";
    private static final String API_URL =
        "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String RELEASES_URL =
        "https://github.com/" + REPO + "/releases";

    private static volatile String latestVersion = null;

    public static void checkAsync() {
        Thread t = new Thread(() -> {
            try {
                String tag = fetchLatestTag();
                if (tag != null) {
                    latestVersion = tag.startsWith("v") ? tag.substring(1) : tag;
                }
            } catch (Exception e) {
                Log.warn("[Singularity] UpdateChecker: no se pudo verificar (" + e.getMessage() + ")");
            } finally {
                Core.app.post(UpdateChecker::onCheckDone);
            }
        }, "singularity-update-checker");
        t.setDaemon(true);
        t.start();
    }

    private static String fetchLatestTag() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setRequestProperty("User-Agent", "singularity-mod-updater");
        conn.setRequestProperty("Accept", "application/vnd.github+json");

        int code = conn.getResponseCode();
        if (code != 200) {
            Log.warn("[Singularity] UpdateChecker: HTTP " + code);
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        String json = sb.toString();
        int idx = json.indexOf("\"tag_name\"");
        if (idx == -1) return null;

        int start = json.indexOf('"', idx + 10) + 1;
        int end = json.indexOf('"', start);
        if (start <= 0 || end <= start) return null;
        return json.substring(start, end);
    }

    private static void onCheckDone() {
        if (latestVersion == null) return;

        if (isNewer(latestVersion, CURRENT_VERSION)) {
            Log.info("[Singularity] Actualización disponible: v" + CURRENT_VERSION
                + " → v" + latestVersion + " | " + RELEASES_URL);

            try {
                if (Core.scene != null) {
                    showUpdateDialog(latestVersion);
                }
            } catch (Exception ignored) {
            }
        } else {
            Log.info("[Singularity] UpdateChecker: mod al día (v" + CURRENT_VERSION + ")");
        }
    }

    private static boolean isNewer(String candidate, String current) {
        try {
            int[] cand = parseVersion(candidate);
            int[] cur = parseVersion(current);
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

    private static void showUpdateDialog(String newVersion) {
        BaseDialog dialog = new BaseDialog("Actualización disponible");
        dialog.cont.add(
            "[accent]Singularity Reactors[] tiene una nueva versión.\n\n" +
            "[lightgray]Versión actual: [white]v" + CURRENT_VERSION + "[]\n" +
            "[lightgray]Nueva versión:  [green]v" + newVersion + "[]\n\n" +
            "[gray]Descárgala en:\n" +
            "[cyan]" + RELEASES_URL + "[]"
        ).center().pad(16f).row();
        dialog.buttons.button("[green]OK[]", dialog::hide).size(100f, 40f);
        dialog.show();
    }
}
