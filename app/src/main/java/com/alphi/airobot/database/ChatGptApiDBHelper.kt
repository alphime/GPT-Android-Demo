package com.alphi.airobot.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteAbortException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.alphi.airobot.database.ChatGptContract.FeedEntry
import com.alphi.airobot.entity.OpenAiApi
import com.alphi.airobot.utils.EncryptionUtil


class ChatGptApiDBHelper(
    context: Context?
) : SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        val sql = "create table ${TABLE_NAME}(" +
                "${FeedEntry.COLUMN_ID} integer PRIMARY KEY AUTOINCREMENT," +
                "${FeedEntry.COLUMN_NAME} text," +
                "${FeedEntry.COLUMN_HOST} text," +
                "${FeedEntry.COLUMN_SK} text)"
        db?.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val sql = "DROP TABLE IF EXISTS $TABLE_NAME"
        db?.execSQL(sql)
        onCreate(db)
    }

    companion object {
        internal const val DATABASE_NAME = "ChatGptApiReader.db"
        internal const val VERSION = 1
        internal const val TABLE_NAME = "t_apis"
        private const val SECRET_KRY = "83hufy77ysde6?=nkxhua8"
    }

    fun insert(data: OpenAiApi) : Boolean{
        writableDatabase.use {
            val id = it.insert(TABLE_NAME, null, ContentValues().apply {
                put(FeedEntry.COLUMN_NAME, data.name)
                put(FeedEntry.COLUMN_HOST, data.host)
                put(FeedEntry.COLUMN_SK, EncryptionUtil.encrypt(data.sk, SECRET_KRY))
            })
            if (id == -1L) {
                Log.d(this.javaClass.simpleName, "insert: 插入失败")
                return false
            } else {
                data.id = id.toInt()
                return true
            }
        }
    }

    fun update(data: OpenAiApi) {
        assertId(data, "update")
        writableDatabase.use {
            it.update(TABLE_NAME, ContentValues().apply {
                put(FeedEntry.COLUMN_NAME, data.name)
                put(FeedEntry.COLUMN_HOST, data.host)
                put(FeedEntry.COLUMN_SK, EncryptionUtil.encrypt(data.sk, SECRET_KRY))
            }, "${FeedEntry.COLUMN_ID}=?", arrayOf("${data.id}"))
        }
    }

    fun deleteOne(data: OpenAiApi) {
        assertId(data, "deleteOne")
        writableDatabase.use {
            it.delete(TABLE_NAME, "${FeedEntry.COLUMN_ID}=?", arrayOf("${data.id}"))
        }
    }

    fun queryAll(): ArrayList<OpenAiApi> {
        val list = ArrayList<OpenAiApi>()
        readableDatabase.query(TABLE_NAME, null, null, null, null, null, null)
            .use {
                with(it) {
                    while (moveToNext()) {
                        val id = getInt(getColumnIndexOrThrow(FeedEntry.COLUMN_ID))
                        val name = getString(getColumnIndexOrThrow(FeedEntry.COLUMN_NAME))
                        val sk = getString(getColumnIndexOrThrow(FeedEntry.COLUMN_SK))
                        val url = getString(getColumnIndexOrThrow(FeedEntry.COLUMN_HOST))
                        list.add(OpenAiApi(id, name, url, EncryptionUtil.decrypt(sk, SECRET_KRY)))
                    }
                }
            }
        return list
    }

    private fun assertId(data: OpenAiApi, method: String) {
        if (data.id == null) {
            throw SQLiteAbortException("<OpenAiApiBean.${FeedEntry.COLUMN_ID}> can't be null when running the $method method!")
        }
    }
}