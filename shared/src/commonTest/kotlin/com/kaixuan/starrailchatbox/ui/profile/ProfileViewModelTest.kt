package com.kaixuan.starrailchatbox.ui.profile

import com.kaixuan.starrailchatbox.data.database.DatabaseManager
import com.kaixuan.starrailchatbox.data.database.InMemoryDatabaseManager
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.data.settings.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Test
    fun initialStateIsCorrect() = runTest {
        val viewModel = createViewModel(scope = this)
        runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.customAvatarUri)
        assertEquals(20, state.summaryThreshold)
        assertFalse(state.saveMultimodalToken)
        assertFalse(state.enableWebSearch)
        assertFalse(state.isSaving)
        assertTrue(state.isLoaded)
    }

    @Test
    fun storedProfileIsLoaded() = runTest {
        val store = FakeProfileStore(
            UserProfile(
                customAvatarUri = "file:///test/avatar.png",
                summaryThreshold = 30,
                saveMultimodalToken = true,
                enableWebSearch = true
            )
        )
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("file:///test/avatar.png", state.customAvatarUri)
        assertEquals(30, state.summaryThreshold)
        assertTrue(state.saveMultimodalToken)
        assertTrue(state.enableWebSearch)
    }

    @Test
    fun avatarChangeTriggersRealTimeSave() = runTest {
        val store = FakeProfileStore()
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.AvatarChanged("data:image/png;base64,AQID", "new_avatar.png", "png"))
        
        // 使用轮询等待，确保异步的 saveProfile 执行完并调用 store.save
        var attempts = 0
        while (store.saved == null && attempts < 100) {
            attempts++
            kotlinx.coroutines.delay(20)
        }

        val savedUri = requireNotNull(store.saved?.customAvatarUri)
        val expectedDir = com.kaixuan.starrailchatbox.platform.KmpFileManager.Default.appDataDir.toString()
        assertTrue(savedUri.startsWith(expectedDir), "Saved URI should start with $expectedDir, but was $savedUri")
        assertTrue(savedUri.contains("user_avatar_"), "Saved URI should contain 'user_avatar_', but was $savedUri")
        assertTrue(savedUri.endsWith(".png"), "Saved URI should end with '.png', but was $savedUri")
    }

    @Test
    fun thresholdChangeTriggersRealTimeSave() = runTest {
        val store = FakeProfileStore()
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.SummaryThresholdChanged(50))
        advanceUntilIdle()

        assertEquals(50, store.saved?.summaryThreshold)
    }

    @Test
    fun multimodalTokenChangeTriggersRealTimeSave() = runTest {
        val store = FakeProfileStore()
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.SaveMultimodalTokenChanged(true))
        advanceUntilIdle()

        assertTrue(store.saved?.saveMultimodalToken == true)
    }

    @Test
    fun webSearchChangeTriggersRealTimeSave() = runTest {
        val store = FakeProfileStore()
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.EnableWebSearchChanged(true))
        advanceUntilIdle()

        assertTrue(store.saved?.enableWebSearch == true)
    }

    @Test
    fun restoreDefaultAvatarClearsCustomAvatarAndSaves() = runTest {
        val store = FakeProfileStore(UserProfile(customAvatarUri = "file:///test/avatar.png"))
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.RestoreDefaultAvatar)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.customAvatarUri)
        assertNull(store.saved?.customAvatarUri)
    }

    private fun createViewModel(
        store: ProfileStore = FakeProfileStore(),
        scope: kotlinx.coroutines.CoroutineScope,
        databaseManager: DatabaseManager = InMemoryDatabaseManager()
    ) = ProfileViewModel(
        profileStore = store,
        databaseManager = databaseManager,
        coroutineScope = scope
    )
}

private class FakeProfileStore(
    initial: UserProfile? = null
) : ProfileStore {
    private val _profile = MutableStateFlow(initial)
    override val profile: Flow<UserProfile?> = _profile

    var saved: UserProfile? = null

    override suspend fun load(): UserProfile? = _profile.value

    override suspend fun save(profile: UserProfile) {
        saved = profile
        _profile.value = profile
    }
}
