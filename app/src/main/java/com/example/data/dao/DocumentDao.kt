package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Document
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentDocuments(limit: Int = 10): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun getDocumentsByFolder(folderId: Long): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): Document?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun getDocumentFlowById(id: Long): Flow<Document?>

    @Query("""
        SELECT DISTINCT d.* FROM documents d 
        LEFT JOIN pages p ON d.id = p.documentId 
        WHERE d.title LIKE '%' || :query || '%' 
        OR (p.ocrText IS NOT NULL AND p.ocrText LIKE '%' || :query || '%')
        ORDER BY d.updatedAt DESC
    """)
    fun searchDocuments(query: String): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document): Long

    @Update
    suspend fun updateDocument(document: Document)

    @Delete
    suspend fun deleteDocument(document: Document)

    @Query("DELETE FROM documents WHERE id IN (:ids)")
    suspend fun deleteDocumentsByIds(ids: List<Long>)
}
