package com.leo.nexus77

import android.webkit.WebView

/**
 * Geração, limpeza e injeção de tema no HTML produzido pela IA.
 * Tudo offline: só manipulação de string + loadDataWithBaseURL.
 */
object Html {

    /** Origem base estável para o loadDataWithBaseURL (localStorage, URLs relativas etc.). */
    const val BASE = "https://nexus77.local/"

    private val VIEWPORT_REGEX = Regex("""name\s*=\s*["']viewport["']""", RegexOption.IGNORE_CASE)
    private val HEAD_REGEX = Regex("""<head[^>]*>""", RegexOption.IGNORE_CASE)
    private val HTML_REGEX = Regex("""<html[^>]*>""", RegexOption.IGNORE_CASE)

    private val BASE_PROMPT = """
        You are the page generator of the app NEXUS77. From the user's short command, you build ONE complete, self-contained HTML document.

        Rules:
        - Output ONLY raw HTML. No markdown fences, no explanations, no text before or after.
        - Start with <!DOCTYPE html>. Single file: all CSS and JavaScript inline.
        - You may load libraries from public CDNs (recommended for 3D: https://unpkg.com/three@0.160.0/build/three.min.js).
        - Make it spectacular and alive: canvas/WebGL animation, CSS 3D, particles, gradients. Motion over static pictures.
        - Fill the whole viewport, be responsive to any screen size, keep content centered, avoid scrollbars.
        - Touch targets at least 44px. Aim for smooth 60fps; cap device pixel ratio at 2.
        - Never ask questions and never refuse: if the command is vague, choose the most impressive reasonable interpretation.
        - If the user writes in Portuguese, keep visible page text in Portuguese (pt-BR).
        - If the message includes a "Current page" HTML document, that is the page currently on screen: treat the command as an edit to it - change only what was asked, keep everything else intact, and output the complete updated document. Only build a page from scratch when no current page is provided.
    """.trimIndent()

    /** Prompt de sistema com a paleta do tema e o nível de detalhe anexados. */
    fun systemPrompt(theme: NexusTheme, detailLevel: Int = 1): String = buildString {
        append(BASE_PROMPT)
        append(detailDirective(detailLevel))
        append("\n\nCurrent app theme - use these exact colors as the main palette:\n")
        append("- background: ").append(theme.hex(theme.background)).append('\n')
        append("- accent (primary/highlights): ").append(theme.hex(theme.accent)).append('\n')
        append("- text: ").append(theme.hex(theme.text)).append('\n')
        append("- secondary text: ").append(theme.hex(theme.secondary)).append('\n')
        append("- surface (cards/panels): ").append(theme.hex(theme.surface)).append('\n')
        append("- surface alt: ").append(theme.hex(theme.surfaceAlt)).append('\n')
        append("Respect the palette: dark background means dark design, light background means light design.\n")
    }

    /** Diretriz de complexidade por nível: 0 = simples, 1 = médio, 2 = rico. */
    private fun detailDirective(level: Int): String = when (level.coerceIn(0, 2)) {
        0 -> "\n\nDetail level: MINIMAL. Keep it small and essential: no external libraries, no heavy effects - just a clean single-file page that answers the command."
        2 -> "\n\nDetail level: MAXIMUM. Deliver a rich, production-quality page: for anything 3D use three.js (CDN) with real lighting, soft shadows and materials; add subtle particles/background, smooth animation and polish. Do not skimp on detail."
        else -> ""
    }

    /**
     * Mensagem do usuário para o chat. Com uma página em tela (refinamento), anexa o
     * documento atual ao comando para o modelo editar o que está na tela — sem isso,
     * pedidos como "agora azul" geram uma página nova sem contexto.
     */
    fun userPrompt(command: String, currentDocument: String?): String {
        if (currentDocument.isNullOrBlank()) return command
        return buildString {
            append("Current page (the HTML document currently displayed on screen):\n")
            append("```html\n")
            append(currentDocument)
            append("\n```\n\n")
            append("Command: ")
            append(command)
        }
    }

