package com.aipoweredgita.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.database.VerseNote
import com.aipoweredgita.app.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBack: () -> Unit = {},
    onVerseClick: (chapter: Int, verse: Int) -> Unit = { _, _ -> },
    viewModel: NotesViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<VerseNote?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Notes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add Note")
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📝", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("No notes yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap + to add a note to any verse",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            val groupedNotes = remember(notes) {
                notes.sortedWith(compareBy({ it.chapterNo }, { it.verseNo }))
                    .groupBy { it.chapterNo }
            }

            val chapterNames = remember {
                mapOf(
                    1 to "Arjuna Visada Yoga", 2 to "Sankhya Yoga", 3 to "Karma Yoga",
                    4 to "Jnana Karma Sanyasa Yoga", 5 to "Karma Sanyasa Yoga", 6 to "Dhyana Yoga",
                    7 to "Jnana Vijnana Yoga", 8 to "Aksara Brahma Yoga", 9 to "Raja Vidya Raja Guhya Yoga",
                    10 to "Vibhuti Yoga", 11 to "Visvarupa Darsana Yoga", 12 to "Bhakti Yoga",
                    13 to "Ksetra Ksetrajna Vibhaga Yoga", 14 to "Gunatraya Vibhaga Yoga",
                    15 to "Purusottama Yoga", 16 to "Daivasura Sampad Vibhaga Yoga",
                    17 to "Sraddhatraya Vibhaga Yoga", 18 to "Moksa Sanyasa Yoga"
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                groupedNotes.forEach { (chapterNo, chapterNotes) ->
                    item(key = "chapter_header_$chapterNo") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📖 Chapter $chapterNo: ${chapterNames[chapterNo] ?: "Bhagavad Gita"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.weight(1f))
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("${chapterNotes.size} Note${if (chapterNotes.size > 1) "s" else ""}")
                                }
                            }
                        }
                    }

                    items(chapterNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onEdit = { noteToEdit = note },
                            onDelete = {
                                viewModel.deleteNote(note.id, note.chapterNo, note.verseNo)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddNoteDialog(
            onDismiss = { showAddDialog = false },
            onSave = { chapter, verse, text, colorHex ->
                viewModel.addNote(chapter, verse, text, colorHex)
                showAddDialog = false
            }
        )
    }

    if (noteToEdit != null) {
        EditNoteDialog(
            note = noteToEdit!!,
            onDismiss = { noteToEdit = null },
            onSave = { text, colorHex ->
                val current = noteToEdit!!
                viewModel.updateNote(current.id, current.chapterNo, current.verseNo, text, colorHex)
                noteToEdit = null
            }
        )
    }
}

val NOTE_COLORS = listOf(
    "#FFB300", // Saffron Gold
    "#4CAF50", // Emerald Green
    "#2196F3", // Royal Blue
    "#E91E63", // Rose Pink
    "#9C27B0", // Deep Purple
    "#FF5722", // Sunset Amber
    "#00BCD4"  // Celestial Teal
)

fun parseColorHex(hex: String, defaultIndex: Int = 0): androidx.compose.ui.graphics.Color {
    return try {
        if (hex.isNotBlank() && hex.startsWith("#")) {
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
        } else {
            val idx = kotlin.math.abs(defaultIndex) % NOTE_COLORS.size
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(NOTE_COLORS[idx]))
        }
    } catch (_: Exception) {
        val idx = kotlin.math.abs(defaultIndex) % NOTE_COLORS.size
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(NOTE_COLORS[idx]))
    }
}

@Composable
private fun NoteCard(
    note: VerseNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = parseColorHex(note.colorHex, note.id + note.chapterNo * 31 + note.verseNo)
    val cardBg = accentColor.copy(alpha = 0.12f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left color accent bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(accentColor)
            )
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Color indicator dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(accentColor, shape = CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Chapter ${note.chapterNo}, Sloka ${note.verseNo}",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                .format(java.util.Date(note.updatedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Edit Note", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Delete Note", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(note.note, style = MaterialTheme.typography.bodyMedium, maxLines = 8)
            }
        }
    }
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onSave: (chapter: Int, verse: Int, text: String, colorHex: String) -> Unit
) {
    var chapter by remember { mutableStateOf("") }
    var verse by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(NOTE_COLORS.random()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var chapterError by remember { mutableStateOf(false) }
    var verseError by remember { mutableStateOf(false) }
    var textError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chapter,
                        onValueChange = { 
                            chapter = it.filter { c -> c.isDigit() }
                            chapterError = false 
                            errorMessage = null
                        },
                        label = { Text("Chapter (1-18)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = chapterError,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = verse,
                        onValueChange = { 
                            verse = it.filter { c -> c.isDigit() }
                            verseError = false 
                            errorMessage = null
                        },
                        label = { Text("Sloka") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = verseError,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { 
                        noteText = it 
                        textError = false
                        errorMessage = null
                    },
                    label = { Text("Your note") },
                    isError = textError,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    maxLines = 6
                )
                
                Spacer(Modifier.height(4.dp))
                Text("Note Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    NOTE_COLORS.forEach { hex ->
                        val color = parseColorHex(hex)
                        val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(color, shape = CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ch = chapter.toIntOrNull()
                    val vs = verse.toIntOrNull()
                    val maxVerse = if (ch != null && ch in 1..18) {
                        com.aipoweredgita.app.util.GitaConstants.CHAPTER_VERSE_COUNTS[ch] ?: 78
                    } else 78

                    chapterError = ch == null || ch < 1 || ch > 18
                    verseError = vs == null || vs < 1 || vs > maxVerse
                    textError = noteText.isBlank()

                    when {
                        chapterError -> errorMessage = "Invalid Chapter. Enter a number between 1 and 18."
                        verseError -> errorMessage = "Invalid Sloka. Chapter $ch only has $maxVerse slokas."
                        textError -> errorMessage = "Note content cannot be empty."
                        else -> {
                            onSave(ch!!, vs!!, noteText.trim(), selectedColorHex)
                        }
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditNoteDialog(
    note: VerseNote,
    onDismiss: () -> Unit,
    onSave: (text: String, colorHex: String) -> Unit
) {
    var noteText by remember { mutableStateOf(note.note) }
    var selectedColorHex by remember { mutableStateOf(if (note.colorHex.isNotBlank()) note.colorHex else NOTE_COLORS.random()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var textError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Note (Chapter ${note.chapterNo}, Sloka ${note.verseNo})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { 
                        noteText = it 
                        textError = false
                        errorMessage = null
                    },
                    label = { Text("Your note") },
                    isError = textError,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    maxLines = 8
                )
                
                Spacer(Modifier.height(4.dp))
                Text("Note Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    NOTE_COLORS.forEach { hex ->
                        val color = parseColorHex(hex)
                        val isSelected = hex.equals(selectedColorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(color, shape = CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (noteText.isBlank()) {
                        textError = true
                        errorMessage = "Note content cannot be empty."
                    } else {
                        onSave(noteText.trim(), selectedColorHex)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
