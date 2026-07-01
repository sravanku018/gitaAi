package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.SearchResult
import com.aipoweredgita.app.database.VerseSearchDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class GitaSearchViewModel @Inject constructor(
    private val searchDao: VerseSearchDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<SearchResult>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                // SQLite FTS requires the query to end with * for prefix matching
                val formattedQuery = query.trim().split("\\s+".toRegex()).joinToString(" ") { "$it*" }
                searchDao.searchFlow(formattedQuery)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
