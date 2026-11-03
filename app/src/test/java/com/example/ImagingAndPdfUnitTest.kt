package com.example

import com.example.data.entity.Document
import com.example.data.entity.Folder
import com.example.data.entity.Page
import com.example.imaging.CornerPoints
import com.example.imaging.FilterType
import com.example.imaging.Point2D
import com.example.pdf.PdfPageSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagingAndPdfUnitTest {

    @Test
    fun testCornerPoints_serializationAndParsing() {
        val original = CornerPoints(
            topLeft = Point2D(10f, 20f),
            topRight = Point2D(500f, 20f),
            bottomRight = Point2D(500f, 800f),
            bottomLeft = Point2D(10f, 800f)
        )

        val json = original.toJson()
        val parsed = CornerPoints.fromJson(json)

        assertNotNull(parsed)
        assertEquals(10f, parsed!!.topLeft.x, 0.01f)
        assertEquals(20f, parsed.topLeft.y, 0.01f)
        assertEquals(500f, parsed.topRight.x, 0.01f)
        assertEquals(800f, parsed.bottomRight.y, 0.01f)
    }

    @Test
    fun testCornerPoints_defaultForDimensions() {
        val corners = CornerPoints.defaultForDimensions(1000f, 2000f, 0.1f)
        assertEquals(100f, corners.topLeft.x, 0.01f)
        assertEquals(200f, corners.topLeft.y, 0.01f)
        assertEquals(900f, corners.bottomRight.x, 0.01f)
        assertEquals(1800f, corners.bottomRight.y, 0.01f)
    }

    @Test
    fun testFilterType_resolution() {
        assertEquals(FilterType.ORIGINAL, FilterType.fromName("ORIGINAL"))
        assertEquals(FilterType.AUTO, FilterType.fromName("AUTO"))
        assertEquals(FilterType.MAGIC_COLOR, FilterType.fromName("MAGIC_COLOR"))
        assertEquals(FilterType.BW, FilterType.fromName("BW"))
        assertEquals(FilterType.GRAYSCALE, FilterType.fromName("GRAYSCALE"))
        assertEquals(FilterType.AUTO, FilterType.fromName("UNKNOWN_VALUE"))
    }

    @Test
    fun testPdfPageSize_dimensions() {
        assertEquals(595, PdfPageSize.A4.widthPt)
        assertEquals(842, PdfPageSize.A4.heightPt)
        assertEquals(612, PdfPageSize.LETTER.widthPt)
        assertEquals(792, PdfPageSize.LETTER.heightPt)
    }

    @Test
    fun testDocumentAndPage_modelIntegrity() {
        val folder = Folder(id = 1L, name = "Receipts")
        val doc = Document(
            id = 10L,
            title = "Tax Receipt 2026",
            folderId = folder.id,
            pageCount = 2,
            isLocked = false
        )
        val page = Page(
            id = 100L,
            documentId = doc.id,
            orderIndex = 0,
            originalPath = "/data/documents/10/orig_0.jpg",
            processedPath = "/data/documents/10/proc_0.jpg",
            filter = "MAGIC_COLOR",
            rotation = 0
        )

        assertEquals("Tax Receipt 2026", doc.title)
        assertEquals(1L, doc.folderId)
        assertEquals(10L, page.documentId)
        assertEquals(0, page.orderIndex)
        assertEquals("MAGIC_COLOR", page.filter)
    }
}
