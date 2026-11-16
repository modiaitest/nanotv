package app.nanotv.tv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.nanotv.tv.data.local.AppSettingsState
import app.nanotv.tv.data.local.ChannelSummaryModel
import app.nanotv.tv.data.local.CuratedSourceEntity
import app.nanotv.tv.data.local.PlaylistEntity
import app.nanotv.tv.data.local.PlaylistVideoCrossRef
import app.nanotv.tv.data.local.VideoCardModel
import app.nanotv.tv.data.repository.CatalogRepository
import app.nanotv.tv.data.repository.SyncResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavSection(val label: String, val requiresParentPin: Boolean = false) {
    SEARCH("Search"),
    HOME("Home"),
    CHANNELS("Channels"),
    LIBRARY("Library"),
    PARENT_CURATE("Parent Curate", requiresParentPin = true),
    PARENT_SETTINGS("Parent Settings", requiresParentPin = true)
}

enum class LibraryTab(val label: String) {
    PLAYLISTS("Shelves"),
    HISTORY("History"),
    LIKED("Favorites"),
    WATCH_LATER("Watch Later")
}

data class PlaylistShelf(
    val playlist: PlaylistEntity,
    val videos: List<VideoCardModel>
)

data class PlaybackSession(
    val currentVideoId: String,
    val contextTitle: String,
    val queueVideoIds: List<String>
)

