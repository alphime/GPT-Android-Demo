package com.alphi.airobot.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.alibaba.fastjson.JSONObject
import com.alphi.airobot.database.ChatGptApiDBHelper
import com.alphi.airobot.entity.MsgData
import com.alphi.airobot.entity.OpenAiApi
import com.alphi.airobot.utils.AdVanceNestCallback
import com.alphi.airobot.view.tempNewMsgText
import com.unfbx.chatgpt.OpenAiStreamClient
import com.unfbx.chatgpt.entity.chat.BaseChatCompletion
import com.unfbx.chatgpt.entity.chat.BaseMessage
import com.unfbx.chatgpt.entity.chat.ChatCompletion
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse
import com.unfbx.chatgpt.entity.chat.Message
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import java.util.concurrent.TimeUnit


const val defaultOpenApiHost = "https://api.openai.com/"
private val defaultKey: String? = null

//    "sk-GnMR9NrWFYn1kKSeEih9U71ExCfP3PX5LKsn3jQYNPp1vG33"
private var currentApiHost: String? = null
private var currentKey: String? = null
internal lateinit var preferences: SharedPreferences
internal var AiModel: BaseChatCompletion.Model = BaseChatCompletion.Model.GPT_4
internal val mChatContextMessages: MutableList<Message> = ArrayList()

internal lateinit var dbHelper: ChatGptApiDBHelper
internal lateinit var mOpenApiList: MutableList<OpenAiApi>
internal var mSelectApiIndex = 0

