package com.alphi.airobot.utils

import android.content.Context
import android.content.Intent
import android.os.Looper
import com.alphi.airobot.CrashReportActivity
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.lang.Thread.UncaughtExceptionHandler


class CrashHandle private constructor(private val content: Context? = null) :
    UncaughtExceptionHandler {
    private val mDefaultExceptionHandler: UncaughtExceptionHandler =
        Thread.getDefaultUncaughtExceptionHandler()!!
    private var listener: ((crashInfo: String) -> Unit)? = null

    override fun uncaughtException(t: Thread, e: Throwable) {
        val isMainThread = Looper.getMainLooper().thread == t
        if (content != null) {
            val crashInfo = parseCrashInfo(e)
            listener?.invoke(crashInfo)
            val intent = Intent(content, CrashReportActivity::class.java)
            intent.putExtra(CrashReportActivity.ExtraMsgKey, crashInfo)
            if (isMainThread) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            } else {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            content.startActivity(intent)
        }
        // 确保主线程异常退出！！！
        if (isMainThread || content == null) {
            mDefaultExceptionHandler.uncaughtException(t, e)
        }
    }

    companion object {
        fun getInstance(
            content: Context,
            listener: ((crashInfo: String) -> Unit)? = null
        ): CrashHandle {
            val handle = CrashHandle(content)
            Thread.setDefaultUncaughtExceptionHandler(handle)
            handle.listener = listener
            return handle
        }
    }

    private fun parseCrashInfo(ex: Throwable): String {
        val writer: Writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        return writer.toString()
    }

    fun setListener(listener: (String) -> Unit) {
        this.listener = listener
    }
}