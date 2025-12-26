package br.com.irse.verse.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object HtmlTextFormatter {
    
    // Simplistic HTML parser for basic tags: <b>, <i>, <br>
    // This assumes the input HTML is relatively well-formed for these specific tags
    fun format(html: String): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0
            val regex = Regex("(<(?<tag>b|i|br|/b|/i)>)")
            val matches = regex.findAll(html)

            // Track active styles
            val styles = java.util.Stack<SpanStyle>()
            
            for (match in matches) {
                // Append text before the tag
                if (match.range.first > currentIndex) {
                    append(html.substring(currentIndex, match.range.first))
                }

                val tag = match.groups["tag"]?.value?.lowercase() ?: ""

                when (tag) {
                    "b" -> {
                        val style = SpanStyle(fontWeight = FontWeight.Bold)
                        pushStyle(style)
                    }
                    "i" -> {
                        val style = SpanStyle(fontStyle = FontStyle.Italic)
                        pushStyle(style)
                    }
                    "/b", "/i" -> {
                         // In a perfect world we check if it matches the top, 
                         // but for simple text we just try to pop
                         try { pop() } catch (e: Exception) {}
                    }
                    "br" -> {
                        append("\n")
                    }
                }
                
                currentIndex = match.range.last + 1
            }

            // Append remaining text
            if (currentIndex < html.length) {
                append(html.substring(currentIndex))
            }
        }
    }
}
