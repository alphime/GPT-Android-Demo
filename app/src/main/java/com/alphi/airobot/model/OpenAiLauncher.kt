package com.alphi.airobot.model

import com.alphi.airobot.entity.MsgData
import com.alphi.airobot.view.ICONBTN_TYPE_ENABLE_STOP_RECEIVE
import com.alphi.airobot.view.ICONBTN_TYPE_RESEND
import com.alphi.airobot.view.hiddenTempNewMsgTextAndEnableSendBtn
import com.alphi.airobot.view.scrollBottomEvent
import com.alphi.airobot.view.setTempNewMsgText
import kotlinx.coroutines.CoroutineScope

class OpenAiLauncher {
    companion object {
        fun launchAiQuestion(list: MutableList<MsgData>, text: String?, scope: CoroutineScope, closeListener: (()->Unit)? = null) {
            OpenAiModel.launchAiQuestion(text, closeListener = {
                it.selectable = true
                if (it.isErr) {
                    setTempNewMsgText(it.text, extendIconButton = ICONBTN_TYPE_RESEND)
                } else {
                    hiddenTempNewMsgTextAndEnableSendBtn()
                    list.add(it)
                }
                closeListener?.invoke()
            }, eventListener = {
                scrollBottomEvent(scope, needOnBottom = true)
                setTempNewMsgText(it, ICONBTN_TYPE_ENABLE_STOP_RECEIVE)
            })
        }
    }
}