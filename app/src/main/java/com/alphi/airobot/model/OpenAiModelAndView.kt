package com.alphi.airobot.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibaba.fastjson.JSONObject
import com.alphi.airobot.database.ChatGptApiDBHelper
import com.alphi.airobot.entity.MsgData
import com.alphi.airobot.entity.OpenAiApi
import com.alphi.airobot.utils.AdVanceNestCallback
import com.alphi.airobot.view.AboutAuthorText
import com.alphi.airobot.view.tempNewMsgText
import com.unfbx.chatgpt.OpenAiStreamClient
import com.unfbx.chatgpt.entity.chat.ChatCompletion
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse
import com.unfbx.chatgpt.entity.chat.Message
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import java.util.concurrent.TimeUnit


private const val defalutOpenApiHost = "https://api.chatanywhere.com.cn/"
private val defaultKey: String? = null

//    "sk-GnMR9NrWFYn1kKSeEih9U71ExCfP3PX5LKsn3jQYNPp1vG33"
private var currentApiHost: String? = null
private var currentKey: String? = null
private lateinit var preferences: SharedPreferences
internal lateinit var AiModel: ChatCompletion.Model
private val key = "83hufy77ysde6?=nkxcvt2"
private val mChatContextMessages: MutableList<Message> = ArrayList()

private lateinit var dbHelper: ChatGptApiDBHelper
private lateinit var mOpenApiList: MutableList<OpenAiApi>
private var mSelectApiIndex = 0

