package com.example.ui.chat

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AerisMonoFont
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

private sealed interface MarkdownSegment {
    data class Prose(val text: String) : MarkdownSegment
    data class CodeBlock(val language: String, val code: String) : MarkdownSegment
}

@Composable
fun MarkdownText(
    markdown: String,
    textColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    val segments = remember(markdown) { parseMarkdownSegments(markdown) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        segments.forEach { segment ->
            when (segment) {
                is MarkdownSegment.CodeBlock -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = HoloBlack,
                        border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.7f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HoloSurfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = segment.language.ifBlank { "code" }.uppercase(),
                                    style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = CyanBright
                                )
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            clipboard.setText(AnnotatedString(segment.code))
                                            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Code",
                                        tint = CyanNeon,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Copy Code",
                                        style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = CyanNeon
                                    )
                                }
                            }
                            Text(
                                text = segment.code,
                                style = Typography.bodyMedium.copy(
                                    fontFamily = AerisMonoFont,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                ),
                                color = TextWhite,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(10.dp)
                            )
                        }
                    }
                }

                is MarkdownSegment.Prose -> {
                    val lines = segment.text.lines()
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        lines.forEach { rawLine ->
                            val trimmed = rawLine.trimEnd()
                            when {
                                trimmed.startsWith("### ") -> {
                                    Text(
                                        text = buildInlineAnnotated(trimmed.removePrefix("### "), CyanBright, boldAll = true),
                                        style = Typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                        color = CyanBright
                                    )
                                }
                                trimmed.startsWith("## ") || trimmed.startsWith("# ") -> {
                                    val clean = trimmed.removePrefix("## ").removePrefix("# ")
                                    Text(
                                        text = buildInlineAnnotated(clean, CyanNeon, boldAll = true),
                                        style = Typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                        color = CyanNeon
                                    )
                                }
                                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = "• ",
                                            style = Typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                            color = CyanNeon
                                        )
                                        Text(
                                            text = buildInlineAnnotated(trimmed.drop(2), textColor),
                                            style = Typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                                            color = textColor
                                        )
                                    }
                                }
                                trimmed.isBlank() -> {
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                else -> {
                                    Text(
                                        text = buildInlineAnnotated(trimmed, textColor),
                                        style = Typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseMarkdownSegments(input: String): List<MarkdownSegment> {
    val result = mutableListOf<MarkdownSegment>()
    val regex = Regex("```([A-Za-z0-9_+\\-#.]*)\\n([\\s\\S]*?)```")
    var lastEnd = 0
    for (match in regex.findAll(input)) {
        if (match.range.first > lastEnd) {
            val prose = input.substring(lastEnd, match.range.first).trim()
            if (prose.isNotEmpty()) result.add(MarkdownSegment.Prose(prose))
        }
        val lang = match.groupValues[1].trim()
        val code = match.groupValues[2].trimEnd()
        result.add(MarkdownSegment.CodeBlock(lang, code))
        lastEnd = match.range.last + 1
    }
    if (lastEnd < input.length) {
        val tail = input.substring(lastEnd).trim()
        if (tail.isNotEmpty()) result.add(MarkdownSegment.Prose(tail))
    }
    if (result.isEmpty() && input.isNotBlank()) {
        result.add(MarkdownSegment.Prose(input))
    }
    return result
}

private fun buildInlineAnnotated(
    line: String,
    defaultColor: Color,
    boldAll: Boolean = false
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < line.length) {
            when {
                line.startsWith("**", i) -> {
                    val end = line.indexOf("**", i + 2)
                    if (end > i + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextWhite)) {
                            append(line.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }
                line[i] == '`' -> {
                    val end = line.indexOf('`', i + 1)
                    if (end > i + 1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = AerisMonoFont,
                                color = CyanBright,
                                background = HoloSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(" ${line.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        append('`')
                        i += 1
                    }
                }
                else -> {
                    withStyle(
                        SpanStyle(
                            color = defaultColor,
                            fontWeight = if (boldAll) FontWeight.Bold else FontWeight.Normal
                        )
                    ) {
                        append(line[i])
                    }
                    i += 1
                }
            }
        }
    }
}
