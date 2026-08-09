package com.aipoweredgita.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object TeluguTransliterator {

    private val vowelMap = buildMap {
        val base = mapOf(
            "a" to "అ", "aa" to "ఆ", "i" to "ఇ", "ee" to "ఈ",
            "u" to "ఉ", "oo" to "ఊ", "e" to "ఏ", "ai" to "ఐ",
            "o" to "ఓ", "au" to "ఔ", "am" to "అం", "ah" to "అః"
        )
        putAll(base)
        base.forEach { (k, v) -> put(k.uppercase(), v) }
    }

    private val consonantMap = mapOf(
        "ka" to "క", "kha" to "ఖ", "ga" to "గ", "gha" to "ఘ", "nga" to "ఙ",
        "cha" to "చ", "chha" to "ఛ", "ja" to "జ", "jha" to "ఝ", "nja" to "ఞ",
        "Ta" to "ట", "Tha" to "ఠ", "Da" to "డ", "Dha" to "ఢ", "Na" to "ణ",
        "tha" to "త", "thha" to "థ", "da" to "ద", "dhha" to "ధ", "na" to "న",
        "pa" to "ప", "pha" to "ఫ", "ba" to "బ", "bha" to "భ", "ma" to "మ",
        "ya" to "య", "ra" to "ర", "la" to "ల", "va" to "వ",
        "sha" to "శ", "shha" to "ష", "sa" to "స", "ha" to "హ",
        "ksha" to "క్ష", "trna" to "ట్ర"
    )

    private val matraMap = mapOf(
        "aa" to "ా", "i" to "ి", "ee" to "ీ", "u" to "ు",
        "oo" to "ూ", "e" to "ే", "ai" to "ై", "o" to "ో",
        "au" to "ౌ", "am" to "ం", "ah" to "ః"
    )

    private val halant = "్"
    private val digitMap = mapOf(
        "0" to "౦", "1" to "౧", "2" to "౨", "3" to "౩", "4" to "౪",
        "5" to "౫", "6" to "౬", "7" to "౭", "8" to "౮", "9" to "౯"
    )

    fun transliterate(input: String): String {
        if (input.isBlank()) return input

        val result = StringBuilder()
        var i = 0

        while (i < input.length) {
            var matched = false

            for (len in 6 downTo 1) {
                if (i + len <= input.length) {
                    val chunk = input.substring(i, i + len)
                    if (vowelMap.containsKey(chunk)) {
                        result.append(vowelMap[chunk])
                        i += len
                        matched = true
                        break
                    }
                    if (consonantMap.containsKey(chunk)) {
                        result.append(consonantMap[chunk])
                        i += len
                        matched = true
                        break
                    }
                    val lowerChunk = chunk.lowercase()
                    if (lowerChunk != chunk) {
                        if (vowelMap.containsKey(lowerChunk)) {
                            result.append(vowelMap[lowerChunk])
                            i += len
                            matched = true
                            break
                        }
                        if (consonantMap.containsKey(lowerChunk)) {
                            result.append(consonantMap[lowerChunk])
                            i += len
                            matched = true
                            break
                        }
                    }
                }
            }

            if (!matched) {
                val ch = input[i]
                when {
                    ch == ' ' -> result.append(' ')
                    ch.isDigit() -> result.append(digitMap[ch.toString()] ?: ch)
                    ch == '.' || ch == ',' || ch == '!' || ch == '?' -> result.append(ch)
                    else -> result.append(ch)
                }
                i++
            }
        }

        return result.toString()
    }
}

@Composable
fun TransliterateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Type in English",
    placeholder: String = "e.g. nenu krishnudu",
    enabled: Boolean = true
) {
    var isTransliterateMode by remember { mutableStateOf(false) }
    var englishInput by remember { mutableStateOf(value) }

    Column(modifier = modifier) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = if (isTransliterateMode) englishInput else value,
                onValueChange = { newValue ->
                    if (isTransliterateMode) {
                        englishInput = newValue
                        onValueChange(TeluguTransliterator.transliterate(newValue))
                    } else {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                enabled = enabled
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { isTransliterateMode = !isTransliterateMode }) {
                Icon(
                    Icons.Default.Translate,
                    contentDescription = "Toggle transliteration",
                    tint = if (isTransliterateMode) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        if (isTransliterateMode) {
            Text(
                "Telugu transliteration ON — type phonetically in English",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
