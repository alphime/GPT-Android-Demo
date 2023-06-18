package com.alphi.airobot.view

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alphi.airobot.compose.MarkdownText
import com.alphi.airobot.entity.MsgData
import com.alphi.airobot.model.OpenAiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Timer
import java.util.TimerTask


class ChatBoxView {
    companion object {
        /**
         * 在onCreate()调用，以启用OneClock；<br/>
         * 实现原理采用计时器，在暗黑模式时生命周期会重新onCreate()
         */
        fun createOneClock() {
            mIsCreateClock = true
        }
    }
}

private var mIsCreateClock = false
private lateinit var ChatListState: LazyListState
internal lateinit var tempNewMsgText: MutableState<String?>


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InitChatBoxView(list: MutableList<MsgData>, modifier: Modifier = Modifier) {
    ChatListState = rememberLazyListState()
    tempNewMsgText = remember { mutableStateOf(null) }
    var chatOneClockText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    if (mIsCreateClock) {
        mIsCreateClock = false
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MILLISECOND, 0)
        var date = calendar.time
        chatOneClockText = "现在是北京时间：$date"
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                date = Date()
                scope.launch {
                    chatOneClockText = "现在是北京时间：$date"
                }
            }
        }, date, 1000)
//        list.add(MsgData("![猫咪图片](https://cdn.pixabay.com/photo/2017/02/20/18/03/cat-2083492_1280.jpg)"))
    }

    LazyColumn(state = ChatListState, modifier = modifier) {
        try {
            // 时钟
            item {
                Row(
                    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start
                ) {
                    NewMsgContentView(text = chatOneClockText, maxWidth = 230.dp)
                }
            }
            // 聊天列表     不建议forEach嵌套item，否则滑动会出现问题
            items(list) { v ->
                val minWidth =
                    if (v.text.contains(Regex("\\|\\s-+?\\s\\|")) || isMarkDownImage(
                            v.text,
                            onlyImage = true
                        )
                    ) 200.dp
                    else MsgContentViewDefaultParam.minWidth
                Row(
                    Modifier.fillMaxWidth(), horizontalArrangement = if (v.isMe) Arrangement.End
                    else Arrangement.Start
                ) {
                    if (v.selectable) SelectionContainer(
                        content = {
                            NewMsgContentView(
                                text = v.text,
                                enableMarkDownText = !v.isMe && !v.isErr,
                                textIsSelectable = true,
                                minWidth = minWidth,
                                isMe = v.isMe
                            )
                        },
                    )
                    else DisableSelection(content = {
                        NewMsgContentView(
                            text = v.text,
                            enableMarkDownText = !v.isMe,
                            minWidth = minWidth,
                            isMe = v.isMe
                        )
                    })
                }

            }
            // 新字节流消息
            if (tempNewMsgText.value != null) {
                item {
                    Row(
                        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start
                    ) {
                        var textValue = tempNewMsgText.value
                            ?.replace("|\n", "|\b  \n") ?: ""
                        if (isMarkDownImage(textValue)) {
                            textValue = textValue.replace("](", "]\b(")
                        }
                        NewMsgContentView(
                            text = textValue,
                            marginValues = PaddingValues(
                                start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp
                            ),      // margin
                            enableMarkDownText = true,
                            isMe = false
                        )
                        var visibleCloseIcon by remember { mutableStateOf(false) }
                        if (visibleCloseIcon) {
                            IconButton(
                                onClick = {
                                    OpenAiModel.interruptAiResponse()
                                    visibleCloseIcon = false
                                },
                                Modifier
                                    .padding(top = 12.dp)
                                    .background(
                                        shape = CircleShape,
                                        color = Color(0x33A3A3A3),
                                    )
                                    .size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "停止",
                                    Modifier.size(20.dp),
                                    tint = Color(
                                        if (!isSystemInDarkTheme()) 0x66333333
                                        else 0xAAAAAAAA
                                    )
                                )
                            }
                        }
                        LaunchedEffect(Unit) {
                            delay(2000)
                            visibleCloseIcon = true
                        }
                    }
                }
            }
        } catch (e: ConcurrentModificationException) {
            //
        }
    }
