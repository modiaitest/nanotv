package app.nanotv.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.nanotv.tv.ui.MainViewModel
import app.nanotv.tv.ui.NavSection
import app.nanotv.tv.ui.components.SidebarNavRail
import app.nanotv.tv.ui.player.NanoTvPlayerScreen
import app.nanotv.tv.ui.screens.AddVideosQrScreen
import app.nanotv.tv.ui.screens.ChannelsScreen
import app.nanotv.tv.ui.screens.HomeScreen
import app.nanotv.tv.ui.screens.LibraryScreen
import app.nanotv.tv.ui.screens.ParentOnboardingScreen
import app.nanotv.tv.ui.screens.ParentPinUnlockModal
import app.nanotv.tv.ui.screens.SearchScreen
import app.nanotv.tv.ui.screens.SettingsScreen
import app.nanotv.tv.ui.theme.NanoPalette
import app.nanotv.tv.ui.theme.NanoTvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as NanoTvApp

        setContent {
            NanoTvTheme {
                val vm: MainViewModel = viewModel(
                    factory = MainViewModel.factory(app.repository)
                )
                NanoTvRootApp(vm = vm)
            }
        }
    }
}

@Composable
fun NanoTvRootApp(vm: MainViewModel) {
    val settingsState by vm.settingsState.collectAsStateWithLifecycle()
    val activeSection by vm.activeSection.collectAsStateWithLifecycle()
    val isParentUnlocked by vm.isParentUnlocked.collectAsStateWithLifecycle()
    val pendingPinTarget by vm.pendingPinTarget.collectAsStateWithLifecycle()
    val pinError by vm.pinError.collectAsStateWithLifecycle()
    val homeUiState by vm.homeUiState.collectAsStateWithLifecycle()
    val curatedSources by vm.curatedSourcesState.collectAsStateWithLifecycle()
    val channels by vm.channelsState.collectAsStateWithLifecycle()
    val selectedChannelId by vm.selectedChannelId.collectAsStateWithLifecycle()
    val selectedChannelVideos by vm.selectedChannelVideos.collectAsStateWithLifecycle()
    val activeLibraryTab by vm.activeLibraryTab.collectAsStateWithLifecycle()
    val selectedPlaylistId by vm.selectedPlaylistDetailId.collectAsStateWithLifecycle()
    val selectedPlaylistVideos by vm.selectedPlaylistVideos.collectAsStateWithLifecycle()
    val historyVideos by vm.watchHistoryState.collectAsStateWithLifecycle()
    val likedVideos by vm.likedVideosState.collectAsStateWithLifecycle()
    val watchLaterVideos by vm.watchLaterState.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val searchResults by vm.searchResultsState.collectAsStateWithLifecycle()
    val playbackSession by vm.playbackSession.collectAsStateWithLifecycle()

    if (!settingsState.isLoaded) {
        Box(modifier = Modifier.fillMaxSize().background(NanoPalette.MidnightBg))
        return
    }

    // First-Launch Parent Onboarding & Mandatory 4-Digit PIN Setup
    if (!settingsState.onboardingCompleted) {
        ParentOnboardingScreen(
            onComplete = vm::completeParentOnboarding
        )
        return
    }

    BackHandler(enabled = playbackSession == null && pendingPinTarget == null && activeSection != NavSection.HOME) {
        vm.selectSection(NavSection.HOME)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NanoPalette.MidnightBg)
    ) {
        when {
            playbackSession != null -> {
                NanoTvPlayerScreen(
                    session = playbackSession!!,
                    allVideos = homeUiState.allVideos,
                    autoplayEnabled = settingsState.autoplayEnabled,
                    onSelectQueueVideo = vm::playVideoInCurrentQueue,
                    onToggleLike = vm::toggleLike,
                    onToggleWatchLater = vm::toggleWatchLater,
                    onSaveProgress = vm::saveWatchProgress,
                    onMarkFinished = vm::markVideoFinished,
                    onUnplayableVideo = vm::handleUnplayableVideo,
                    onExitPlayer = vm::closePlayer
                )
            }

            pendingPinTarget != null -> {
                ParentPinUnlockModal(
                    errorMessage = pinError,
                    onSubmitPin = vm::submitParentPin,
                    onCancel = vm::cancelPinPrompt
                )
            }

            else -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    SidebarNavRail(
                        familySpaceName = settingsState.familyProfileName,
                        isParentUnlocked = isParentUnlocked,
                        activeSection = activeSection,
                        onSelectSection = vm::selectSection
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        when (activeSection) {
                            NavSection.HOME -> HomeScreen(
                                uiState = homeUiState,
                                onSelectCategory = vm::selectCategory,
                                onPlayVideo = vm::startPlayback,
                                onOpenParentCurate = { vm.selectSection(NavSection.PARENT_CURATE) }
                            )

                            NavSection.SEARCH -> SearchScreen(
                                query = searchQuery,
                                results = searchResults,
                                onQueryChange = vm::updateSearchQuery,
                                onAppendChar = vm::appendSearchChar,
                                onBackspace = vm::backspaceSearch,
                                onClear = vm::clearSearch,
                                onPlayVideo = vm::startPlayback
                            )

                            NavSection.CHANNELS -> ChannelsScreen(
                                channels = channels,
                                selectedChannelId = selectedChannelId,
                                selectedChannelVideos = selectedChannelVideos,
                                onSelectChannel = vm::openChannelDetail,
                                onCloseChannelDetail = vm::closeChannelDetail,
                                onPlayVideo = vm::startPlayback
                            )

                            NavSection.LIBRARY -> LibraryScreen(
                                activeTab = activeLibraryTab,
                                playlists = homeUiState.playlists,
                                selectedPlaylistId = selectedPlaylistId,
                                selectedPlaylistVideos = selectedPlaylistVideos,
                                historyVideos = historyVideos,
                                likedVideos = likedVideos,
                                watchLaterVideos = watchLaterVideos,
                                onSelectTab = vm::selectLibraryTab,
                                onOpenPlaylist = vm::openPlaylistDetail,
                                onClosePlaylist = vm::closePlaylistDetail,
                                onPlayVideo = vm::startPlayback
                            )

                            NavSection.PARENT_CURATE -> AddVideosQrScreen(
                                curatedSources = curatedSources,
                                totalVideos = homeUiState.allVideos.size,
                                isSyncing = homeUiState.isSyncing,
                                onRefreshNow = vm::refreshCatalogNow,
                                onLockParentSession = vm::lockParentSessionNow
                            )

                            NavSection.PARENT_SETTINGS -> SettingsScreen(
                                settings = settingsState,
                                isSyncing = homeUiState.isSyncing,
                                playlistCount = homeUiState.playlists.size,
                                videoCount = homeUiState.allVideos.size,
                                syncMessage = homeUiState.syncBannerMessage,
                                onOpenQrCurate = { vm.selectSection(NavSection.PARENT_CURATE) },
                                onRefreshNow = vm::refreshCatalogNow,
                                onToggleAutoplay = vm::setAutoplay,
                                onClearHistory = vm::clearWatchHistory,
                                onRestoreHidden = vm::restoreHiddenVideos,
                                onChangePin = vm::updateParentPin,
                                onLockParentSession = vm::lockParentSessionNow
                            )
                        }
                    }
                }
            }
        }
    }
}