    /** Extrai o documento HTML cru da resposta do modelo (sem cercas markdown). */
    fun extractDocument(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```")) s = s.substringAfter('\n', s).trim()
        val lower = s.lowercase()
        val doctype = lower.indexOf("<!doctype")
        val htmlIdx = lower.indexOf("<html")
        val cut = listOf(doctype, htmlIdx).filter { it > 0 }.minOrNull()
        if (cut != null) s = s.substring(cut)
        if (s.endsWith("```")) s = s.substring(0, s.length - 3).trim()
        return s
    }

    /** Injeta meta viewport (se faltar) e o CSS base com as variáveis do tema. */
    fun decorate(theme: NexusTheme, html: String): String {
        val builder = StringBuilder()
        if (!VIEWPORT_REGEX.containsMatchIn(html)) {
            builder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1, viewport-fit=cover\">\n")
        }
        builder.append("<style id=\"nx77-base\">\n")
            .append(varsCss(theme))
            .append('\n')
            .append(baseCss())
            .append("\n</style>")
        val injection = builder.toString()

        HEAD_REGEX.find(html)?.let { head ->
            val at = head.range.last + 1
            return html.substring(0, at) + "\n" + injection + "\n" + html.substring(at)
        }
        HTML_REGEX.find(html)?.let { tag ->
            val at = tag.range.last + 1
            return html.substring(0, at) + "\n<head>\n" + injection + "\n</head>" + html.substring(at)
        }
        return "<!DOCTYPE html><html><head>\n" + injection + "\n</head><body>\n" + html + "\n</body></html>"
    }

    fun show(web: WebView, html: String) {
        web.loadDataWithBaseURL(BASE, html, "text/html", "UTF-8", null)
    }

    /** Página inicial (cubo girando + dicas) enquanto nada foi gerado. */
    fun welcomePage(theme: NexusTheme, hasKey: Boolean): String {
        val bg = theme.hex(theme.background)
        val fg = theme.hex(theme.text)
        val secondary = theme.hex(theme.secondary)
        val accent = theme.hex(theme.accent)
        val accentSoft = theme.hexA(theme.accent, 140)
        val accentFaint = theme.hexA(theme.accent, 20)
        val warn = if (hasKey) "" else """<div class="warn">⚙ Configure sua API key para começar</div>"""
        val chips = listOf(
            "criar cubo html 3d",
            "relógio digital neon",
            "sistema solar interativo",
            "partículas conectadas",
            "dashboard de energia"
        ).joinToString("") { """<button class="chip" data-p="${esc(it)}">${esc(it)}</button>""" }

        return """<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, viewport-fit=cover">
<title>NEXUS77</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
html,body{height:100%}
body{background:$bg;color:$fg;font-family:-apple-system,'Segoe UI',Roboto,sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:32px 22px;gap:6px;overflow:hidden}
.scene{width:120px;height:120px;perspective:600px;margin-bottom:12px}
.cube{width:80px;height:80px;margin:20px;position:relative;transform-style:preserve-3d;animation:spin 9s linear infinite}
.face{position:absolute;top:0;left:0;right:0;bottom:0;border:2px solid $accent;background:$accentFaint;box-shadow:0 0 18px $accentSoft inset;border-radius:4px}
.f1{transform:translateZ(40px)}
.f2{transform:rotateY(180deg) translateZ(40px)}
.f3{transform:rotateY(90deg) translateZ(40px)}
.f4{transform:rotateY(-90deg) translateZ(40px)}
.f5{transform:rotateX(90deg) translateZ(40px)}
.f6{transform:rotateX(-90deg) translateZ(40px)}
@keyframes spin{from{transform:rotateX(-18deg) rotateY(0deg)}to{transform:rotateX(-18deg) rotateY(360deg)}}
h1{font-family:'Courier New',monospace;font-size:30px;letter-spacing:.35em;text-indent:.35em;color:$accent;text-shadow:0 0 24px $accentSoft}
.sub{color:$secondary;font-size:13.5px;line-height:1.6;max-width:340px}
.warn{margin-top:12px;font-size:12.5px;color:$accent;background:$accentFaint;border:1px solid $accentSoft;border-radius:10px;padding:8px 12px}
.chips{display:flex;flex-wrap:wrap;justify-content:center;gap:8px;margin-top:20px;max-width:360px}
.chip{border:1px solid $accentSoft;color:$accent;background:$accentFaint;border-radius:999px;padding:10px 14px;font-size:12.5px;font-family:'Courier New',monospace;cursor:pointer;-webkit-tap-highlight-color:transparent}
.chip:active{background:$accentSoft;color:$bg}
</style>
</head>
<body>
<div class="scene"><div class="cube"><div class="face f1"></div><div class="face f2"></div><div class="face f3"></div><div class="face f4"></div><div class="face f5"></div><div class="face f6"></div></div></div>
<h1>NEXUS77</h1>
<p class="sub">Descreva um comando e eu construo a página — HTML vivo, direto no seu bolso.</p>
$warn
<div class="chips">$chips</div>
<script>
function pick(p){
  try{ Nexus77.setInput(p); return; }catch(e){}
  try{ navigator.clipboard.writeText(p); }catch(e){}
}
document.querySelectorAll('.chip').forEach(function(c){
  c.addEventListener('click', function(){ pick(c.getAttribute('data-p')); });
});
</script>
</body>
</html>"""
    }

    /** Página de erro exibida quando ainda não há documento gerado. */
    fun errorPage(theme: NexusTheme, title: String, detail: String): String {
        val bg = theme.hex(theme.background)
        val surface = theme.hex(theme.surface)
        val fg = theme.hex(theme.text)
        val secondary = theme.hex(theme.secondary)
        val accent = theme.hex(theme.accent)
        val border = theme.hexA(theme.accent, 70)
        return """<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>NEXUS77</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
html,body{height:100%}
body{background:$bg;color:$fg;font-family:-apple-system,'Segoe UI',Roboto,sans-serif;display:flex;align-items:center;justify-content:center;padding:28px}
.card{max-width:380px;text-align:center;background:$surface;border:1px solid $border;border-radius:16px;padding:26px 22px}
.icon{font-size:34px;margin-bottom:10px}
h2{font-family:'Courier New',monospace;font-size:15px;letter-spacing:.18em;text-transform:uppercase;color:$accent;margin-bottom:10px}
p{color:$secondary;font-size:13.5px;line-height:1.6;word-break:break-word}
</style>
</head>
<body>
<div class="card"><div class="icon">⚠️</div><h2>${esc(title)}</h2><p>${esc(detail)}</p></div>
</body>
</html>"""
    }

    /** Item da página "Abrir" (páginas salvas). */
    data class SavedEntry(val name: String, val meta: String)

    /** Página que lista o que está salvo em `Downloads/nexus77` (+ abrir arquivo do celular). */
    fun savedListPage(theme: NexusTheme, entries: List<SavedEntry>): String {
        val bg = theme.hex(theme.background)
        val surface = theme.hex(theme.surface)
        val surfaceAlt = theme.hex(theme.surfaceAlt)
        val fg = theme.hex(theme.text)
        val secondary = theme.hex(theme.secondary)
        val accent = theme.hex(theme.accent)
        val accentSoft = theme.hexA(theme.accent, 140)
        val accentFaint = theme.hexA(theme.accent, 22)
        val border = theme.hexA(theme.accent, 70)

        val body = if (entries.isEmpty()) {
            """<div class="empty">Nada salvo ainda.<br><span>Gere uma página e toque em <b>SALVAR</b>.</span></div>"""
        } else {
            entries.joinToString("") { e ->
                val n = escAttr(e.name)
                """<div class="row">""" +
                    """<button class="open" data-n="$n">""" +
                    """<span class="fname">${esc(e.name)}</span>""" +
                    """<span class="fmeta">${esc(e.meta)}</span></button>""" +
                    """<button class="del" data-n="$n" title="Excluir">✕</button></div>"""
            }
        }

        return """<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, viewport-fit=cover">
<title>Salvos — NEXUS77</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
html,body{height:100%}
body{background:$bg;color:$fg;font-family:-apple-system,'Segoe UI',Roboto,sans-serif;display:flex;flex-direction:column;overflow:hidden}
.top{display:flex;align-items:center;gap:10px;padding:14px 16px 6px}
.back{border:1px solid $border;background:$accentFaint;color:$accent;border-radius:999px;padding:8px 14px;font-size:13px;font-family:'Courier New',monospace;cursor:pointer;-webkit-tap-highlight-color:transparent}
.back:active{background:$accentSoft;color:$bg}
.ttl{font-family:'Courier New',monospace;font-size:14px;letter-spacing:.22em;color:$accent}
.pick{margin-left:auto;border:1px solid $border;background:$accentFaint;color:$accent;border-radius:999px;padding:8px 14px;font-size:12.5px;font-family:'Courier New',monospace;cursor:pointer;-webkit-tap-highlight-color:transparent}
.pick:active{background:$accentSoft;color:$bg}
.path{padding:0 18px 6px;color:$secondary;font-size:11.5px;font-family:'Courier New',monospace}
.list{flex:1;overflow-y:auto;padding:6px 16px 24px;display:flex;flex-direction:column;gap:10px;-webkit-overflow-scrolling:touch}
.row{display:flex;align-items:stretch;gap:8px}
.open{flex:1;display:flex;flex-direction:column;gap:3px;text-align:left;background:$surface;border:1px solid $border;border-radius:14px;padding:14px 15px;cursor:pointer;-webkit-tap-highlight-color:transparent}
.open:active{background:$surfaceAlt}
.fname{color:$fg;font-size:14px;word-break:break-all}
.fmeta{color:$secondary;font-size:11.5px;font-family:'Courier New',monospace}
.del{width:50px;background:$surface;border:1px solid $border;color:$accent;border-radius:14px;font-size:18px;cursor:pointer;-webkit-tap-highlight-color:transparent}
.del:active{background:$accentFaint}
.empty{margin:auto;text-align:center;color:$secondary;font-size:13.5px;line-height:1.7;padding:24px}
.empty span{font-size:12px}
.empty b{color:$accent;font-family:'Courier New',monospace}
</style>
</head>
<body>
<div class="top"><button class="back" id="bk">‹ Voltar</button><span class="ttl">SALVOS</span><button class="pick" id="pk">Abrir HTML…</button></div>
<div class="path">Downloads/nexus77</div>
<div class="list">$body</div>
<script>
document.getElementById('bk').addEventListener('click', function(){ try{ Nexus77.closeList(); }catch(e){} });
document.getElementById('pk').addEventListener('click', function(){ try{ Nexus77.pickFile(); }catch(e){} });
document.querySelectorAll('.open[data-n]').forEach(function(c){
  c.addEventListener('click', function(){ try{ Nexus77.openSaved(c.getAttribute('data-n')); }catch(e){} });
});
document.querySelectorAll('.del[data-n]').forEach(function(c){
  c.addEventListener('click', function(){ try{ Nexus77.deleteSaved(c.getAttribute('data-n')); }catch(e){} });
});
</script>
</body>
</html>"""
    }

    private fun varsCss(theme: NexusTheme): String = """
        :root{
          --nx-bg: ${theme.hex(theme.background)};
          --nx-surface: ${theme.hex(theme.surface)};
          --nx-surface-alt: ${theme.hex(theme.surfaceAlt)};
          --nx-accent: ${theme.hex(theme.accent)};
          --nx-accent-soft: ${theme.hexA(theme.accent, 140)};
          --nx-accent-faint: ${theme.hexA(theme.accent, 20)};
          --nx-text: ${theme.hex(theme.text)};
          --nx-secondary: ${theme.hex(theme.secondary)};
        }
    """.trimIndent()

    private fun baseCss(): String = """
        html,body{margin:0;padding:0;background:var(--nx-bg);color:var(--nx-text);overscroll-behavior:none}
        ::-webkit-scrollbar{width:0;height:0}
        ::selection{background:var(--nx-accent-soft);color:var(--nx-text)}
        a{color:var(--nx-accent)}
    """.trimIndent()

    private fun esc(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;")

    private fun escAttr(s: String): String = esc(s).replace("\"", "&quot;")
}
