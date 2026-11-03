package com.example.data.repository

import com.example.data.dao.FolderDao
import com.example.data.entity.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FolderRepository(private val folderDao: FolderDao) {
    fun getAllFolders(): Flow<List<Folder>> = folderDao.getAllFolders()

    suspend fun createFolder(name: String): Long = withContext(Dispatchers.IO) {
        val folder = Folder(name = name)
        folderDao.insertFolder(folder)
    }

    suspend fun renameFolder(id: Long, name: String) = withContext(Dispatchers.IO) {
        val folder = folderDao.getFolderById(id)
        if (folder != null) {
            folderDao.updateFolder(folder.copy(name = name))
        }
    }

    suspend fun deleteFolder(id: Long) = withContext(Dispatchers.IO) {
        val folder = folderDao.getFolderById(id)
        if (folder != null) {
            folderDao.deleteFolder(folder)
        }
    }
}
