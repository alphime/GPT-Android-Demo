package com.alphi.airobot.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.alphi.airobot.entity.MsgData
import com.alphi.airobot.model.OpenAiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking


@Composable
fun InitInputView(list: MutableList<MsgData>, modifier: Modifier = Modifier) {
    var mSendButtonEnable by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var inputText by remember { mutableStateOf("") }
    fun launchAiQuestion(text: String?) {
        OpenAiModel.launchAiQuestion(text, closeListener = {
            it.selectable = true
            if (it.isErr) {
                setTempNewMsgText(it.text, enableCloseDownIcon = false)
                runBlocking {
                    delay(3000)     // 重发时间 ms
                    if (!isEmptyTempNewMsgText()) {
                        launchAiQuestion(null)
                    }
                }
            } else {
                mSendButtonEnable = true
                list.add(it)
                setTempNewMsgText(null)
            }
        }, eventListener = {
            scrollBottomEvent(scope, needOnBottom = true)
            setTempNewMsgText(it)
        })
    }
    // 发送事件
    val onSendClick = fun() {
        if (inputText.isBlank())
            return
        mSendButtonEnable = false
        val data = MsgData(inputText, isMe = true, selectable = true)
        list.add(data)
        launchAiQuestion(inputText)
        inputText = ""
        keyboardController?.hide()
        scrollBottomEvent(scope)
    }

    Row(
        Modifier
            .padding(6.dp)
            .then(modifier)
    ) {
        TextField(
            value = inputText,
            onValueChange = { inputText = it },
            Modifier
                .padding(6.dp)
                .weight(1f),
//            interactionSource = remember { MutableInteractionSource() }
//                .also { interactionSource ->
//                    LaunchedEffect(interactionSource) {
//                        interactionSource.interactions.collect {
//                            if (it is PressInteraction.Release) {
//                                // works like onClick
//                                if (text.isEmpty()) {
//                                    delay(100)
//                                    scrollBottomEvent()
//                                }
//                            }
//                        }
//                    }
//                },
            shape = RoundedCornerShape(size = 80F),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            placeholder = {
                Text(text = "有什么问题尽管问我...")
            },
            maxLines = 5,
            trailingIcon = {
                Row {
                    if (inputText.isNotEmpty()) {
                        IconButton(     //clear
                            onClick = {
                                inputText = ""
                            }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除文字")
                        }
                    }
                    IconButton(     // send
                        onClick = onSendClick,
                        Modifier
                            .wrapContentWidth()
                            .height(50.dp)
                            .align(Alignment.CenterVertically),
                        enabled = mSendButtonEnable
                    ) {
                        Icon(Icons.AutoMirrored.Default.Send, contentDescription = "发送")
                    }
                }
            }
        )
    }

}