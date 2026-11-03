package com.example.pdf

enum class PdfPageSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 × 297 mm)", 595, 842),
    LETTER("US Letter (8.5 × 11 in)", 612, 792);

    companion object {
        fun fromName(name: String): PdfPageSize {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: A4
        }
    }
}

data class WatermarkConfig(
    val text: String,
    val opacity: Float = 0.35f,
    val angle: Float = -45f,
    val textSizeSp: Float = 42f
)
