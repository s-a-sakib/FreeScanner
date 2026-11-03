package com.example.imaging

enum class FilterType(val displayName: String) {
    ORIGINAL("Original"),
    AUTO("Auto"),
    MAGIC_COLOR("Magic Color"),
    BW("B&W"),
    GRAYSCALE("Grayscale");

    companion object {
        fun fromName(name: String): FilterType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: AUTO
        }
    }
}
