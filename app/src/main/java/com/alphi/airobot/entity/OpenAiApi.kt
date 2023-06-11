package com.alphi.airobot.entity

data class OpenAiApi(
    var id: Int? = null,
    var name: String,
    var host: String,
    var sk: String
) {
    override fun toString(): String {
        return "OpenAiApi(id=$id, name='$name', host='$host', sk='$sk')"
    }
}