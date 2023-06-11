package com.alphi.airobot.view

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.alphi.airobot.entity.MsgData
import com.alphi.airobot.model.OpenAiModel
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class
)
@Composable
fun InitInputView(list: MutableList<MsgData>) {
    var mSendButtonEnable by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var text by remember { mutableStateOf("") }
    // 发送事件
    val onSendClick = fun() {
        if (text.isBlank())
            return
        scope.launch {
            mSendButtonEnable = false
            text = ""
        }
        val data = MsgData(text, isMe = true, selectable = true)
        list.add(data)
        OpenAiModel.launchAiQuestion(text, list, closeListener = {
            mSendButtonEnable = true
            it.selectable = true
        }, eventListener = {
            scrollBottomEvent(scope, needOnBottom = true)
        })
        keyboardController?.hide()
    }

    Row(Modifier.padding(6.dp)) {
        TextField(
            value = text,
            onValueChange = { text = it },
            Modifier
                .padding(6.dp)
                .weight(1f),
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            placeholder = {
                Text(text = "有什么问题尽管问我...")
            },

            trailingIcon = {
                Row {
                    IconButton(     //clear
                        onClick = {
                            text = ""
                        }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除文字")
                    }
                    IconButton(     // send
                        onClick = onSendClick,
                        Modifier
                            .wrapContentWidth()
                            .height(50.dp)
                            .align(Alignment.CenterVertically),
                        enabled = mSendButtonEnable
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "发送")
                    }
                }
            }
        )
    }

}