//            Box(contentAlignment = Alignment.BottomEnd) {
//                Button(onClick = {
//                    scope.launch { state.animateScrollToItem(mList.size) }
//                },
//                    Modifier
//                        .wrapContentWidth()
//                        .wrapContentHeight()
//                ){
//                    Text(text = "滚动到底部")
//                }
//            }
}

/**
 * @param scope rememberCoroutineScope
 * @param needOnBottom 只有在底部才能执行
 */
fun scrollBottomEvent(
    scope: CoroutineScope, needOnBottom: Boolean = false
) {
    if (!ChatListState.canScrollForward || !needOnBottom) {
        scope.launch {
            scrollBottomEvent()
        }
    }
}

internal suspend fun scrollBottomEvent() {
    ChatListState.animateScrollToItem(Int.MAX_VALUE)
}


val msgBackgroundBrush = Brush.linearGradient(
    listOf(
        Color(0xFF9E82F0), Color(0xFF42A5F5)
    )
)

class MsgContentViewDefaultParam {
    companion object {
        val minWidth: Dp = 20.dp
        val maxWidth: Dp = 252.dp
    }
}

@Composable
private fun NewMsgContentView(
    text: String,
    marginValues: PaddingValues = PaddingValues(14.dp, 6.dp),
    enableMarkDownText: Boolean = false,
    textIsSelectable: Boolean = false,
    isMe: Boolean? = null,
    minWidth: Dp = MsgContentViewDefaultParam.minWidth,
    maxWidth: Dp = MsgContentViewDefaultParam.maxWidth
) {
    if (enableMarkDownText) {
        MarkdownText(markdown = text,
            fontSize = 16.sp,
            textIsSelectable = textIsSelectable,
            modifier = Modifier
                .padding(marginValues)   // margin
                .drawWithCache {
                    val brush = msgBackgroundBrush
                    onDrawBehind {
                        drawPath(Path().apply {
                            addRoundRect(
                                msgRoundRectCreate(size, 16.dp.toPx(), 2.dp.toPx(), isMe?.not())
                            )
                        }, brush = brush)
                    }
                }
                .padding(10.dp)
                .sizeIn(
                    maxWidth = maxWidth, minWidth = minWidth
                ))
    } else {
        Text(text = text, modifier = Modifier
            .padding(marginValues)   // margin
            .drawWithCache {
                val brush = msgBackgroundBrush
                onDrawBehind {
                    drawPath(Path().apply {
                        addRoundRect(
                            msgRoundRectCreate(size, 16.dp.toPx(), 2.dp.toPx(), isMe?.not())
                        )
                    }, brush = brush)
                }
            }
            .padding(10.dp)
            .sizeIn(maxWidth = maxWidth))
    }
}


/**
 * @param size size
 * @param cornerRadius 三圆角
 * @param cornerRadius2 次圆角
 * @param isLeft 真 左上； 假 右下； 空 四个角都用 cornerRadius
 */
fun msgRoundRectCreate(
    size: Size,
    cornerRadius: Float,
    cornerRadius2: Float = 2F,
    isLeft: Boolean? = null
): RoundRect {
    return RoundRect(
        rect = Rect(
            offset = Offset(0f, 0f),
            size = size
        ),
        topLeft = CornerRadius(if (isLeft == null || !isLeft) cornerRadius else cornerRadius2),
        topRight = CornerRadius(cornerRadius),
        bottomLeft = CornerRadius(cornerRadius),
        bottomRight = CornerRadius(if (isLeft == null || isLeft) cornerRadius else cornerRadius2),
    )
}

private fun isMarkDownImage(text: String, onlyImage: Boolean = false): Boolean {
    return if (onlyImage) text.startsWith("![") && text.contains("](")
    else {
        var imageMiddleIndex = text.indexOf("](")
        while (imageMiddleIndex != -1) {
            val secondIndex = text.lastIndexOf("[", imageMiddleIndex)
            if (secondIndex > 0 && text[secondIndex - 1] == '!') {
                return true
            }
            imageMiddleIndex = text.indexOf("](", imageMiddleIndex + 10)
        }
        false
    }
}