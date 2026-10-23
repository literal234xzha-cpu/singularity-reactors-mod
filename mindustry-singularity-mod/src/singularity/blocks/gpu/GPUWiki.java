package singularity.blocks.gpu;

import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;

/**
 * GPUWiki — Sistema de wiki/ayuda in-game para las GPUs.
 *
 * Accesible desde la UI de cualquier GPU.
 * Explica con ejemplos simples cómo usar cada función,
 * instrucción vectorial y evento.
 *
 * SECCIONES:
 * - ¿Qué es una GPU?
 * - Modo Simple (funciones precargadas)
 * - Modo Código (instrucciones especiales)
 * - Instrucciones Vectoriales (gpuvec)
 * - Eventos (onevent)
 * - Ejemplos completos
 */
public class GPUWiki {

    public static class WikiPage {
        public final String title;
        public final String icon;
        public final String content; // texto formateado con colores Mindustry
        public WikiPage(String title, String icon, String content) {
            this.title = title; this.icon = icon; this.content = content;
        }
    }

    public static final Seq<WikiPage> PAGES = new Seq<>();

    static {
        // ── Página 1: ¿Qué es una GPU? ──────────────────────────────────────
        PAGES.add(new WikiPage("¿Qué es una GPU?", "🖥",
            "[cyan]GPU vs Procesador Normal[]\n\n" +
            "[white]Un procesador normal ejecuta[]\n" +
            "[white]1 instrucción a la vez.[]\n\n" +
            "[green]Una GPU puede:[]\n" +
            "[green]• Ejecutar 12-200 ipt[]\n" +
            "[green]• Operar sobre TODOS[]\n" +
            "[green]  los bloques a la vez[]\n" +
            "[green]• Reaccionar a eventos[]\n" +
            "[green]  sin bucles continuos[]\n\n" +
            "[yellow]Tiers:[]\n" +
            "[white]MicroGPU → 12 ipt, 1×1[]\n" +
            "[white]GPU      → 50 ipt, 2×2[]\n" +
            "[white]HiperGPU → 200 ipt, 3×3[]\n\n" +
            "[lightgray]Cada tier desbloquea\nmás funciones y mayor\nrango de conexión.[]"
        ));

        // ── Página 2: Modo Simple ────────────────────────────────────────────
        PAGES.add(new WikiPage("Modo Simple", "🔘",
            "[cyan]Modo Simple - Sin código[]\n\n" +
            "[white]1. Coloca la GPU[]\n" +
            "[white]2. Toca para abrir UI[]\n" +
            "[white]3. Selecciona función[]\n" +
            "[white]4. Configura parámetros[]\n" +
            "[white]5. Conecta un[]\n" +
            "[white]   Message Block[]\n" +
            "[white]6. ¡Funciona solo![]\n\n" +
            "[yellow]Ejemplo - Energy Counter:[]\n" +
            "[lightgray]Conecta la GPU a cualquier\nbloque con energía y\nun Message Block.\nMostrará producción,\nconsumo y balance\nautomáticamente.[]"
        ));

        // ── Página 3: Instrucciones Vectoriales ─────────────────────────────
        PAGES.add(new WikiPage("Instrucciones Vec", "⚡",
            "[cyan]gpuvec - Opera en paralelo[]\n\n" +
            "[white]Una instrucción afecta[]\n" +
            "[white]TODOS los bloques del[]\n" +
            "[white]mismo tipo a la vez.[]\n\n" +
            "[yellow]Sintaxis:[]\n" +
            "[green]gpuvec <instrucción>[]\n\n" +
            "[yellow]Ejemplos:[]\n\n" +
            "[green]gpuvec blocks.enable.all[]\n" +
            "[lightgray]→ Activa todos los bloques\n\n[]" +
            "[green]gpuvec turrets.aim[]\n" +
            "[lightgray]→ Activa torretas en rango\ndel enemigo más cercano\n\n[]" +
            "[green]gpuvec generators.balance[]\n" +
            "[lightgray]→ Balancea generadores\nsegún demanda\n\n[]" +
            "[green]set n (gpuvec\n  query.enemies.count 20)[]\n" +
            "[lightgray]→ Cuenta enemigos\nen 20 tiles de radio[]"
        ));

        // ── Página 4: Sistema de Eventos ────────────────────────────────────
        PAGES.add(new WikiPage("Sistema de Eventos", "📡",
            "[cyan]Eventos - Sin bucles[]\n\n" +
            "[white]Los eventos solo corren[]\n" +
            "[white]cuando algo CAMBIA.[]\n" +
            "[white]Más eficiente que bucles.[]\n\n" +
            "[yellow]Tipos de evento:[]\n" +
            "[green]• ENEMY_ENTER[]\n" +
            "[lightgray]  Enemigo entra en rango\n[]" +
            "[green]• POWER_LOW[]\n" +
            "[lightgray]  Energía cae bajo umbral\n[]" +
            "[green]• HEALTH_LOW[]\n" +
            "[lightgray]  Bloque siendo atacado\n[]" +
            "[green]• WAVE_START[]\n" +
            "[lightgray]  Inicio de oleada\n[]" +
            "[green]• TIMER[]\n" +
            "[lightgray]  Cada X ticks\n\n[]" +
            "[yellow]En la GPU:[]\n" +
            "[white]Tab 'Eventos' → añade[]\n" +
            "[white]evento → asigna acción[]\n" +
            "[white]vectorial → listo[]"
        ));

        // ── Página 5: Ejemplo - Defensa automática ───────────────────────────
        PAGES.add(new WikiPage("Ejemplo: Defensa", "⚔",
            "[cyan]Defensa automática[]\n[lightgray](sin escribir código)[]\n\n" +
            "[yellow]Objetivo:[]\n" +
            "[white]Activar torretas cuando[]\n" +
            "[white]detecte enemigos[]\n\n" +
            "[yellow]Pasos con GPU:[]\n\n" +
            "[white]1.[green] Tab Eventos[]\n" +
            "[white]   + ENEMY_ENTER[]\n" +
            "[white]   Radio: 30 tiles[]\n\n" +
            "[white]2.[green] Acción:[]\n" +
            "[white]   turrets.aim[]\n\n" +
            "[white]3.[green] + ENEMY_EXIT[]\n" +
            "[white]   Acción:[]\n" +
            "[white]   turrets.enable false[]\n\n" +
            "[white]4. Conecta torretas[]\n" +
            "[white]5. ¡Listo![]\n\n" +
            "[lightgray]Las torretas se activarán\nsolas cuando haya enemigos\ny se desactivarán cuando\nel área esté despejada.[]"
        ));

        // ── Página 6: Ejemplo - Energy Manager ──────────────────────────────
        PAGES.add(new WikiPage("Ejemplo: Energía", "⚡",
            "[cyan]Gestión de energía[]\n\n" +
            "[yellow]Objetivo:[]\n" +
            "[white]Apagar fábricas si la[]\n" +
            "[white]energía cae < 60%[]\n\n" +
            "[yellow]Con Modo Simple:[]\n" +
            "[white]1. Función 8: Toggle Switch[]\n" +
            "[white]2. Modo: Auto Energía[]\n" +
            "[white]3. Umbral: 60%[]\n" +
            "[white]4. Conectar fábricas[]\n\n" +
            "[yellow]Con Eventos (GPU tier 1):[]\n" +
            "[white]1.[green] Evento POWER_LOW[]\n" +
            "[white]   Umbral: 60%[]\n" +
            "[white]   Acción:[]\n" +
            "[white]   blocks.disable.all[]\n\n" +
            "[white]2.[green] Evento POWER_RESTORED[]\n" +
            "[white]   Umbral: 60%[]\n" +
            "[white]   Acción:[]\n" +
            "[white]   blocks.enable.all[]\n\n" +
            "[lightgray]La GPU no necesita\nbucles - reacciona solo\ncuando cambia la energía.[]"
        ));

        // ── Página 7: Modo Código GPU ────────────────────────────────────────
        PAGES.add(new WikiPage("Modo Código", "💻",
            "[cyan]Modo Código - Avanzado[]\n\n" +
            "[white]Igual que procesador\nnormal pero más rápido\ny con instrucciones extra.[]\n\n" +
            "[yellow]Counter de energía:[]\n" +
            "[lightgray]Normal (8 líneas):[]\n" +
            "[white]sensor prod @n @powerNet\n" +
            "       Produced\n" +
            "sensor cons @n @powerNet\n" +
            "       Consumed\n" +
            "op mul prod prod 60\n" +
            "print \"Prod: \" prod\n" +
            "printflush msg1[]\n\n" +
            "[yellow]Con GPU (igual código,[]\n" +
            "[yellow]12x más rápido):[]\n" +
            "[white]sensor prod @n ...\n" +
            "sensor cons @n ...\n" +
            "op mul prod prod 60\n" +
            "printflush msg1[]\n\n" +
            "[lightgray]La diferencia es la\nvelocidad de ejecución\ny el rango de conexión.[]"
        ));
    }

    /** Construye el panel de wiki en una tabla UI */
    public static void buildWikiPanel(Table table) {
        table.defaults().left().pad(4f);
        table.add("[cyan]📖 GPU Wiki[]").row();

        ScrollPane scroll = new ScrollPane(new Table(t -> {
            for (WikiPage page : PAGES) {
                t.table(card -> {
                    card.defaults().left().padBottom(2f);
                    card.add("[white]" + page.icon + " " + page.title + "[]")
                        .padBottom(4f).row();
                    card.add("[lightgray]" + page.content + "[]")
                        .width(200f).wrap(true).row();
                    card.image().color(arc.graphics.Color.darkGray)
                        .fillX().height(1f).pad(4f).row();
                }).left().pad(4f).row();
            }
        }));
        table.add(scroll).width(220f).height(300f).row();
    }
}
