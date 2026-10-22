package singularity;

/**
 * GradientText — Genera texto con degradado de color para descripciones de bloques.
 *
 * Uso:
 *   GradientText.make("Hola mundo", 0xFF4400, 0x0044FF)
 *   → "[#FF4400]H[#EE3811]o[#DD2C22]l[#CC2033]a..."
 *
 * Para descripciones de bloques en bundle.properties usa makeSimple()
 * que aplica un degradado de 2 colores a frases cortas.
 */
public class GradientText {

    /**
     * Genera texto con degradado entre dos colores hex.
     * @param text  Texto a colorear
     * @param from  Color inicial (0xRRGGBB)
     * @param to    Color final   (0xRRGGBB)
     */
    public static String make(String text, int from, int to) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder sb = new StringBuilder();
        int len = text.length();

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            // Omitir espacios sin tag de color (ahorra caracteres)
            if (c == ' ') { sb.append(' '); continue; }

            float t = len <= 1 ? 0f : (float) i / (len - 1);
            int r = lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
            int g = lerp((from >>  8) & 0xFF, (to >>  8) & 0xFF, t);
            int b = lerp( from        & 0xFF,  to        & 0xFF, t);

            sb.append(String.format("[#%02X%02X%02X]%c", r, g, b, c));
        }
        sb.append("[]");
        return sb.toString();
    }

    /**
     * Aplica degradado solo al primer segmento (titulo) de una descripcion.
     * El resto del texto queda en color claro.
     * @param title  Titulo en degradado
     * @param body   Cuerpo en gris claro
     * @param from   Color inicio
     * @param to     Color fin
     */
    public static String titled(String title, String body, int from, int to) {
        return make(title, from, to) + "\n[lightgray]" + body + "[]";
    }

    /**
     * Degradado predefinido para cada tipo de bloque.
     */
    public static class Presets {
        // Rojo → Naranja (reactores, peligroso)
        public static String reactor(String text) {
            return make(text, 0xFF2200, 0xFF8800);
        }

        // Azul → Cyan (energia, monitores)
        public static String energy(String text) {
            return make(text, 0x2244FF, 0x00CCFF);
        }

        // Cyan → Verde (liquidos, tuberias)
        public static String liquid(String text) {
            return make(text, 0x00AACC, 0x00FF88);
        }

        // Amarillo → Naranja (items, logistica)
        public static String items(String text) {
            return make(text, 0xFFCC00, 0xFF6600);
        }

        // Morado → Azul (nodos, superconductores)
        public static String node(String text) {
            return make(text, 0x8800FF, 0x0088FF);
        }

        // Blanco → Gris (informacion neutral)
        public static String info(String text) {
            return make(text, 0xFFFFFF, 0xAAAAAA);
        }
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }
}
