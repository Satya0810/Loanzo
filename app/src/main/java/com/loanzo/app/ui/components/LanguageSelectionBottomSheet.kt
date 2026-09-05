package com.loanzo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.ui.theme.Emerald400
import com.loanzo.app.ui.theme.Gold500
import com.loanzo.app.ui.theme.Navy700
import com.loanzo.app.ui.theme.Navy900

data class SupportedLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String
)

val APP_SUPPORTED_LANGUAGES = listOf(
    SupportedLanguage("en", "English", "English"),
    SupportedLanguage("hi", "हिन्दी", "Hindi"),
    SupportedLanguage("mr", "मराठी", "Marathi"),
    SupportedLanguage("bn", "বাংলা", "Bengali"),
    SupportedLanguage("te", "తెలుగు", "Telugu"),
    SupportedLanguage("ta", "தமிழ்", "Tamil"),
    SupportedLanguage("gu", "ગુજરાતી", "Gujarati"),
    SupportedLanguage("kn", "ಕನ್ನಡ", "Kannada"),
    SupportedLanguage("ml", "മലയാളം", "Malayalam"),
    SupportedLanguage("pa", "ਪੰਜਾਬੀ", "Punjabi"),
    SupportedLanguage("or", "ଓଡ଼ିଆ", "Odia"),
    SupportedLanguage("ur", "اردو", "Urdu"),
    SupportedLanguage("as", "অসমীয়া", "Assamese"),
    SupportedLanguage("ne", "नेपाली", "Nepali"),
    SupportedLanguage("sa", "संस्कृतम्", "Sanskrit"),
    SupportedLanguage("ar", "العربية", "Arabic"),
    SupportedLanguage("es", "Español", "Spanish"),
    SupportedLanguage("fr", "Français", "French"),
    SupportedLanguage("de", "Deutsch", "German"),
    SupportedLanguage("ja", "日本語", "Japanese"),
    SupportedLanguage("ru", "Русский", "Russian")
)

fun getLanguageNameByCode(code: String): String {
    val lang = APP_SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
    return lang?.let { "${it.nativeName} (${it.englishName})" } ?: code.uppercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionBottomSheet(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            APP_SUPPORTED_LANGUAGES
        } else {
            val query = searchQuery.trim().lowercase()
            APP_SUPPORTED_LANGUAGES.filter {
                it.englishName.lowercase().contains(query) ||
                it.nativeName.lowercase().contains(query) ||
                it.code.lowercase().contains(query)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Gold500.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = Gold500,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Select App Language",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Change the language everywhere in Loanzo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search language (e.g. Hindi, मराठी, Tamil)...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold500,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Language List
            if (filteredLanguages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No languages found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(filteredLanguages, key = { it.code }) { lang ->
                        val isSelected = lang.code.equals(currentLanguageCode, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Gold500.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    onLanguageSelected(lang.code)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Gold500 else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = lang.englishName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = Emerald400,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Navy900,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}
