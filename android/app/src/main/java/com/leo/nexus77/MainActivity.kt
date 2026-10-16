package com.leo.nexus77

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela principal: título, linha de sessão (modelo de IA em uso, barra de
 * tokens e reset), WebView com o resultado gerado pela IA e barra de
 * comando (input + enviar + engrenagem de configurações).
 */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var theme: NexusTheme

    private lateinit var root: LinearLayout
    private lateinit var titleBar: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var sessionRow: LinearLayout
    private lateinit var dotView: View
    private lateinit var modelText: TextView
    private lateinit var tokenBar: ProgressBar
    private lateinit var tokenLabel: TextView
    private lateinit var resetBtn: TextView
    private lateinit var statusText: TextView
    private lateinit var web: WebView
    private lateinit var inputBar: LinearLayout
    private lateinit var input: EditText
    private lateinit var sendBtn: ImageButton
    private lateinit var gearBtn: ImageButton
    private lateinit var saveBtn: TextView
    private lateinit var openBtn: TextView
    private lateinit var statusRow: LinearLayout
    private lateinit var vuMeter: VuMeterView
    private lateinit var kbLabel: TextView

    private var lastThemeIndex = -1
    private var lastRendered: String? = null
    private var lastPrompt: String? = null
    private var generating = false
    private var showingList = false

    // Estimativas ao vivo da geração em andamento (contadores durante o carregamento)
    private var liveActive = false
    private var liveIn = 0
    private var liveOut = 0
    private var lastProgressAt = 0L

    private val reqStorage = 41
    private val reqOpenDoc = 51
    private var pendingStorageAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        theme = NexusTheme.of(prefs.themeIndex)
        lastThemeIndex = prefs.themeIndex
        buildUi()
        applyTheme()
    }

    override fun onResume() {
        super.onResume()
        val changed = prefs.themeIndex != lastThemeIndex
        theme = NexusTheme.of(prefs.themeIndex)
        lastThemeIndex = prefs.themeIndex
        applyTheme()
        val rendered = lastRendered
        if (rendered == null) {
            showWelcome()
        } else if (changed) {
            Html.show(web, Html.decorate(theme, rendered))
        }
    }

    // ---------- UI ----------

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(6))
        }
        titleText = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 19f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.35f
        }
        saveBtn = headerButton("SALVAR") { requestSave() }
        openBtn = headerButton("ABRIR") { openSavedList() }
        statusText = TextView(this).apply {
            textSize = 11.5f
            typeface = Typeface.MONOSPACE
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.START
            setPadding(0, dp(3), 0, 0)
        }
        vuMeter = VuMeterView(this).apply {
            visibility = View.GONE
        }
        kbLabel = TextView(this).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            visibility = View.GONE
            setPadding(dp(8), dp(3), dp(6), 0)
        }
        statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        statusRow.addView(vuMeter, LinearLayout.LayoutParams(dp(54), dp(20)))
        statusRow.addView(kbLabel, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        statusRow.addView(statusText, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        sessionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        dotView = View(this).apply {
            background = dotDrawable(theme.secondary)
        }
        modelText = TextView(this).apply {
            textSize = 11.5f
            typeface = Typeface.MONOSPACE
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        tokenBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = Prefs.SESSION_TOKEN_BUDGET
            progress = 0
        }
        tokenLabel = TextView(this).apply {
            textSize = 10.5f
            typeface = Typeface.MONOSPACE
            maxLines = 1
        }
        resetBtn = TextView(this).apply {
            text = "↺"
            textSize = 17f
            gravity = Gravity.CENTER
            contentDescription = "Reiniciar sessão"
            setOnClickListener { resetSession() }
        }
        sessionRow.addView(dotView, LinearLayout.LayoutParams(dp(7), dp(7)))
        val modelLp = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        modelLp.marginStart = dp(7)
        sessionRow.addView(modelText, modelLp)
        val barLp = LinearLayout.LayoutParams(dp(66), dp(6))
        barLp.marginStart = dp(10)
        barLp.marginEnd = dp(7)
        sessionRow.addView(tokenBar, barLp)
        sessionRow.addView(tokenLabel, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        val resetLp = LinearLayout.LayoutParams(dp(34), dp(28))
        resetLp.marginStart = dp(10)
        sessionRow.addView(resetBtn, resetLp)
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topRow.addView(titleText, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        topRow.addView(saveBtn, headerBtnParams())
        topRow.addView(openBtn, headerBtnParams())
        titleBar.addView(topRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        titleBar.addView(sessionRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        titleBar.addView(statusRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        web = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.mediaPlaybackRequiresUserGesture = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isFocusable = true
            isFocusableInTouchMode = true
            addJavascriptInterface(NxBridge(), "Nexus77")
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    handleUrl(request.url.toString())

                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    handleUrl(url)
            }
        }

        inputBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(10), dp(10))
        }
        input = EditText(this).apply {
            hint = "Ex: criar cubo html 3d"
            textSize = 14f
            isSingleLine = true
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendPrompt()
                    true
                } else {
                    false
                }
            }
        }
        sendBtn = ImageButton(this).apply {
            setImageResource(R.drawable.ic_send)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(11), dp(11), dp(11), dp(11))
            contentDescription = "Gerar"
            setOnClickListener { sendPrompt() }
        }
        gearBtn = ImageButton(this).apply {
            setImageResource(R.drawable.ic_gear)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(11), dp(11), dp(11), dp(11))
            contentDescription = "Configurações"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }

        val inputLp = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        inputLp.marginEnd = dp(8)
        inputBar.addView(input, inputLp)
        val sendLp = LinearLayout.LayoutParams(dp(46), dp(46))
        sendLp.marginEnd = dp(8)
        inputBar.addView(sendBtn, sendLp)
        inputBar.addView(gearBtn, LinearLayout.LayoutParams(dp(46), dp(46)))

        root.addView(titleBar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(web)
        root.addView(inputBar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(root)
    }

    @Suppress("DEPRECATION")
    private fun applyTheme() {
        root.setBackgroundColor(theme.background)
        titleBar.setBackgroundColor(theme.surface)
        titleText.setTextColor(theme.accent)
        statusText.setTextColor(theme.secondary)
        kbLabel.setTextColor(theme.accent)
        vuMeter.setColors(
            theme.accent,
            (theme.accent and 0x00FFFFFF) or 0x33000000,
            (theme.secondary and 0x00FFFFFF) or 0x1A000000
        )
        web.setBackgroundColor(theme.background)
        inputBar.setBackgroundColor(theme.surface)

        input.setTextColor(theme.text)
        input.setHintTextColor(theme.secondary)
        input.background = theme.rounded(theme.surfaceAlt, dp(22).toFloat())
        input.setPadding(dp(16), dp(10), dp(16), dp(10))

        sendBtn.background = theme.rounded(theme.accent, dp(14).toFloat())
        sendBtn.setColorFilter(theme.onAccent)
        gearBtn.background = theme.rounded(theme.surfaceAlt, dp(14).toFloat())
        gearBtn.setColorFilter(theme.secondary)

        modelText.setTextColor(theme.text)
        tokenLabel.setTextColor(theme.secondary)
        resetBtn.setTextColor(theme.accent)
        resetBtn.background = theme.rounded(theme.surfaceAlt, dp(9).toFloat())
        saveBtn.setTextColor(theme.accent)
        saveBtn.background = theme.rounded(theme.surfaceAlt, dp(9).toFloat())
        openBtn.setTextColor(theme.accent)
        openBtn.background = theme.rounded(theme.surfaceAlt, dp(9).toFloat())
        tokenBar.progressDrawable = barDrawable(theme)
        updateSessionUi()

        window.statusBarColor = theme.background
        window.navigationBarColor = theme.background
        var flags = window.decorView.systemUiVisibility
        flags = if (theme.isDark) {
            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (Build.VERSION.SDK_INT >= 26) {
            flags = if (theme.isDark) {
                flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            } else {
                flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
        window.decorView.systemUiVisibility = flags
    }

    // ---------- Geração ----------

    private fun sendPrompt() {
        if (generating) return
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) {
            toast("Descreva o que você quer ver")
            return
        }
        if (!prefs.hasKey) {
            toast("Configure sua API key em ⚙")
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }
        generating = true
        showingList = false
        lastPrompt = prompt
        sendBtn.isEnabled = false
        sendBtn.alpha = 0.55f
        saveBtn.isEnabled = false
        openBtn.isEnabled = false
        statusText.setTextColor(theme.accent)
        statusText.gravity = Gravity.START
        statusText.text = if (lastRendered != null) "ajustando…" else "gerando…"
        setLoading(true)
        hideKeyboard()

        val endpoint = prefs.endpoint
        val apiKey = prefs.apiKey
        val model = prefs.model
        val temperature = prefs.temperature
        val maxTokens = prefs.maxTokens
        val system = Html.systemPrompt(theme, prefs.detailLevel)
        val user = Html.userPrompt(prompt, lastRendered)
        liveActive = true
        liveIn = (system.toByteArray(Charsets.UTF_8).size + user.toByteArray(Charsets.UTF_8).size) / 4
        liveOut = 0
        lastProgressAt = 0L
        updateSessionUi()
        val started = System.currentTimeMillis()

        Thread {
            val result = runCatching {
                AiClient.chat(endpoint, apiKey, model, temperature, maxTokens, system, user) { bytes ->
                    runOnUiThread { onGenProgress(bytes) }
                }
            }
            val elapsed = (System.currentTimeMillis() - started) / 1000.0
            runOnUiThread {
                result.fold(
                    onSuccess = { result ->
                        val doc = Html.extractDocument(result.content)
                        if (doc.isBlank()) {
                            failGeneration("resposta vazia do modelo")
                        } else {
                            lastRendered = doc
                            Html.show(web, Html.decorate(theme, doc))
                            val kb = (doc.toByteArray(Charsets.UTF_8).size + 1023) / 1024
                            liveActive = false
                            val tokSuffix = if (result.totalTokens > 0) {
                                prefs.addSessionTokens(result.promptTokens, result.completionTokens)
                                updateSessionUi()
                                String.format(
                                    Locale.US, " • ↑%s ↓%s tok",
                                    formatTokens(result.promptTokens), formatTokens(result.completionTokens)
                                )
                            } else {
                                ""
                            }
                            statusText.setTextColor(theme.secondary)
                            statusText.gravity = Gravity.END
                            statusText.text = String.format(
                                Locale.US, "✓ %d KB • %.1fs • %s%s", kb, elapsed, model, tokSuffix
                            )
                            input.setText("")
                            input.clearFocus()
                            finishGeneration()
                        }
                    },
                    onFailure = { e ->
                        failGeneration(AiClient.errorMessage(e))
                    }
                )
            }
        }.start()
    }

    private fun failGeneration(message: String) {
        statusText.setTextColor(errorColor)
        statusText.gravity = Gravity.START
        statusText.text = "✗ $message"
        if (lastRendered == null) {
            Html.show(web, Html.errorPage(theme, "Falha na geração", message))
        }
        toast(message)
        finishGeneration()
    }

    private fun finishGeneration() {
        generating = false
        sendBtn.isEnabled = true
        sendBtn.alpha = 1f
        saveBtn.isEnabled = true
        openBtn.isEnabled = true
        liveActive = false
        updateSessionUi()
        setLoading(false)
    }

    /** Liga/desliga o VU meter + o contador de KB ao lado do status. */
    private fun setLoading(on: Boolean) {
        if (on) {
            kbLabel.text = "…"
            kbLabel.visibility = View.VISIBLE
            vuMeter.start()
        } else {
            vuMeter.stop()
            kbLabel.visibility = View.GONE
        }
    }

    /** Tamanho do documento em formação (B / KB / MB). */
    private fun kbText(bytes: Int): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1048576 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
    }

    /**
     * Progresso ao vivo (thread de UI). O KB é o tamanho **real** do documento que está
     * sendo recebido; os tokens são **estimados** (~) durante a geração e trocados pelo
     * valor exato da API ao finalizar.
     */
    private fun onGenProgress(contentBytes: Int) {
        val now = System.currentTimeMillis()
        if (now - lastProgressAt < 60L) return   // limita a ~16 atualizações/s
        lastProgressAt = now
        kbLabel.text = kbText(contentBytes)
        liveOut = contentBytes / 4
        updateSessionUi()
    }

    private fun showWelcome() {
        showingList = false
        statusText.setTextColor(theme.secondary)
        statusText.gravity = Gravity.START
        statusText.text = if (prefs.hasKey) {
            "pronto — descreva o que quer ver"
        } else {
            "configure a API key em ⚙ para começar"
        }
        Html.show(web, Html.welcomePage(theme, prefs.hasKey))
    }

    // ---------- Sessão (modelo / tokens / reset) ----------

    /** Zera o consumo da sessão e volta à tela inicial. */
    private fun resetSession() {
        if (generating) {
            toast("Aguarde a geração atual terminar")
            return
        }
        prefs.sessionTokens = 0
        prefs.sessionPromptTokens = 0
        prefs.sessionCompletionTokens = 0
        lastRendered = null
        updateSessionUi()
        showWelcome()
        toast("Sessão reiniciada")
    }

    /** Atualiza dot do modelo, barra de tokens e rótulo com o estado atual. */
    private fun updateSessionUi() {
        val keySet = prefs.hasKey
        dotView.background = dotDrawable(if (keySet) theme.accent else errorColor)
        modelText.text = if (keySet) prefs.model else "sem API key"
        val extraIn = if (liveActive) liveIn else 0
        val extraOut = if (liveActive) liveOut else 0
        val inTok = prefs.sessionPromptTokens + extraIn
        val outTok = prefs.sessionCompletionTokens + extraOut
        val used = prefs.sessionTokens + extraIn + extraOut
        tokenBar.max = Prefs.SESSION_TOKEN_BUDGET
        tokenBar.progress = used.coerceAtMost(Prefs.SESSION_TOKEN_BUDGET)
        tokenLabel.text = String.format(
            Locale.US, "%s↑%s ↓%s · %s/%s",
            if (liveActive) "~" else "",
            formatTokens(inTok),
            formatTokens(outTok),
            formatTokens(used),
            formatTokens(Prefs.SESSION_TOKEN_BUDGET)
        )
    }

    private fun formatTokens(n: Int): String = when {
        n < 1000 -> n.toString()
        n < 1_000_000 -> if (n % 1000 == 0) "${n / 1000}k" else String.format(Locale.US, "%.1fk", n / 1000.0)
        n % 1_000_000 == 0 -> "${n / 1_000_000}M"
        else -> String.format(Locale.US, "%.2fM", n / 1_000_000.0)
    }

    private val errorColor: Int
        get() = if (theme.isDark) 0xFFFF5C5C.toInt() else 0xFFD32F2F.toInt()

    private fun barDrawable(t: NexusTheme): LayerDrawable {
        val track = t.rounded(t.surfaceAlt, dp(3).toFloat())
        val fill = t.rounded(t.accent, dp(3).toFloat())
        return LayerDrawable(arrayOf(track, fill)).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
    }

    private fun dotDrawable(color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }

    // ---------- Cabeçalho (salvar / abrir) ----------

    private fun headerButton(label: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(7), dp(12), dp(7))
            setOnClickListener { onClick() }
        }

    private fun headerBtnParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginStart = dp(8) }

    // ---------- Salvar / abrir (Downloads/nexus77) ----------

    private fun requestSave() {
        if (lastRendered == null) {
            toast("Gere uma página antes de salvar")
            return
        }
        withStorage { showSaveDialog() }
    }

    private fun showSaveDialog() {
        val field = EditText(this).apply {
            setText(defaultSaveName())
            textSize = 14f
            isSingleLine = true
            setTextColor(theme.text)
            setHintTextColor(theme.secondary)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = theme.rounded(theme.surfaceAlt, dp(10).toFloat())
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), 0)
        }
        box.addView(TextView(this).apply {
            text = "Pasta: Downloads/nexus77"
            textSize = 11.5f
            typeface = Typeface.MONOSPACE
            setTextColor(theme.secondary)
            setPadding(0, 0, 0, dp(8))
        })
        box.addView(field, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Salvar página")
            .setView(box)
            .setPositiveButton("Salvar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            field.setSelection(field.text.length)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(theme.accent)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(theme.secondary)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = field.text.toString().trim()
                if (name.isEmpty()) {
                    toast("Digite um nome")
                } else {
                    dialog.dismiss()
                    savePage(name)
                }
            }
        }
        dialog.show()
    }

    private fun savePage(rawName: String) {
        val doc = lastRendered ?: return
        val html = Html.decorate(theme, doc)
        val name = Store.fileName(rawName)
        saveBtn.isEnabled = false
        Thread {
            val result = runCatching { Store.save(this@MainActivity, name, html) }
            runOnUiThread {
                saveBtn.isEnabled = true
                result.fold(
                    onSuccess = { saved ->
                        statusText.setTextColor(theme.accent)
                        statusText.gravity = Gravity.START
                        statusText.text = "✓ salvo: $saved"
                        toast("Salvo em Downloads/nexus77/$saved")
                    },
                    onFailure = { e -> toast("Falha ao salvar: ${e.message}") }
                )
            }
        }.start()
    }

    private fun openSavedList() {
        withStorage {
            openBtn.isEnabled = false
            Thread {
                val result = runCatching { Store.list(this@MainActivity) }
                runOnUiThread {
                    openBtn.isEnabled = true
                    result.fold(
                        onSuccess = { items ->
                            showingList = true
                            val entries = items.map { Html.SavedEntry(it.name, Store.meta(it)) }
                            Html.show(web, Html.savedListPage(theme, entries))
                            statusText.setTextColor(theme.secondary)
                            statusText.gravity = Gravity.START
                            statusText.text = if (items.isEmpty()) {
                                "nada salvo ainda"
                            } else {
                                "${items.size} arquivo(s) em Downloads/nexus77"
                            }
                        },
                        onFailure = { e -> toast("Falha ao listar: ${e.message}") }
                    )
                }
            }.start()
        }
    }

    private fun openSaved(name: String) {
        Thread {
            val result = runCatching { Store.read(this@MainActivity, name) }
            runOnUiThread {
                result.fold(
                    onSuccess = { text ->
                        if (text.isNullOrBlank()) {
                            toast("Arquivo vazio ou não encontrado")
                        } else {
                            lastRendered = text
                            showingList = false
                            Html.show(web, text)
                            statusText.setTextColor(theme.secondary)
                            statusText.gravity = Gravity.START
                            statusText.text = "aberto: $name"
                        }
                    },
                    onFailure = { e -> toast("Falha ao abrir: ${e.message}") }
                )
            }
        }.start()
    }

    /** Volta da lista para a página atual (ou welcome). */
    private fun closeSavedList() {
        val doc = lastRendered
        if (doc == null) {
            showWelcome()
        } else {
            showingList = false
            Html.show(web, Html.decorate(theme, doc))
            statusText.setTextColor(theme.secondary)
            statusText.gravity = Gravity.START
            statusText.text = "página atual"
        }
    }

    /** Abre o seletor de arquivos do celular para escolher um HTML. */
    @Suppress("DEPRECATION")
    private fun pickHtmlFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/html", "application/xhtml+xml", "text/plain"))
        }
        try {
            startActivityForResult(intent, reqOpenDoc)
        } catch (_: Exception) {
            toast("Nenhum app para escolher arquivos")
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqOpenDoc && resultCode == RESULT_OK) {
            data?.data?.let { openPickedFile(it) }
        }
    }

    private fun openPickedFile(uri: Uri) {
        Thread {
            val result = runCatching {
                contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            }
            runOnUiThread {
                result.fold(
                    onSuccess = { text ->
                        if (text.isNullOrBlank()) {
                            toast("Arquivo vazio ou não legível")
                        } else {
                            lastRendered = text
                            showingList = false
                            Html.show(web, text)
                            statusText.setTextColor(theme.secondary)
                            statusText.text = "aberto do celular"
                        }
                    },
                    onFailure = { e -> toast("Falha ao abrir: ${e.message}") }
                )
            }
        }.start()
    }

    /** Exclui um salvo, com confirmação. */
    private fun deleteSaved(name: String) {
        AlertDialog.Builder(this)
            .setTitle("Excluir arquivo")
            .setMessage("Excluir \"$name\" de Downloads/nexus77?")
            .setPositiveButton("Excluir") { _, _ ->
                Thread {
                    val result = runCatching { Store.delete(this@MainActivity, name) }
                    runOnUiThread {
                        result.fold(
                            onSuccess = { ok ->
                                toast(if (ok) "Excluído" else "Não foi possível excluir")
                                openSavedList()
                            },
                            onFailure = { e -> toast("Falha ao excluir: ${e.message}") }
                        )
                    }
                }.start()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun defaultSaveName(): String {
        val slug = (lastPrompt ?: "").lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(40)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val stem = if (slug.isEmpty()) "pagina" else slug
        return "$stem-$stamp.html"
    }

    private fun withStorage(action: () -> Unit) {
        if (Store.needsPermission(this)) {
            pendingStorageAction = action
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                reqStorage
            )
        } else {
            action()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == reqStorage) {
            val action = pendingStorageAction
            pendingStorageAction = null
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                action?.invoke()
            } else {
                toast("Permita o armazenamento para usar Downloads/nexus77")
            }
        }
    }

    // ---------- Navegação / bridge ----------

    private fun handleUrl(url: String): Boolean {
        if (url.startsWith(Html.BASE) || url.startsWith("about:")) return false
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            true
        } catch (_: Exception) {
            true
        }
    }

    /** Exposto ao JS da página (window.Nexus77). */
    inner class NxBridge {
        @JavascriptInterface
        fun setInput(text: String) {
            runOnUiThread {
                input.setText(text)
                input.setSelection(input.text.length)
                input.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        /** Abre uma página salva (a partir da página "Abrir"). */
        @JavascriptInterface
        fun openSaved(name: String) {
            runOnUiThread { this@MainActivity.openSaved(name) }
        }

        /** Volta da página "Abrir" para a página atual. */
        @JavascriptInterface
        fun closeList() {
            runOnUiThread { closeSavedList() }
        }

        /** Abre o seletor de arquivos do celular (HTML). */
        @JavascriptInterface
        fun pickFile() {
            runOnUiThread { pickHtmlFile() }
        }

        /** Exclui um salvo (com confirmação nativa). */
        @JavascriptInterface
        fun deleteSaved(name: String) {
            runOnUiThread { this@MainActivity.deleteSaved(name) }
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        when {
            showingList -> closeSavedList()
            web.canGoBack() -> web.goBack()
            else -> super.onBackPressed()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(input.windowToken, 0)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
