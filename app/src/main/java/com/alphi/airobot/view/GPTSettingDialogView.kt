package com.alphi.airobot.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alphi.airobot.entity.MsgData
import com.alphi.airobot.entity.OpenAiApi
import com.alphi.airobot.model.AiModel
import com.alphi.airobot.model.OpenAiModel
import com.alphi.airobot.model.dbHelper
import com.alphi.airobot.model.defaultOpenApiHost
import com.alphi.airobot.model.mChatContextMessages
import com.alphi.airobot.model.mOpenApiList
import com.alphi.airobot.model.mSelectApiIndex
import com.alphi.airobot.model.preferences
import com.alphi.airobot.model.refreshApiConfig
import com.unfbx.chatgpt.entity.chat.BaseChatCompletion


/**
 * 模型设置
 */
@Composable
fun OpenModelSettingDialog(
    dismiss: () -> Unit,
    model: MutableState<BaseChatCompletion.Model>,
    msgDataList: MutableList<MsgData>
) {

    AlertDialog(onDismissRequest = { dismiss() },
        title = {
            Text(text = "GPT Model")
        },
        confirmButton = {},
        text = {
            LazyColumn {
                items(BaseChatCompletion.Model.values()) {
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
    fun dismiss() {
        dialogState.value = false
    }

    val updateIndex = remember {
        mutableStateOf<Int?>(null)
    }

    if (dialogState.value) {

        var rememberAPiSelectIndex by remember { mutableIntStateOf(mSelectApiIndex) }

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
                TextButton(onClick = { updateIndex.value = -1 }) {
                    Text(text = "添加")
                }
            },
            title = { Text(text = "设置") },
            // 内容
            text = {
                Column {
                    LazyColumn {
                        itemsIndexed(mOpenApiList) { index, data ->
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
                                            updateIndex.value = index
                                        },
                                        onDoubleClick = null
                                    )
                            ) {
                                Row {
                                    RadioButton(
                                        selected = index == rememberAPiSelectIndex,
                                        onClick = null,
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
                    }
                    AboutAuthorText()
                }
            }
        )


    }

    ModifyApiDialog(updateIndex = updateIndex) {
        dialogState.value = true
    }
}


@Composable
fun ModifyApiDialog(
    updateIndex: MutableState<Int?>,
    dismissListener: (() -> Unit)?
) {
    if (updateIndex.value != null) {
        val aiProperties = if (updateIndex.value != -1) mOpenApiList[updateIndex.value!!] else null
        var showApiDeleteDialog by remember {
            mutableStateOf(false)
        }
        var nameTempValue by remember {
            mutableStateOf(
                aiProperties?.name ?: ""
            )
        }
        var hostTempValue by remember {
            mutableStateOf(
                aiProperties?.host ?: defaultOpenApiHost
            )
        }
        var skTempValue by remember { mutableStateOf(aiProperties?.sk ?: "") }
        AlertDialog(
            onDismissRequest = { updateIndex.value = null; dismissListener?.invoke() },
            title = { Text(text = "API 设置") },
            text = {
                Column {
                    TextField(value = nameTempValue, label = {
                        Text(text = "名称")
                    }, onValueChange = {
                        nameTempValue = it.trim()
                    }, singleLine = true)
                    TextField(value = hostTempValue, label = {
                        Text(text = "Open Host")
                    }, onValueChange = {
                        hostTempValue = it.trim()
                    }, singleLine = true)
                    TextField(value = skTempValue, label = {
                        Text(text = "Api Key")
                    }, onValueChange = {
                        skTempValue = it.trim()
                    }, singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!hostTempValue.endsWith('/'))
                            hostTempValue += '/'
                        val data = OpenAiApi(
                            name = nameTempValue,
                            host = hostTempValue,
                            sk = skTempValue
                        )
                        if (aiProperties != null) {
                            data.id = aiProperties.id
                            dbHelper.update(aiProperties)
                            mOpenApiList[updateIndex.value!!] = data
                        } else {
                            if (dbHelper.insert(data)) {
                                mOpenApiList.add(data)
                            }
                        }
                        dismissListener?.invoke()
                        updateIndex.value = null
                    },
                    enabled = (hostTempValue.startsWith("http:") || hostTempValue.startsWith("https:"))
                            && skTempValue.isNotBlank() && nameTempValue.isNotBlank()
                ) {
                    Text(text = if (aiProperties != null) "更新" else "添加")
                }
            },
            dismissButton = {
                Box(modifier = Modifier.fillMaxWidth(0.75F)) {
                    if (aiProperties != null) {
                        TextButton(
                            onClick = { showApiDeleteDialog = true },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Text(text = "删除")
                        }
                    }
                    TextButton(
                        onClick = { updateIndex.value = null; dismissListener?.invoke() },
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
                        if (aiProperties != null) {
                            dbHelper.deleteOne(aiProperties)
                            mOpenApiList.remove(aiProperties)
                            if (mOpenApiList.size - 1 < mSelectApiIndex) {
                                mSelectApiIndex = 0
                            }
                        }
                        showApiDeleteDialog = false
                        updateIndex.value = null
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