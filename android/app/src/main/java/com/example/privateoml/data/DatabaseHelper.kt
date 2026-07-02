package com.example.privateoml.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "private_oml.db"
        private const val DATABASE_VERSION = 2

        // Table local_config
        const val TABLE_CONFIG = "local_config"
        const val COL_CONFIG_KEY = "config_key"
        const val COL_CONFIG_VAL = "config_value"

        // Table local_logs
        const val TABLE_LOGS = "local_logs"
        const val COL_LOG_ID = "id"
        const val COL_LOG_TS = "timestamp"
        const val COL_LOG_MOD = "module"
        const val COL_LOG_SEV = "severity"
        const val COL_LOG_MSG = "message"

        // Table gallery_cache
        const val TABLE_GALLERY = "gallery_cache"
        const val COL_IMG_ID = "image_id"
        const val COL_IMG_NAME = "file_name"
        const val COL_IMG_HASH = "file_hash"
        const val COL_IMG_THUMB = "thumbnail_id"
        const val COL_IMG_SIZE = "size_bytes"
        const val COL_IMG_CREATED = "created_time"
        const val COL_IMG_MODIFIED = "modified_time"
        const val COL_IMG_FAV = "is_favorite"
        const val COL_IMG_HIDDEN = "is_hidden"
        const val COL_IMG_SYNC = "sync_status" // 'pending', 'synced', 'failed'

        // Table message_drafts
        const val TABLE_DRAFTS = "message_drafts"
        const val COL_DRAFT_ID = "id"
        const val COL_DRAFT_TITLE = "title"
        const val COL_DRAFT_CONTENT = "content"
        const val COL_DRAFT_CREATED = "created_at"
        const val COL_DRAFT_UPDATED = "updated_at"
        const val COL_DRAFT_IS_VERSION = "is_version"
        const val COL_DRAFT_VERSION_NUM = "version_number"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Create config table
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_CONFIG (" +
                    "$COL_CONFIG_KEY TEXT PRIMARY KEY, " +
                    "$COL_CONFIG_VAL TEXT NOT NULL)"
        )

        // 2. Create logs table
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_LOGS (" +
                    "$COL_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COL_LOG_TS DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "$COL_LOG_MOD TEXT, " +
                    "$COL_LOG_SEV TEXT, " +
                    "$COL_LOG_MSG TEXT)"
        )

        // 3. Create gallery cache table
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_GALLERY (" +
                    "$COL_IMG_ID TEXT PRIMARY KEY, " +
                    "$COL_IMG_NAME TEXT NOT NULL, " +
                    "$COL_IMG_HASH TEXT NOT NULL, " +
                    "$COL_IMG_THUMB TEXT, " +
                    "$COL_IMG_SIZE INTEGER, " +
                    "$COL_IMG_CREATED TEXT, " +
                    "$COL_IMG_MODIFIED TEXT, " +
                    "$COL_IMG_FAV INTEGER DEFAULT 0, " +
                    "$COL_IMG_HIDDEN INTEGER DEFAULT 0, " +
                    "$COL_IMG_SYNC TEXT DEFAULT 'pending')"
        )

        // 4. Create drafts table
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE_DRAFTS (" +
                    "$COL_DRAFT_ID TEXT PRIMARY KEY, " +
                    "$COL_DRAFT_TITLE TEXT NOT NULL, " +
                    "$COL_DRAFT_CONTENT TEXT NOT NULL, " +
                    "$COL_DRAFT_CREATED TEXT NOT NULL, " +
                    "$COL_DRAFT_UPDATED TEXT NOT NULL, " +
                    "$COL_DRAFT_IS_VERSION INTEGER DEFAULT 0, " +
                    "$COL_DRAFT_VERSION_NUM INTEGER DEFAULT 1)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONFIG")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GALLERY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DRAFTS")
        onCreate(db)
    }

    // Config setters/getters
    fun saveConfig(key: String, value: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CONFIG_KEY, key)
            put(COL_CONFIG_VAL, value)
        }
        db.insertWithOnConflict(TABLE_CONFIG, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getConfig(key: String, defaultValue: String): String {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CONFIG,
            arrayOf(COL_CONFIG_VAL),
            "$COL_CONFIG_KEY = ?",
            arrayOf(key),
            null, null, null
        )
        var value = defaultValue
        if (cursor.moveToFirst()) {
            value = cursor.getString(0)
        }
        cursor.close()
        return value
    }

    // Gallery Cache CRUD operations
    data class LocalImage(
        val id: String,
        val name: String,
        val hash: String,
        val thumbnailId: String?,
        val sizeBytes: Long,
        val createdTime: String,
        val modifiedTime: String,
        val isFavorite: Boolean,
        val isHidden: Boolean,
        val syncStatus: String
    )

    fun getGalleryImages(): List<LocalImage> {
        val list = mutableListOf<LocalImage>()
        val db = readableDatabase
        val cursor = db.query(TABLE_GALLERY, null, null, null, null, null, "$COL_IMG_CREATED DESC")
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    LocalImage(
                        id = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMG_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMG_NAME)),
                        hash = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMG_HASH)),
                        thumbnailId = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMG_THUMB)),
                        sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(COL_IMG_SIZE)),
                        createdTime = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMG_CREATED)),
                        modifiedTime = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMG_MODIFIED)),
                        isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IMG_FAV)) == 1,
                        isHidden = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IMG_HIDDEN)) == 1,
                        syncStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMG_SYNC))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun saveGalleryImage(image: LocalImage) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IMG_ID, image.id)
            put(COL_IMG_NAME, image.name)
            put(COL_IMG_HASH, image.hash)
            put(COL_IMG_THUMB, image.thumbnailId)
            put(COL_IMG_SIZE, image.sizeBytes)
            put(COL_IMG_CREATED, image.createdTime)
            put(COL_IMG_MODIFIED, image.modifiedTime)
            put(COL_IMG_FAV, if (image.isFavorite) 1 else 0)
            put(COL_IMG_HIDDEN, if (image.isHidden) 1 else 0)
            put(COL_IMG_SYNC, image.syncStatus)
        }
        db.insertWithOnConflict(TABLE_GALLERY, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateImageSyncStatus(id: String, status: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IMG_SYNC, status)
        }
        db.update(TABLE_GALLERY, values, "$COL_IMG_ID = ?", arrayOf(id))
    }

    fun updateImageVisibility(id: String, isHidden: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IMG_HIDDEN, if (isHidden) 1 else 0)
        }
        db.update(TABLE_GALLERY, values, "$COL_IMG_ID = ?", arrayOf(id))
    }

    fun updateImageFavorite(id: String, isFav: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IMG_FAV, if (isFav) 1 else 0)
        }
        db.update(TABLE_GALLERY, values, "$COL_IMG_ID = ?", arrayOf(id))
    }

    fun deleteGalleryImage(id: String) {
        val db = writableDatabase
        db.delete(TABLE_GALLERY, "$COL_IMG_ID = ?", arrayOf(id))
    }

    // Message drafts and version history operations
    data class MessageDraft(
        val id: String,
        val title: String,
        val content: String,
        val createdAt: String,
        val updatedAt: String,
        val isVersion: Boolean,
        val versionNumber: Int
    )

    fun getMessageDrafts(isVersion: Boolean): List<MessageDraft> {
        val list = mutableListOf<MessageDraft>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DRAFTS, null, "$COL_DRAFT_IS_VERSION = ?",
            arrayOf(if (isVersion) "1" else "0"),
            null, null, "$COL_DRAFT_UPDATED DESC"
        )
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    MessageDraft(
                        id = cursor.getString(cursor.getColumnIndexOrThrow(COL_DRAFT_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COL_DRAFT_TITLE)),
                        content = cursor.getString(cursor.getColumnIndexOrThrow(COL_DRAFT_CONTENT)),
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_DRAFT_CREATED)),
                        updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_DRAFT_UPDATED)),
                        isVersion = cursor.getInt(cursor.getColumnIndexOrThrow(COL_DRAFT_IS_VERSION)) == 1,
                        versionNumber = cursor.getInt(cursor.getColumnIndexOrThrow(COL_DRAFT_VERSION_NUM))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun saveMessageDraft(draft: MessageDraft) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_DRAFT_ID, draft.id)
            put(COL_DRAFT_TITLE, draft.title)
            put(COL_DRAFT_CONTENT, draft.content)
            put(COL_DRAFT_CREATED, draft.createdAt)
            put(COL_DRAFT_UPDATED, draft.updatedAt)
            put(COL_DRAFT_IS_VERSION, if (draft.isVersion) 1 else 0)
            put(COL_DRAFT_VERSION_NUM, draft.versionNumber)
        }
        db.insertWithOnConflict(TABLE_DRAFTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteMessageDraft(id: String) {
        val db = writableDatabase
        db.delete(TABLE_DRAFTS, "$COL_DRAFT_ID = ?", arrayOf(id))
    }
}
