package com.alphi.airobot

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alphi.airobot.ui.theme.MyApplicationTheme


class CrashReportActivity : ComponentActivity() {
    companion object {
        internal const val ExtraMsgKey = "crash-msg"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val msg = intent.getStringExtra(ExtraMsgKey)
        if (msg == null) {
            finish()
            return
        }
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(msg)
                }
            }
        }
    }
}


@Composable
fun Greeting(msg: String) {
    val manager = LocalClipboardManager.current
    val context = LocalContext.current
    Box (Modifier.padding(8.dp, 2.dp)) {
        Text(
            text = "程序出现异常！以下是报错信息:\n$msg",
            color = Color.Red,
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .horizontalScroll(state = rememberScrollState()),
        )
        Button(onClick = {
            manager.setText(AnnotatedString(msg))
            Toast.makeText(context, "复制报错信息成功！", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.padding(10.dp, 70.dp).align(Alignment.BottomEnd)) {
            Text(text = "复制")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    MyApplicationTheme {
        Greeting("Android")
    }
}