package com.alphi.airobot

import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alphi.airobot.database.ChatGptApiDBHelper
import com.alphi.airobot.entity.MsgData
import com.alphi.airobot.model.AiModel
import com.alphi.airobot.model.OpenAiModel
import com.alphi.airobot.model.OpenModelSettingDialog
import com.alphi.airobot.model.OpenSettingsDialog
import com.alphi.airobot.ui.theme.MyApplicationTheme
import com.alphi.airobot.utils.CrashHandle
import com.alphi.airobot.view.ChatBoxView
import com.alphi.airobot.view.InitChatBoxView
import com.alphi.airobot.view.InitInputView
import com.alphi.airobot.view.msgBackgroundBrush
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OpenAiModel.initProperty(this)
        ChatBoxView.createOneClock()
        val directory =
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "ApkExport"
            )
        Log.d("TAG", "onCreate: ${directory.listFiles()!!.filter { it.isFile }}")
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.background
                ) {
                    FuncView()
                }
            }
        }
        val chatGptApiDBHelper = ChatGptApiDBHelper(this)
        chatGptApiDBHelper.writableDatabase
        CrashHandle.getInstance(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        OpenAiModel.closeOpenDBHelper()
    }
}

//@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuncView() {
    val mList = remember { mutableStateListOf<MsgData>() }
    val showSettingsDialogState = remember { mutableStateOf(false) }
    var showModelSettingDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val model = remember { mutableStateOf(AiModel) }
//    LaunchedEffect(Unit) {
//        var date = Date()
//        val msgData = MsgData("现在是北京时间：$date")
//        mList.add(msgData)
//        val index = mList.lastIndexOf(msgData)
//        while (true) {
//            date = Date()
//            delay(1000)
//            mList[index] = msgData.copy("现在是北京时间：$date")
//        }
//    }
    Column(Modifier.fillMaxHeight()) {
        TopAppBar(
            title = {
                Text(text = "Chat GPT")
            },
            actions = {         // 菜单
                Text(
                    text = model.value.getName().replaceFirst("gpt", "GPT"),
                    Modifier.clickable {
                        showModelSettingDialog = true
                    },
                )
                IconButton(onClick = { scope.launch { showSettingsDialogState.value = true } }) {
                    Icon(Icons.Filled.Settings, "设置")
                }
            },
            colors = appBarColors()
        )
        Box(
            Modifier
                .weight(1F)
                .fillMaxWidth()
        ) {
            InitChatBoxView(mList)
        }
        InitInputView(mList)
    }
    OpenSettingsDialog(showSettingsDialogState)
    if (showModelSettingDialog)
        OpenModelSettingDialog(
            dismiss = {
                showModelSettingDialog = false
            },
            model = model,
            msgDataList = mList
        )
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Row(
        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start
    ) {
        Text(text = "6666", modifier = Modifier
            .padding(PaddingValues(14.dp, 6.dp))   // margin
            .drawWithCache {
                val brush = msgBackgroundBrush
                val cornerRadius = CornerRadius(12.dp.toPx())
                onDrawBehind {
                    drawPath(Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = Rect(
                                    offset = Offset(0f, 0f),
                                    size = size,
                                ),
                                topRight = CornerRadius(2.dp.toPx()),
                                topLeft = cornerRadius,
                                bottomLeft = cornerRadius,
                                bottomRight = cornerRadius,
                            )
                        )
                    }, brush = brush)
                }
            }
            .padding(10.dp)
            .sizeIn(maxWidth = 230.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun appBarColors(): TopAppBarColors {
    return TopAppBarDefaults.topAppBarColors(
        containerColor = colorScheme.primary,
        titleContentColor = Color.White,
        actionIconContentColor = Color.White
    )
}

