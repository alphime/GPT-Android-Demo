package com.example.airobot

import org.junit.Test
import java.util.regex.Pattern


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val str = """
            pppp| 姓名 | 年龄 | 性别 |
                                                                         | ---- | ---- | ---- |
                                                                         | 张三 | 25   | 男   |
                                                                         | 李四 | 30   | 女   |
                                                                         | 王五 | 28   | 男   |
                                                                         | 赵六 | 35   | 女   |
        """.trimIndent()
//        println(str.contains(Regex("\\|\\s-+?\\s\\|")))


        println(isMarkDownImage("""
            ## 标题2
                                                                         ### 标题3
                                                                         
                                                                         这是一段普通的文本。
                                                                         
                                                                         这是另一段普通的文本。
                                                                         
                                                                         * 列表1
                                                                         * 列表2
                                                                         * 列表3
                                                                         
                                                                         1. 有序列表1
                                                                         2. 有序列表2
                                                                         3. 有序列表3
                                                                         
                                                                         **加粗文本**
                                                                         
                                                                         *斜体文本*
                                                                         
                                                                         [链接文字](链接地址)
                                                                         
                                                                         ![百度logo](https://www.baidu.com/img/flexible/logo/pc/result.png)
                                                                         
                                                                         上面的文字
                                                                         
                                                                         ![百度logo](https://www.baidu.com/img/flexible/logo/pc/result.png)
                                                                         
                                                                         下面的文字
                                                                         
        """.trimIndent()))
        println(isMarkDownImage(str))
        println(6666)
//        println(str.replace(Regex("\\\\|.+\\\\|"), "_<表格生成中...>"))
    }
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
            imageMiddleIndex = text.indexOf("](", imageMiddleIndex + 3)
        }
        false
    }
}