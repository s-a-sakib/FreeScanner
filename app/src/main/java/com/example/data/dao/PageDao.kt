package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Page
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY orderIndex ASC")
    fun getPagesForDocument(documentId: Long): Flow<List<Page>>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY orderIndex ASC")
    suspend fun getPagesListForDocument(documentId: Long): List<Page>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun getPageById(id: Long): Page?

    @Query("SELECT COUNT(*) FROM pages WHERE documentId = :documentId")
    suspend fun getPageCount(documentId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: Page): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<Page>): List<Long>

    @Update
    suspend fun updatePage(page: Page)

    @Update
    suspend fun updatePages(pages: List<Page>)

    @Delete
    suspend fun deletePage(page: Page)

    @Query("DELETE FROM pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: Long)
}
