package com.leo.nexus77

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import java.util.Locale

/**
 * Tema visual do app. As cores são injetadas no HTML gerado pela IA
 * (variáveis CSS --nx-*) e também usadas na UI nativa.
 */
data class NexusTheme(
    val name: String,
    val background: Int,
    val surface: Int,
    val surfaceAlt: Int,
    val accent: Int,
    val text: Int,
    val secondary: Int,
    val isDark: Boolean
) {
    /** Cor de conteúdo (ícone/texto) sobre o accent. */
    val onAccent: Int get() = if (isDark) 0xFF0A0E12.toInt() else Color.WHITE

    fun hex(color: Int): String = String.format(Locale.US, "#%06X", color and 0xFFFFFF)

    /** Formato CSS RRGGBBAA (alpha por último). */
    fun hexA(color: Int, alpha: Int): String =
        String.format(Locale.US, "#%06X%02X", color and 0xFFFFFF, alpha.coerceIn(0, 255))

    fun rounded(fill: Int, radiusPx: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = radiusPx
        }

    companion object {
        val ALL: List<NexusTheme> = listOf(
            NexusTheme(
                "NEXUS · ciano",
                0xFF0B0F14.toInt(), 0xFF10161E.toInt(), 0xFF161E28.toInt(),
                0xFF00E5FF.toInt(), 0xFFE8F0F5.toInt(), 0xFF7C8B99.toInt(), true
            ),
            NexusTheme(
                "MATRIX · verde",
                0xFF020A04.toInt(), 0xFF06170C.toInt(), 0xFF0A2113.toInt(),
                0xFF00FF41.toInt(), 0xFFD8FFE2.toInt(), 0xFF5E8F6C.toInt(), true
            ),
            NexusTheme(
                "NEON · roxo",
                0xFF15121D.toInt(), 0xFF1D1927.toInt(), 0xFF262032.toInt(),
                0xFFBD93F9.toInt(), 0xFFF0EAF8.toInt(), 0xFF8F86A3.toInt(), true
            ),
            NexusTheme(
                "OCEAN · azul",
                0xFF071A24.toInt(), 0xFF0C2735.toInt(), 0xFF113244.toInt(),
                0xFF00B4D8.toInt(), 0xFFE0F2F7.toInt(), 0xFF6D98A8.toInt(), true
            ),
            NexusTheme(
                "CLARO · branco",
                0xFFF2F5F9.toInt(), 0xFFFFFFFF.toInt(), 0xFFE6EBF1.toInt(),
                0xFF0066FF.toInt(), 0xFF101418.toInt(), 0xFF5B6672.toInt(), false
            ),
            NexusTheme(
                "SUNSET · laranja",
                0xFF160D08.toInt(), 0xFF1F1209.toInt(), 0xFF2A190D.toInt(),
                0xFFFF8C42.toInt(), 0xFFFCEEDF.toInt(), 0xFFA98A6F.toInt(), true
            ),
            NexusTheme(
                "SAKURA · rosa",
                0xFFFDF1F4.toInt(), 0xFFFFFFFF.toInt(), 0xFFF8E1E8.toInt(),
                0xFFD6336C.toInt(), 0xFF40232D.toInt(), 0xFF8D6A76.toInt(), false
            ),
            NexusTheme(
                "GOLD · dourado",
                0xFF12100A.toInt(), 0xFF1A1710.toInt(), 0xFF242016.toInt(),
                0xFFE8B84B.toInt(), 0xFFF7F1E1.toInt(), 0xFF9D9478.toInt(), true
            ),
            NexusTheme(
                "GELO · azul claro",
                0xFFEFF5FA.toInt(), 0xFFFFFFFF.toInt(), 0xFFDCEAF5.toInt(),
                0xFF1E88C7.toInt(), 0xFF152E3F.toInt(), 0xFF6C8A9E.toInt(), false
            ),
            NexusTheme(
                "MAGMA · vermelho",
                0xFF120707.toInt(), 0xFF1B0B0B.toInt(), 0xFF251010.toInt(),
                0xFFFF5757.toInt(), 0xFFFBE8E8.toInt(), 0xFFAA7B7B.toInt(), true
            ),
            NexusTheme(
                "GRAFITE · cinza",
                0xFF0E0E10.toInt(), 0xFF16161A.toInt(), 0xFF1E1E24.toInt(),
                0xFFC7CBD1.toInt(), 0xFFECEDEF.toInt(), 0xFF8A8F98.toInt(), true
            ),
            NexusTheme(
                "ÍNDIGO · violeta",
                0xFF0B0B1A.toInt(), 0xFF12122A.toInt(), 0xFF1A1A38.toInt(),
                0xFF7C6BFF.toInt(), 0xFFE9E7FF.toInt(), 0xFF8B86B8.toInt(), true
            ),
            NexusTheme(
                "ESMERALDA · verde",
                0xFF04140C.toInt(), 0xFF072018.toInt(), 0xFF0B2B20.toInt(),
                0xFF2BD576.toInt(), 0xFFDCFCE7.toInt(), 0xFF6FA98A.toInt(), true
            ),
            NexusTheme(
                "VINHO · carmesim",
                0xFF16070B.toInt(), 0xFF220A11.toInt(), 0xFF2E0D17.toInt(),
                0xFFF2545B.toInt(), 0xFFFBE4E6.toInt(), 0xFFA97C82.toInt(), true
            ),
            NexusTheme(
                "CAFÉ · marrom",
                0xFF120C08.toInt(), 0xFF1C130C.toInt(), 0xFF271B12.toInt(),
                0xFFD9A066.toInt(), 0xFFF3E6D8.toInt(), 0xFF9C8571.toInt(), true
            )
        )

        fun of(index: Int): NexusTheme = ALL[index.coerceIn(0, ALL.size - 1)]
    }
}