class OpenAiModel {
    companion object {
        var client: OpenAiStreamClient? = null
            private set
        private var currentEventSource: EventSource? = null
        fun initProperty(context: Context) {
            AdVanceNestCallback {
                preferences = context.getSharedPreferences("ai-property", Context.MODE_PRIVATE)

                val model = preferences.getString("model", null)
                AiModel = if (model == null) ChatCompletion.Model.GPT_3_5_TURBO_0301
                else ChatCompletion.Model.values().find { it.getName() == model }
                    ?: ChatCompletion.Model.GPT_3_5_TURBO_0301

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
                private var mAssistantModel: Message.Role? = null
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
                        mAssistantModel = Message.Role.values()
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
                        val testBuilder = Request.Builder().url("https://www.google.cn/generate_204").build()
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
                Message.builder().role(Message.Role.USER).content(text).build()
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
            val apiHost = if (!currentApiHost.isNullOrBlank()) currentApiHost else defalutOpenApiHost
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


/**
 * 普通设置
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OpenSettingsDialog(dialogState: MutableState<Boolean>) {
//    var apiValue by remember {
//        mutableStateOf(defalutOpenApiHost)
//    }
//    var keyValue by remember {
//        mutableStateOf(currentKey)
//    }
    fun dismiss() { dialogState.value = false }

    val showApiAddModifyDialogState = remember {
        mutableStateOf(false)
    }

    var rememberTempModifyData: OpenAiApi? by remember { mutableStateOf(null) }

    if (dialogState.value) {
        fun showApiAddModifyDialog() {
            showApiAddModifyDialogState.value = true
            dismiss()
        }


        var rememberAPiSelectIndex by remember { mutableStateOf(mSelectApiIndex) }

        val confirmListener = fun() {
            if (mOpenApiList.size > 0) {
                refreshApiConfig()
                OpenAiModel.refreshClient()
            }
            dismiss()
        }
        AlertDialog(
            onDismissRequest = confirmListener,
            confirmButton = {
                TextButton(onClick = confirmListener) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiAddModifyDialog() }) {
                    Text(text = "添加")
                }
            },
            title = { Text(text = "设置") },
            // 内容
            text = {
                Column {
                    mOpenApiList.forEachIndexed { index, data ->
                        var spendAvailable by remember {
                            mutableStateOf("查询中")
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .combinedClickable(
                                    onClick = {
                                        // 选择事件
                                        mSelectApiIndex = index
                                        rememberAPiSelectIndex = index
                                        refreshApiConfig()
                                    },
                                    onLongClick = {
                                        // 修改事件
                                        rememberTempModifyData = data
                                        showApiAddModifyDialog()
                                    },
                                    onDoubleClick = null
                                )
                        ) {
                            Row {
                                RadioButton(
                                    selected = index == rememberAPiSelectIndex, onClick = null,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                Text(
                                    text = data.name,
                                    fontSize = 18.sp,
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(horizontal = 6.dp)
                                )
                            }
                            Text(text = spendAvailable, Modifier.align(Alignment.CenterEnd))
                        }

                        // 修复请求反应太快了，Compose界面还没加载完成出现的异常
                        LaunchedEffect(Unit) {
                            Thread {
                                spendAvailable = try {
                                    "￥ ${OpenAiModel.client?.subscription()?.hardLimitUsd}"
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    "查询失败"
                                }
                            }.start()
                        }
                    }
    //                TextField(value = apiValue, label = {
    //                    Text(text = "Open Host")
    //                }, onValueChange = {
    //                    apiValue = it.ifBlank { defalutOpenApiHost }
    //                }, singleLine = true)
    //                TextField(value = keyValue, label = {
    //                    Text(text = "Api Key")
    //                }, onValueChange = {
    //                    keyValue = it
    //                }, singleLine = true
    //                )
                    AboutAuthorText()
                }
            }
        )


    }

    ModifyApiDialog(showApiModifyDialog = showApiAddModifyDialogState, rememberTempModifyData) {
        rememberTempModifyData = null
        dialogState.value = true
    }
}


@Composable
fun ModifyApiDialog(
    showApiModifyDialog: MutableState<Boolean>,
    apiData: OpenAiApi? = null,
    dismissListener: (() -> Unit)?
) {
    if (showApiModifyDialog.value) {
        var showApiDeleteDialog by remember {
            mutableStateOf(false)
        }
        var nameTempValue by remember {
            mutableStateOf(
                apiData?.name ?: ""
            )
        }
        var hostTempValue by remember {
            mutableStateOf(
                apiData?.host ?: ""
            )
        }
        var skTempValue by remember { mutableStateOf(apiData?.sk ?: "") }
        AlertDialog(
            onDismissRequest = { showApiModifyDialog.value = false; dismissListener?.invoke() },
            title = { Text(text = "API 设置") },
            text = {
                Column {
                    TextField(value = nameTempValue, label = {
                        Text(text = "名称")
                    }, onValueChange = {
                        nameTempValue = it
                    }, singleLine = true)
                    TextField(value = hostTempValue, label = {
                        Text(text = "Open Host")
                    }, onValueChange = {
                        hostTempValue = it
                    }, singleLine = true)
                    TextField(value = skTempValue, label = {
                        Text(text = "Api Key")
                    }, onValueChange = {
                        skTempValue = it
                    }, singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!hostTempValue.endsWith('/'))
                            hostTempValue += '/'
                        if (apiData != null) {
                            apiData.name = nameTempValue
                            apiData.host = hostTempValue
                            apiData.sk = skTempValue
                            dbHelper.update(apiData)
                        } else {
                            val data = OpenAiApi(
                                name = nameTempValue,
                                host = hostTempValue,
                                sk = skTempValue
                            )
                            if (dbHelper.insert(data)) {
                                mOpenApiList.add(data)
                            }
                        }
                        dismissListener?.invoke()
                        showApiModifyDialog.value = false
                    },
                    enabled = (hostTempValue.startsWith("http:") || hostTempValue.startsWith("https:"))
                            && skTempValue.isNotBlank() && nameTempValue.isNotBlank()
                ) {
                    Text(text = if (apiData != null) "更新" else "添加")
                }
            },
            dismissButton = {
                Box(modifier = Modifier.fillMaxWidth(0.75F)) {
                    if (apiData != null) {
                        TextButton(
                            onClick = { showApiDeleteDialog = true },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Text(text = "删除")
                        }
                    }
                    TextButton(
                        onClick = { showApiModifyDialog.value = false; dismissListener?.invoke() },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(text = "取消")
                    }
                }
            }
        )

        if (showApiDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showApiDeleteDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        if (apiData != null) {
                            dbHelper.deleteOne(apiData)
                            mOpenApiList.remove(apiData)
                            if (mOpenApiList.size - 1 < mSelectApiIndex) {
                                mSelectApiIndex = 0
                            }
                        }
                        showApiDeleteDialog = false
                        showApiModifyDialog.value = false
                        dismissListener?.invoke()
                    }) {
                        Text(text = "确定")
                    }
                },
                text = {
                    Text(text = "确定删除？删除后无法恢复")
                },
                dismissButton = {
                    TextButton(onClick = { showApiDeleteDialog = false }) {
                        Text(text = "取消")
                    }
                }
            )
        }
    }
}


/**
 * 模型设置
 */
@Composable
fun OpenModelSettingDialog(
    dismiss: () -> Unit,
    model: MutableState<ChatCompletion.Model>,
    msgDataList: MutableList<MsgData>
) {

    AlertDialog(onDismissRequest = { dismiss() },
        title = {
            Text(text = "GPT Model")
        },
        confirmButton = {},
        text = {
            Column {
                ChatCompletion.Model.values().forEach {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (it == model.value),
                                onClick = {
                                    if (AiModel != it) {
                                        model.value = it
                                        AiModel = model.value
                                        val editor = preferences.edit()
                                        editor.putString("model", AiModel.getName())
                                        editor.apply()
                                        OpenAiModel.interruptAiResponse()
                                    }
                                    mChatContextMessages.clear()
                                    msgDataList.clear()
                                }
                            )
                            .padding(vertical = 10.dp)
                    ) {
                        RadioButton(
                            selected = (it == model.value),
                            onClick = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Text(
                            text = it.getName().replaceFirst("gpt", "GPT"),
                            fontSize = 20.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        })
}


private fun refreshApiConfig() {
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

