package br.com.irse.verse.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object HtmlTextFormatter {
    
    private val tagRegex = Regex("""<(/?)(\w+)([^>]*)>""")

    fun format(html: String): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0
            val matches = tagRegex.findAll(html)
            
            for (match in matches) {
                if (match.range.first > currentIndex) {
                    append(html.substring(currentIndex, match.range.first))
                }

                val isClosing = match.groupValues[1] == "/"
                val tagName = match.groupValues[2].lowercase()
                val attributes = match.groupValues[3].lowercase()

                if (isClosing) {
                    when (tagName) {
                        "b", "strong", "i", "em", "span", "font", "cite" -> { 
                            try { pop() } catch (e: Exception) { }
                        }
                    }
                } else {
                    when (tagName) {
                        "b", "strong" -> pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        "i", "em", "cite" -> pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        "u" -> pushStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                        "br" -> append("\n")
                        "span", "font" -> {
                            var style = SpanStyle()
                            
                            if (attributes.contains("class")) {
                                if (attributes.contains("quote") || 
                                    attributes.contains("ot") || 
                                    attributes.contains("citac") ||
                                    attributes.contains("cita") ||
                                    attributes.contains("at")) {
                                    style = style.copy(fontWeight = FontWeight.Bold)
                                }
                                
                                if (attributes.contains("jesus") || attributes.contains("red")) {
                                    style = style.copy(color = Color(0xFFC62828))
                                }
                                
                                if (attributes.contains("italic")) {
                                    style = style.copy(fontStyle = FontStyle.Italic)
                                }
                            }
                            
                            if (attributes.contains("color")) {
                                if (attributes.contains("red") || attributes.contains("#ff0000")) {
                                    style = style.copy(color = Color(0xFFC62828))
                                }
                            }
                            
                            pushStyle(style)
                        }
                        else -> { } 
                    }
                }
                
                currentIndex = match.range.last + 1
            }

            if (currentIndex < html.length) {
                append(html.substring(currentIndex))
            }
        }
    }
}