class OpenAiModel {
    companion object {
        var client: OpenAiStreamClient? = null
            private set
        private var currentEventSource: EventSource? = null
        fun initProperty(context: Context) {
            AdVanceNestCallback {
                preferences = context.getSharedPreferences("ai-property", Context.MODE_PRIVATE)

                val model = preferences.getString("model", null)
                if (!model.isNullOrBlank()) {
                    try {
                        for (m in BaseChatCompletion.Model.values()) {
                            if (m.getName() == model) {
                                AiModel = m
                                break
                            }
                        }
                        AiModel = BaseChatCompletion.Model.valueOf(model)
                    } catch (e: Exception) {
                        Log.e("initProperty", "AiModel: load properties failure.", e)
                    }
                }

                dbHelper = ChatGptApiDBHelper(context)
                mOpenApiList = dbHelper.queryAll()

                mSelectApiIndex = preferences.getInt("selectApiIndex", 0)
                refreshApiConfig()

                Log.d("TAG", "initProperty: $mOpenApiList")
            }
            if (client == null) {
                refreshClient()
            }
        }

        fun launchAiQuestion(
            text: String,
            list: MutableList<MsgData>,
            closeListener: (MsgData) -> Unit,
            eventListener: (() -> Unit?)? = null
        ) {
            if (client == null)
                refreshClient()

            if (client == null) {
                val mData = MsgData("注意：使用GPT前，需设置API才能使用！", isMe = false)
                list.add(mData)
                closeListener(mData)
                return
            }

            //聊天模型：gpt-3.5
            //聊天模型：gpt-3.5
            val eventSourceListener = object : EventSourceListener() {
                private lateinit var mData: MsgData
                private var isDoneSuccess = false
                private val mChatStrBuilder = StringBuilder()
                private var mAssistantModel: BaseMessage.Role? = null
                override fun onOpen(eventSource: EventSource, response: Response) {
                    currentEventSource = eventSource
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    if (data == "[DONE]") {
                        isDoneSuccess = true
                        mData = MsgData(mChatStrBuilder.toString(), isMe = false)
                        closeListener(mData)
                        mChatContextMessages.add(
                            Message.builder().role(mAssistantModel)
                                .content(mChatStrBuilder.toString())
                                .build()
                        )
                        list.add(mData)
                        tempNewMsgText.value = null
                        Log.d("OpenAiResponse", "onEvent: $mChatStrBuilder")
                        return
                    }
                    val responseData =
                        JSONObject.parseObject(data, ChatCompletionResponse::class.java)
                    if (mAssistantModel == null) {
                        mAssistantModel = BaseMessage.Role.values()
                            .find { responseData.choices[0].delta.role == it.getName() }
                    }
                    val content: String = responseData.choices.joinToString {
                        it.delta.content ?: ""
                    }
                    mChatStrBuilder.append(content)
                    tempNewMsgText.value = mChatStrBuilder.toString()

                    if (eventListener != null) {
                        eventListener()
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    if (!isDoneSuccess) {
                        mData = MsgData(mChatStrBuilder.toString(), isMe = false)
                        closeListener(mData)
                        list.add(mData)
                        tempNewMsgText.value = null
                    }
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    var resultBody: String? = null
                    val content = if (response != null) {
                        if (response.code == 200) {
                            mData = MsgData("$mChatStrBuilder\t\t /End", isMe = false)
                            closeListener(mData)
                            list.add(mData)
                            tempNewMsgText.value = null
                            return      // 200状态码接受正常直接结束异常处理
                        }
                        val strBuilder = StringBuilder("ERR code: ${response.code}  \n")
                        strBuilder.append(errCodeToString(response.code))
                        strBuilder.append("  \n--------------------------  \n")
                        resultBody = response.body?.string()
                        strBuilder.append(resultBody)
                        strBuilder.toString()
                    } else {
                        val testBuilder =
                            Request.Builder().url("https://www.google.cn/generate_204").build()
                        val build =
                            OkHttpClient.Builder().connectTimeout(200, TimeUnit.MILLISECONDS)
                                .build()
                        val responseTest = build.newCall(testBuilder).execute()
                        if (responseTest.code == 204) {
                            "连接超时，API配置存在问题，请检查配置"
                        } else {
                            "存在网络问题，请您检查网络！  \n--------------------------  \nnet::ERR_INTERNET_DISCONNECTED"
                        }
                    }
                    mData = MsgData(content, isMe = false, isErr = true)
                    list.add(mData)
                    closeListener(mData)
                    Log.w("OpenAiResponse", resultBody, t)
                }
            }
            val message: Message =
                Message.builder().role(BaseMessage.Role.USER).content(text).build()
            mChatContextMessages.add(message)
            val chatCompletion =
                ChatCompletion.builder().messages(mChatContextMessages).model(AiModel.getName())
                    .build()
            client?.streamChatCompletion(chatCompletion, eventSourceListener)
        }


        fun interruptAiResponse() {
            currentEventSource?.cancel()
        }


        fun refreshClient() {
            val key = if (!currentKey.isNullOrBlank()) currentKey else defaultKey
            val apiHost =
                if (!currentApiHost.isNullOrBlank()) currentApiHost else defaultOpenApiHost
            if (key == null || apiHost == null) {
                client = null
                return
            }
            client = OpenAiStreamClient.builder()
                .okHttpClient(OkHttpClient.Builder().apply {
                    connectTimeout(800, TimeUnit.MILLISECONDS)
                }.build())
                .apiKey(
                    listOf(key)
                ) //自定义key的获取策略：默认KeyRandomStrategy
                //.keyStrategy(new KeyRandomStrategy())
                // 自己做了代理就传代理地址，没有可不不传
                .apiHost(apiHost)
                .build()
        }

        fun closeOpenDBHelper() {
            dbHelper.close()
        }
    }
}




fun refreshApiConfig() {
    if (mOpenApiList.size > 0) {
        val openAiApi = mOpenApiList[mSelectApiIndex.coerceAtMost(mOpenApiList.size - 1)]
        currentKey = openAiApi.sk
        currentApiHost = openAiApi.host
    } else {
        currentKey = null
        currentApiHost = null
    }
}


fun errCodeToString(code: Int): String {
    return when (code) {
        500 -> {
            "系统繁忙，服务器在处理您的请求时出错"
        }

        403 -> {
            "限制只能使用GPT-3.5-turbo的相关版本"
        }

        429 -> {
            "达到请求的速率限制/您超出了当前配额，请检查您的计划和帐单详细信息/发动机当前过载，请稍后重试"
        }

        401 -> {
            "身份验证无效/提供的 API 密钥不正确/您必须是组织的成员才能使用 API"
        }

        501 -> {
            "参数异常"
        }

        502 -> {
            "请求异常，请重试~"
        }

        else -> {
            "未知错误"
        }
    }
}
