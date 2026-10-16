package com.leo.nexus77

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

/**
 * Tela de configurações: endpoint/chave/modelo, busca de modelos na API,
 * temperatura, max tokens, tema ativo e persistência.
 */
class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var theme: NexusTheme

    private lateinit var endpointField: EditText
    private lateinit var keyField: EditText
    private lateinit var modelField: EditText
    private lateinit var fetchButton: Button
    private lateinit var tempLabel: TextView
    private lateinit var tempBar: SeekBar
    private lateinit var tokensField: EditText
    private lateinit var detailRadios: MutableList<RadioButton>
    private lateinit var themeRadios: MutableList<RadioButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        theme = NexusTheme.of(prefs.themeIndex)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        buildUi()
        applySystemBars()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(36))
        }

        val header = TextView(this).apply {
            text = "CONFIGURAÇÕES"
            textSize = 16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.22f
            setTextColor(theme.accent)
        }
        col.addView(header)

        col.addView(section("API"))
        endpointField = addField(
            col,
            "Endpoint (compatível OpenAI)",
            prefs.endpoint,
            "https://api.openai.com/v1",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        )
        keyField = addField(
            col,
            "API key",
            prefs.apiKey,
            "sk-…",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        val showKey = CheckBox(this).apply {
            text = "ver chave"
            textSize = 13f
            setTextColor(theme.secondary)
            buttonTintList = ColorStateList.valueOf(theme.accent)
            setPadding(0, dp(8), 0, 0)
        }
        showKey.setOnCheckedChangeListener { _, checked ->
            val pos = keyField.selectionEnd.coerceIn(0, keyField.text.length)
            keyField.inputType = if (checked) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            keyField.isSingleLine = true
            keyField.setSelection(pos)
        }
        col.addView(showKey)

        col.addView(section("GERAÇÃO"))
        modelField = addField(col, "Modelo", prefs.model, "gpt-4o-mini", InputType.TYPE_CLASS_TEXT)

        fetchButton = Button(this).apply {
            text = "Buscar modelos"
            isAllCaps = false
            textSize = 14f
            setTextColor(theme.text)
            setOnClickListener { fetchModels() }
        }
        val fetchLp = LinearLayout.LayoutParams(MATCH_PARENT, dp(46))
        fetchLp.topMargin = dp(10)
        col.addView(fetchButton, fetchLp)

        tempLabel = TextView(this).apply {
            text = String.format(Locale.US, "Temperatura: %.2f", prefs.temperature)
            textSize = 12.5f
            typeface = Typeface.MONOSPACE
            setTextColor(theme.secondary)
            setPadding(0, dp(14), 0, dp(2))
        }
        col.addView(tempLabel)

        tempBar = SeekBar(this).apply {
            max = 200
            progress = (prefs.temperature * 100).toInt().coerceIn(0, 200)
            progressTintList = ColorStateList.valueOf(theme.accent)
            thumbTintList = ColorStateList.valueOf(theme.accent)
            progressBackgroundTintList = ColorStateList.valueOf(theme.surfaceAlt)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    tempLabel.text = String.format(Locale.US, "Temperatura: %.2f", value / 100f)
                }

                override fun onStartTrackingTouch(bar: SeekBar?) = Unit

                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
        col.addView(tempBar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val detailLabel = TextView(this).apply {
            text = "Nível de detalhe"
            textSize = 12.5f
            setTextColor(theme.secondary)
            setPadding(0, dp(14), 0, dp(2))
        }
        col.addView(detailLabel)
        val detailGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        detailRadios = ArrayList()
        listOf("Simples", "Médio", "Rico").forEachIndexed { index, label ->
            val radio = RadioButton(this).apply {
                id = 2000 + index
                text = label
                textSize = 13f
                setTextColor(theme.text)
                buttonTintList = ColorStateList.valueOf(theme.accent)
                isChecked = index == prefs.detailLevel
                setPadding(0, dp(4), dp(10), dp(4))
            }
            detailRadios.add(radio)
            detailGroup.addView(radio)
        }
        col.addView(detailGroup, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        tokensField = addField(
            col,
            "Max tokens (0 = sem limite)",
            prefs.maxTokens.toString(),
            "0",
            InputType.TYPE_CLASS_NUMBER
        )

        col.addView(section("TEMA"))
        val group = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        themeRadios = ArrayList()
        fun addThemeGroup(label: String, entries: List<Pair<Int, NexusTheme>>) {
            if (entries.isEmpty()) return
            group.addView(TextView(this).apply {
                text = label
                textSize = 11.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                letterSpacing = 0.12f
                setTextColor(theme.accent)
                setPadding(0, dp(12), 0, dp(4))
            })
            entries.forEach { (index, item) ->
                val radio = RadioButton(this).apply {
                    id = 1000 + index
                    text = item.name
                    textSize = 14f
                    setTextColor(theme.text)
                    buttonTintList = ColorStateList.valueOf(theme.accent)
                    isChecked = index == prefs.themeIndex
                    setPadding(0, dp(6), 0, dp(6))
                }
                themeRadios.add(radio)
                group.addView(radio, RadioGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
        }
        val indexedThemes = NexusTheme.ALL.withIndex()
        addThemeGroup("ESCUROS", indexedThemes.filter { it.value.isDark }.map { it.index to it.value })
        addThemeGroup("CLAROS", indexedThemes.filter { !it.value.isDark }.map { it.index to it.value })
        col.addView(group, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val saveButton = Button(this).apply {
            text = "Salvar configurações"
            isAllCaps = false
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(theme.onAccent)
            setOnClickListener { save() }
        }
        val saveLp = LinearLayout.LayoutParams(MATCH_PARENT, dp(52))
        saveLp.topMargin = dp(24)
        col.addView(saveButton, saveLp)

        scroll.setBackgroundColor(theme.background)
        scroll.addView(col, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(scroll)
    }

    // ---------- Busca de modelos ----------

    private fun fetchModels() {
        val endpoint = endpointField.text.toString().trim()
        if (!endpoint.startsWith("http")) {
            toast("Endpoint deve começar com http")
            return
        }
        if (keyField.text.toString().trim().isEmpty()) {
            toast("Informe a API key")
            return
        }
        fetchButton.isEnabled = false
        fetchButton.text = "buscando…"
        Thread {
            val result = runCatching {
                AiClient.listModels(endpoint, keyField.text.toString().trim())
            }
            runOnUiThread {
                fetchButton.isEnabled = true
                fetchButton.text = "Buscar modelos"
                result.fold(
                    onSuccess = { models ->
                        if (models.isEmpty()) {
                            toast("Nenhum modelo retornado")
                        } else {
                            showModelsDialog(models)
                        }
                    },
                    onFailure = { e ->
                        toast(AiClient.errorMessage(e))
                    }
                )
            }
        }.start()
    }

    private fun showModelsDialog(models: List<String>) {
        val current = modelField.text.toString().trim()
        val preselect = models.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Modelos")
            .setSingleChoiceItems(models.toTypedArray(), preselect) { dialog, which ->
                modelField.setText(models[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------- Persistência ----------

    private fun save() {
        val endpoint = endpointField.text.toString().trim()
        if (!endpoint.startsWith("http")) {
            toast("Endpoint deve começar com http")
            return
        }
        val selected = themeRadios.firstOrNull { it.isChecked }?.id?.minus(1000) ?: -1
        prefs.endpoint = endpoint
        prefs.apiKey = keyField.text.toString()
        prefs.model = modelField.text.toString()
        prefs.maxTokens = tokensField.text.toString().trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
        prefs.temperature = tempBar.progress / 100f
        if (selected >= 0) prefs.themeIndex = selected
        val detail = detailRadios.indexOfFirst { it.isChecked }
        if (detail >= 0) prefs.detailLevel = detail
        toast("Configurações salvas ✓")
        finish()
    }

    // ---------- Tema / utilitários ----------

    @Suppress("DEPRECATION")
    private fun applySystemBars() {
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

    private fun section(title: String): TextView = TextView(this).apply {
        text = title
        textSize = 12f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        letterSpacing = 0.18f
        setTextColor(theme.accent)
        setPadding(0, dp(22), 0, dp(8))
    }

    private fun addField(
        parent: LinearLayout,
        label: String,
        value: String,
        placeholder: String,
        fieldType: Int
    ): EditText {
        val lbl = TextView(this).apply {
            text = label
            textSize = 12.5f
            setTextColor(theme.secondary)
            setPadding(0, dp(12), 0, dp(6))
        }
        parent.addView(lbl)
        val field = EditText(this)
        field.setText(value)
        field.hint = placeholder
        field.textSize = 14f
        field.isSingleLine = true
        field.inputType = fieldType
        field.setTextColor(theme.text)
        field.setHintTextColor(theme.secondary)
        field.setPadding(dp(12), dp(10), dp(12), dp(10))
        field.background = theme.rounded(theme.surface, dp(10).toFloat())
        parent.addView(field, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        return field
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
