package com.example.imaging

data class Point2D(val x: Float, val y: Float)

data class CornerPoints(
    val topLeft: Point2D,
    val topRight: Point2D,
    val bottomRight: Point2D,
    val bottomLeft: Point2D
) {
    fun toFloatArray(): FloatArray {
        return floatArrayOf(
            topLeft.x, topLeft.y,
            topRight.x, topRight.y,
            bottomRight.x, bottomRight.y,
            bottomLeft.x, bottomLeft.y
        )
    }

    fun toJson(): String {
        return "${topLeft.x},${topLeft.y};${topRight.x},${topRight.y};${bottomRight.x},${bottomRight.y};${bottomLeft.x},${bottomLeft.y}"
    }

    companion object {
        fun defaultForDimensions(width: Float, height: Float, insetRatio: Float = 0.08f): CornerPoints {
            val insetX = width * insetRatio
            val insetY = height * insetRatio
            return CornerPoints(
                topLeft = Point2D(insetX, insetY),
                topRight = Point2D(width - insetX, insetY),
                bottomRight = Point2D(width - insetX, height - insetY),
                bottomLeft = Point2D(insetX, height - insetY)
            )
        }

        fun fromJson(json: String?): CornerPoints? {
            if (json.isNullOrBlank()) return null
            return try {
                val parts = json.split(";")
                if (parts.size != 4) return null
                val p0 = parts[0].split(",").map { it.toFloat() }
                val p1 = parts[1].split(",").map { it.toFloat() }
                val p2 = parts[2].split(",").map { it.toFloat() }
                val p3 = parts[3].split(",").map { it.toFloat() }
                CornerPoints(
                    topLeft = Point2D(p0[0], p0[1]),
                    topRight = Point2D(p1[0], p1[1]),
                    bottomRight = Point2D(p2[0], p2[1]),
                    bottomLeft = Point2D(p3[0], p3[1])
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
