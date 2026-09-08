package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.EpisodeDto
import com.example.data.api.ShowDto
import com.example.data.backup.TvBackupManager
import com.example.data.db.AppDatabase
import com.example.data.db.EpisodeEntity
import com.example.data.db.ListStatus
import com.example.data.db.NextUpShowEpisode
import com.example.data.db.ShowWithProgress
import com.example.data.repository.OverallStatistics
import com.example.data.repository.TvRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class TvViewModel(
    application: Application,
    private val repository: TvRepository = TvRepository(AppDatabase.getInstance(application).tvDao())
) : AndroidViewModel(application) {

    // --- Dashboard / My Shows ---
    private val _selectedStatusTab = MutableStateFlow(ListStatus.WATCHING)
    val selectedStatusTab: StateFlow<String> = _selectedStatusTab.asStateFlow()

    val myShows: StateFlow<List<ShowWithProgress>> = _selectedStatusTab
        .flatMapLatest { status -> repository.getShowsWithProgress(status) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTrackedShows: StateFlow<List<ShowWithProgress>> = repository.getShowsWithProgress(null)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nextUpEpisodes: StateFlow<List<NextUpShowEpisode>> = repository.getNextUpEpisodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Explore / Search ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ShowDto>>(emptyList())
    val searchResults: StateFlow<List<ShowDto>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // Default popular/recommended shows
    private val _trendingShows = MutableStateFlow<List<ShowDto>>(emptyList())
    val trendingShows: StateFlow<List<ShowDto>> = _trendingShows.asStateFlow()

    // --- Detail Screen ---
    private val _currentShowId = MutableStateFlow<Long?>(null)
    val currentShowId: StateFlow<Long?> = _currentShowId.asStateFlow()

    val currentShowWithProgress: StateFlow<ShowWithProgress?> = _currentShowId
        .flatMapLatest { id ->
            if (id != null) repository.getShowWithProgress(id)
            else kotlinx.coroutines.flow.flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentEpisodes: StateFlow<List<EpisodeEntity>> = _currentShowId
        .flatMapLatest { id ->
            if (id != null) repository.getEpisodes(id)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Remote show details when viewing an unadded show from search
    private val _remoteShowDto = MutableStateFlow<ShowDto?>(null)
    val remoteShowDto: StateFlow<ShowDto?> = _remoteShowDto.asStateFlow()

    private val _remoteEpisodesDto = MutableStateFlow<List<EpisodeDto>>(emptyList())
    val remoteEpisodesDto: StateFlow<List<EpisodeDto>> = _remoteEpisodesDto.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason.asStateFlow()

    // --- Statistics ---
    val statistics: StateFlow<OverallStatistics> = repository.getStatistics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallStatistics())

    // --- Notification / Feedback ---
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadTrendingShows()
        observeSearchQueryDebounce()
    }

    fun onSelectStatusTab(tab: String) {
        _selectedStatusTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun observeSearchQueryDebounce() {
        viewModelScope.launch {
            _searchQuery
                .debounce(350)
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun performSearch(query: String) {
        searchJob?.cancel()
        if (query.trim().isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            _searchError.value = null
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            val result = repository.searchShows(query.trim())
            result.onSuccess { shows ->
                _searchResults.value = shows
                _isSearching.value = false
            }.onFailure { err ->
                _searchError.value = err.message ?: "Failed to load shows"
                _isSearching.value = false
            }
        }
    }

    private fun loadTrendingShows() {
        viewModelScope.launch {
            // Pre-seed popular searches like "drama", "action", etc. for great initial screen content
            val result = repository.searchShows("star")
            result.onSuccess { shows ->
                _trendingShows.value = shows.take(12)
            }
        }
    }

    fun loadShowDetails(showId: Long, fallbackShowDto: ShowDto? = null) {
        _currentShowId.value = showId
        _selectedSeason.value = 1
        _isLoadingDetail.value = true

        viewModelScope.launch {
            if (showId < 0) {
                // Custom show already stored in Room
                _remoteShowDto.value = null
                _remoteEpisodesDto.value = emptyList()
                _isLoadingDetail.value = false
                return@launch
            }

            // Check if show is already saved in DB
            val showInDb = currentShowWithProgress.value
            if (showInDb == null && fallbackShowDto != null) {
                _remoteShowDto.value = fallbackShowDto
            }

            // Always fetch fresh episodes & info from TVmaze
            val episodesResult = repository.fetchShowEpisodes(showId)
            episodesResult.onSuccess { eps ->
                _remoteEpisodesDto.value = eps
                // If this show is already tracked, sync any new episodes into Room
                if (showInDb != null) {
                    val dto = fallbackShowDto ?: repository.fetchShowDetail(showId).getOrNull()
                    if (dto != null) {
                        repository.addOrTrackShow(dto, showInDb.listStatus, eps)
                    }
                }
            }

            if (fallbackShowDto == null && showInDb == null) {
                val detailResult = repository.fetchShowDetail(showId)
                detailResult.onSuccess { dto ->
                    _remoteShowDto.value = dto
                }
            }
            _isLoadingDetail.value = false
        }
    }

    fun onAddCustomShow(
        name: String,
        summary: String? = null,
        imageUrl: String? = null,
        genres: String = "",
        seasonsCount: Int = 1,
        episodesPerSeason: Int = 10,
        averageRuntime: Int = 45,
        networkName: String? = null,
        listStatus: String = ListStatus.WATCHING,
        premieredYear: String? = null,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.addCustomShow(
                name = name,
                summary = summary,
                imageUrl = imageUrl,
                genres = genres,
                seasonsCount = seasonsCount,
                episodesPerSeason = episodesPerSeason,
                averageRuntime = averageRuntime,
                networkName = networkName,
                listStatus = listStatus,
                premieredYear = premieredYear
            )
            result.onSuccess { newShowId ->
                _userMessage.value = "\"$name\" added to $listStatus!"
                onSuccess(newShowId)
            }.onFailure { err ->
                _userMessage.value = "Failed to add custom show: ${err.message}"
            }
        }
    }

    fun onAddCustomEpisode(
        showId: Long,
        season: Int,
        name: String? = null,
        runtime: Int? = null
    ) {
        viewModelScope.launch {
            val result = repository.addCustomEpisode(showId, season, name, runtime)
            result.onSuccess { ep ->
                _userMessage.value = "${ep.name ?: "Episode"} added to Season $season!"
            }.onFailure { err ->
                _userMessage.value = "Failed to add episode: ${err.message}"
            }
        }
    }

    fun onSelectSeason(season: Int) {
        _selectedSeason.value = season
    }

    fun onToggleEpisode(episodeId: Long, currentWatched: Boolean) {
        viewModelScope.launch {
            repository.toggleEpisodeWatched(episodeId, currentWatched)
        }
    }

    fun onQuickToggleNextUp(episodeId: Long) {
        viewModelScope.launch {
            repository.toggleEpisodeWatched(episodeId, false)
            _userMessage.value = "Episode marked as watched!"
        }
    }

    fun onMarkSeasonWatched(showId: Long, season: Int, isWatched: Boolean) {
        viewModelScope.launch {
            repository.markSeasonWatched(showId, season, isWatched)
            _userMessage.value = if (isWatched) "Season $season marked watched" else "Season $season unmarked"
        }
    }

    fun onMarkPreviousEpisodesWatched(showId: Long, upToSeason: Int, upToNumber: Int) {
        viewModelScope.launch {
            repository.markPreviousEpisodesWatched(showId, upToSeason, upToNumber)
            _userMessage.value = "Marked previous episodes as watched"
        }
    }

    fun onMarkAllEpisodesWatched(showId: Long, isWatched: Boolean) {
        viewModelScope.launch {
            repository.markAllEpisodesWatched(showId, isWatched)
            _userMessage.value = if (isWatched) "All episodes marked as watched!" else "All episodes unmarked"
        }
    }

    fun onUpdateShowStatus(showId: Long, status: String) {
        viewModelScope.launch {
            repository.updateShowStatus(showId, status)
            _userMessage.value = "Moved to $status"
        }
    }

    fun onUpdateUserRating(showId: Long, rating: Int) {
        viewModelScope.launch {
            repository.updateUserRating(showId, rating)
            _userMessage.value = if (rating > 0) "Rated $rating/10" else "Rating cleared"
        }
    }

    fun onTrackShow(showDto: ShowDto, initialStatus: String = ListStatus.WATCHING) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            val eps = _remoteEpisodesDto.value.ifEmpty {
                repository.fetchShowEpisodes(showDto.id).getOrNull() ?: emptyList()
            }
            repository.addOrTrackShow(showDto, initialStatus, eps)
            _userMessage.value = "Added to $initialStatus"
            _isLoadingDetail.value = false
        }
    }

    fun onRemoveShow(showId: Long) {
        viewModelScope.launch {
            repository.removeShow(showId)
            _userMessage.value = "Removed from My Shows"
        }
    }

    suspend fun getExportNote(): String {
        val currentShows = allTrackedShows.value
        val stats = statistics.value
        return TvBackupManager.generateExportNote(currentShows, stats)
    }

    suspend fun getExportJson(): String {
        val shows = repository.getAllShowsDirect()
        val episodes = repository.getAllEpisodesDirect()
        return TvBackupManager.exportToJson(shows, episodes)
    }

    fun importBackup(jsonString: String, onComplete: (Result<Pair<Int, Int>>) -> Unit) {
        viewModelScope.launch {
            try {
                val backupData = TvBackupManager.parseBackupJson(jsonString)
                if (backupData.shows.isEmpty()) {
                    val res = Result.failure<Pair<Int, Int>>(IllegalArgumentException("Backup contains no shows to restore."))
                    onComplete(res)
                    return@launch
                }
                repository.restoreBackup(backupData.shows, backupData.episodes)
                _userMessage.value = "Restored ${backupData.shows.size} shows & ${backupData.episodes.size} episodes!"
                onComplete(Result.success(Pair(backupData.shows.size, backupData.episodes.size)))
            } catch (e: Exception) {
                val res = Result.failure<Pair<Int, Int>>(e)
                _userMessage.value = "Import error: ${e.localizedMessage ?: "Invalid JSON"}"
                onComplete(res)
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}

class TvViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TvViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TvViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
