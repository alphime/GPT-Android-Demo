package com.alphi.airobot.database

import android.provider.BaseColumns

class ChatGptContract {
    object FeedEntry : BaseColumns {
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_HOST = "host"
        const val COLUMN_SK = "sk"
    }
}