data class HomeUiState(
    val playlists: List<PlaylistEntity> = emptyList(),
    val shelves: List<PlaylistShelf> = emptyList(),
    val allVideos: List<VideoCardModel> = emptyList(),
    val continueWatching: List<VideoCardModel> = emptyList(),
    val selectedCategoryId: String? = null,
    val isSyncing: Boolean = false,
    val syncBannerMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: CatalogRepository
) : ViewModel() {

    private val _activeSection = MutableStateFlow(NavSection.HOME)
    val activeSection: StateFlow<NavSection> = _activeSection.asStateFlow()

    private val _isParentUnlocked = MutableStateFlow(false)
    val isParentUnlocked: StateFlow<Boolean> = _isParentUnlocked.asStateFlow()

    private val _pendingPinTarget = MutableStateFlow<NavSection?>(null)
    val pendingPinTarget: StateFlow<NavSection?> = _pendingPinTarget.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _isSyncing = MutableStateFlow(false)
    private val _syncBannerMessage = MutableStateFlow<String?>(null)

    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId: StateFlow<String?> = _selectedChannelId.asStateFlow()

    private val _selectedPlaylistDetailId = MutableStateFlow<String?>(null)
    val selectedPlaylistDetailId: StateFlow<String?> = _selectedPlaylistDetailId.asStateFlow()

    private val _activeLibraryTab = MutableStateFlow(LibraryTab.PLAYLISTS)
    val activeLibraryTab: StateFlow<LibraryTab> = _activeLibraryTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _playbackSession = MutableStateFlow<PlaybackSession?>(null)
    val playbackSession: StateFlow<PlaybackSession?> = _playbackSession.asStateFlow()

    val curatedSourcesState: StateFlow<List<CuratedSourceEntity>> = repository.curatedSourcesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settingsState: StateFlow<AppSettingsState> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsState())

    val channelsState: StateFlow<List<ChannelSummaryModel>> = repository.channelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedChannelVideos: StateFlow<List<VideoCardModel>> = _selectedChannelId
        .flatMapLatest { cid ->
            if (cid == null) flowOf(emptyList()) else repository.observeVideosForChannel(cid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedPlaylistVideos: StateFlow<List<VideoCardModel>> = _selectedPlaylistDetailId
        .flatMapLatest { pid ->
            if (pid == null) flowOf(emptyList()) else repository.observeVideosForPlaylist(pid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val watchHistoryState: StateFlow<List<VideoCardModel>> = repository.watchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val likedVideosState: StateFlow<List<VideoCardModel>> = repository.likedVideosFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val watchLaterState: StateFlow<List<VideoCardModel>> = repository.watchLaterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchResultsState: StateFlow<List<VideoCardModel>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allVideosFlow else repository.searchApprovedVideos(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val catalogBundleFlow = combine(
        repository.playlistsFlow,
        repository.allVideosFlow,
        repository.crossRefsFlow,
        repository.continueWatchingFlow
    ) { playlists, allVideos, crossRefs, continueWatching ->
        CatalogBundle(playlists, allVideos, crossRefs, continueWatching)
    }

    val homeUiState: StateFlow<HomeUiState> = combine(
        catalogBundleFlow,
        _selectedCategoryId,
        _isSyncing,
        _syncBannerMessage
    ) { bundle, selectedCatId, syncing, bannerMsg ->
        val videosById = bundle.allVideos.associateBy { it.videoId }
        val refsByPlaylist = bundle.crossRefs.groupBy { it.playlistId }

        val shelves = bundle.playlists.mapNotNull { playlist ->
            val orderedVideos = refsByPlaylist[playlist.playlistId]
                .orEmpty()
                .sortedBy { it.position }
                .mapNotNull { videosById[it.videoId] }
            if (orderedVideos.isEmpty()) null else PlaylistShelf(playlist, orderedVideos)
        }

        val validSelectedCat = selectedCatId?.takeIf { id -> shelves.any { it.playlist.playlistId == id } }

        HomeUiState(
            playlists = shelves.map { it.playlist },
            shelves = shelves,
            allVideos = bundle.allVideos,
            continueWatching = bundle.continueWatching,
            selectedCategoryId = validSelectedCat,
            isSyncing = syncing,
            syncBannerMessage = bannerMsg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            val prefs = repository.settingsFlow.first { it.isLoaded }
            if (prefs.onboardingCompleted) {
                refreshCatalogNow()
            }
        }
    }

    fun completeParentOnboarding(
        pin: String,
        familySpaceName: String,
        selectedStarterChannelIds: Set<String>
    ) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.completeOnboarding(pin, familySpaceName, selectedStarterChannelIds)
            _isSyncing.value = false
        }
    }

    fun refreshCatalogNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncBannerMessage.value = null
            when (val res = repository.syncCatalog()) {
                is SyncResult.Success -> _syncBannerMessage.value =
                    if (res.videoCount > 0) "Synced ${res.playlistCount} shelves & ${res.videoCount} videos." else null
                is SyncResult.Error -> _syncBannerMessage.value = res.message
            }
            _isSyncing.value = false
        }
    }

    fun selectSection(section: NavSection) {
        if (section.requiresParentPin && !_isParentUnlocked.value) {
            _pinError.value = null
            _pendingPinTarget.value = section
            return
        }
        // Auto-relock parent session whenever returning to family viewing sections
        if (!section.requiresParentPin) {
            _isParentUnlocked.value = false
        }
        _pendingPinTarget.value = null
        _activeSection.value = section
        _selectedChannelId.value = null
        _selectedPlaylistDetailId.value = null
    }

    fun submitParentPin(candidatePin: String) {
        viewModelScope.launch {
            if (repository.verifyPin(candidatePin)) {
                _isParentUnlocked.value = true
                _pinError.value = null
                val target = _pendingPinTarget.value ?: NavSection.PARENT_CURATE
                _pendingPinTarget.value = null
                _activeSection.value = target
            } else {
                _pinError.value = "Incorrect 4-digit Parent PIN. Please try again."
            }
        }
    }

    fun cancelPinPrompt() {
        _pendingPinTarget.value = null
        _pinError.value = null
    }

    fun lockParentSessionNow() {
        _isParentUnlocked.value = false
        _activeSection.value = NavSection.HOME
    }

    fun updateParentPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            viewModelScope.launch {
                repository.changeParentPin(newPin)
                _syncBannerMessage.value = "Parent PIN updated!"
            }
        }
    }

    fun selectCategory(playlistId: String?) {
        _selectedCategoryId.value = playlistId
    }

    fun openChannelDetail(channelId: String) {
        _selectedChannelId.value = channelId
        _activeSection.value = NavSection.CHANNELS
    }

    fun closeChannelDetail() {
        _selectedChannelId.value = null
    }

    fun openPlaylistDetail(playlistId: String) {
        _selectedPlaylistDetailId.value = playlistId
        _activeSection.value = NavSection.LIBRARY
    }

    fun closePlaylistDetail() {
        _selectedPlaylistDetailId.value = null
    }

    fun selectLibraryTab(tab: LibraryTab) {
        _activeLibraryTab.value = tab
        _selectedPlaylistDetailId.value = null
    }

    fun updateSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun appendSearchChar(ch: String) {
        if (_searchQuery.value.length < 40) _searchQuery.value += ch
    }

    fun backspaceSearch() {
        if (_searchQuery.value.isNotEmpty()) _searchQuery.value = _searchQuery.value.dropLast(1)
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun startPlayback(video: VideoCardModel, queue: List<VideoCardModel>, contextTitle: String) {
        _isParentUnlocked.value = false
        val ids = queue.map { it.videoId }.distinct().ifEmpty { listOf(video.videoId) }
        val finalIds = if (video.videoId in ids) ids else listOf(video.videoId) + ids
        _playbackSession.value = PlaybackSession(
            currentVideoId = video.videoId,
            contextTitle = contextTitle,
            queueVideoIds = finalIds
        )
    }

    fun playVideoInCurrentQueue(videoId: String) {
        val current = _playbackSession.value ?: return
        _playbackSession.value = current.copy(currentVideoId = videoId)
    }

    fun closePlayer() {
        _playbackSession.value = null
    }

    fun saveWatchProgress(videoId: String, currentSeconds: Float, durationSeconds: Float) {
        viewModelScope.launch { repository.updateWatchProgress(videoId, currentSeconds, durationSeconds) }
    }

    fun markVideoFinished(videoId: String, durationSeconds: Float) {
        viewModelScope.launch { repository.markVideoCompleted(videoId, durationSeconds) }
    }

    fun toggleLike(videoId: String) {
        viewModelScope.launch { repository.toggleLike(videoId) }
    }

    fun toggleWatchLater(videoId: String) {
        viewModelScope.launch { repository.toggleWatchLater(videoId) }
    }

    fun handleUnplayableVideo(videoId: String) {
        viewModelScope.launch { repository.markVideoBlockedAtRuntime(videoId) }
    }

    fun setAutoplay(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoplayEnabled(enabled) }
    }

    fun clearWatchHistory() {
        viewModelScope.launch { repository.clearWatchHistory() }
    }

    fun restoreHiddenVideos() {
        viewModelScope.launch { repository.restoreHiddenVideos() }
    }

    private data class CatalogBundle(
        val playlists: List<PlaylistEntity>,
        val allVideos: List<VideoCardModel>,
        val crossRefs: List<PlaylistVideoCrossRef>,
        val continueWatching: List<VideoCardModel>
    )

    companion object {
        fun factory(repository: CatalogRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
    }
}
