package com.alphi.airobot.compose

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.annotation.IdRes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import coil.ImageLoader
import coil.util.DebugLogger
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.ImageSize
import io.noties.markwon.image.ImageSizeResolverDef
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import me.saket.bettermovementmethod.BetterLinkMovementMethod

enum class MarkdownViewLinkType {
    Image, WebLink;
}

@Composable
fun MarkdownView(
    content: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    textIsSelectable: Boolean = false,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    @FontRes fontResource: Int? = null,
    style: TextStyle = LocalTextStyle.current,
    @IdRes viewId: Int? = null,
    // this option will disable all clicks on links, inside the markdown text
    // it also enable the parent view to receive the click event
    imageLoader: ImageLoader? = null,
    onTextLayout: ((numLines: Int) -> Unit)? = null,
    onLinkClickListener: ((link: String, type: MarkdownViewLinkType) -> Unit)? = null
) {
    val defaultColor: Color = LocalContentColor.current.copy()
    val context: Context = LocalContext.current
    val markdownRender: Markwon =
        createMarkdownRender(context, imageLoader, onLinkClicked = {
//            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            if (it.contains("://")) {
                if (onLinkClickListener != null)
                    onLinkClickListener(it, MarkdownViewLinkType.WebLink)
                else {
                    val intent = Intent()
                    intent.data = Uri.parse(it)
                    context.startActivity(intent)
                }
            }
        })
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            createTextView(
                context = ctx,
                color = color,
                defaultColor = defaultColor,
                fontSize = fontSize,
                fontResource = fontResource,
                maxLines = maxLines,
                style = style,
                textAlign = textAlign,
                viewId = viewId,
                onClick = null,
                isSelectable = textIsSelectable,
            )
        },
        update = { textView ->
            markdownRender.setMarkdown(textView, content)
            if (onTextLayout != null) {
                textView.post {
                    onTextLayout(textView.lineCount)
                }
            }
            textView.maxLines = maxLines
        }
    )
}

private fun createTextView(
    context: Context,
    color: Color = Color.Unspecified,
    defaultColor: Color,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    isSelectable: Boolean,
    maxLines: Int = Int.MAX_VALUE,
    @FontRes fontResource: Int? = null,
    style: TextStyle,
    @IdRes viewId: Int? = null,
    onClick: (() -> Unit)? = null
): TextView {

    val textColor = color.takeOrElse { style.color.takeOrElse { defaultColor } }
    val mergedStyle = style.merge(
        TextStyle(
            color = textColor,
            fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize,
            textAlign = textAlign ?: TextAlign.Start,
        )
    )
    return TextView(context).apply {
        setTextColor(textColor.toArgb())
        setMaxLines(maxLines)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, mergedStyle.fontSize.value)
        setTextIsSelectable(isSelectable)
        letterSpacing = 0.02f
        setLineSpacing(0f, 1.14f)
        movementMethod =
            BetterLinkMovementMethod.getInstance()        // 解决link点击与textSelectable冲突问题
        onClick?.let { setOnClickListener { onClick() } }
        viewId?.let { id = viewId }
        textAlign?.let { align ->
            textAlignment = when (align) {
                TextAlign.Left, TextAlign.Start -> View.TEXT_ALIGNMENT_TEXT_START
                TextAlign.Right, TextAlign.End -> View.TEXT_ALIGNMENT_TEXT_END
                TextAlign.Center -> View.TEXT_ALIGNMENT_CENTER
                else -> View.TEXT_ALIGNMENT_TEXT_START
            }
        }

        if (mergedStyle.textDecoration == TextDecoration.LineThrough) {
            paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
        }

        fontResource?.let { font ->
            typeface = ResourcesCompat.getFont(context, font)
        }
    }
}

private fun createMarkdownRender(
    context: Context,
    imageLoader: ImageLoader?,
    onLinkClicked: ((String) -> Unit)? = null
): Markwon {
    val coilImageLoader = imageLoader ?: ImageLoader.Builder(context)
        .apply {
            crossfade(true)
        }.logger(DebugLogger()).build()

    return Markwon.builder(context)
        .usePlugin(HtmlPlugin.create())
        .usePlugin(CoilImagesPlugin.create(context, coilImageLoader))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { view, link ->
                    // handle individual clicks on Textview link
                    onLinkClicked?.invoke(link)
                }
                builder.imageSizeResolver(FitWidthImageSizeResolver())

            }

        })
        .build()
}

private class FitWidthImageSizeResolver : ImageSizeResolverDef() {
    override fun resolveImageSize(drawable: AsyncDrawable): Rect {
        return resolveImageSize(
            ImageSize(
                ImageSize.Dimension(76f, UNIT_PERCENT),
                null
            ),
            // important detail - `drawable.result` bounds must be used
            drawable.result.bounds,
            drawable.lastKnownCanvasWidth,
            drawable.lastKnowTextSize
        )
    }
}
