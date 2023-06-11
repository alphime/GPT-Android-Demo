package com.alphi.airobot.entity

data class MsgData(val text: String, val isMe: Boolean = false, var selectable: Boolean = false, val isErr: Boolean = false) {
    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MsgData

        if (text != other.text) return false
        if (isMe != other.isMe) return false

        return true
    }
}