package com.kaixuan.starrailchatbox.ui.profile

import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.data.settings.UserProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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
        assertEquals("星空旅人", state.nickname)
        assertNull(state.customAvatarUri)
        assertFalse(state.isSaving)
        assertTrue(state.isLoaded)
    }

    @Test
    fun storedProfileIsLoaded() = runTest {
        val store = FakeProfileStore(
            UserProfile(
                nickname = "三月七",
                customAvatarUri = "file:///test/avatar.png"
            )
        )
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("三月七", state.nickname)
        assertEquals("file:///test/avatar.png", state.customAvatarUri)
    }

    @Test
    fun nicknameChangeUpdatesState() = runTest {
        val viewModel = createViewModel(scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.NicknameChanged("开拓者"))
        assertEquals("开拓者", viewModel.uiState.value.nickname)
    }

    @Test
    fun avatarChangeUpdatesState() = runTest {
        val viewModel = createViewModel(scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.AvatarChanged("file:///test/avatar.png"))
        assertEquals("file:///test/avatar.png", viewModel.uiState.value.customAvatarUri)
    }

    @Test
    fun restoreDefaultAvatarClearsCustomAvatar() = runTest {
        val store = FakeProfileStore(UserProfile("星空旅人", "file:///test/avatar.png"))
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.RestoreDefaultAvatar)
        assertNull(viewModel.uiState.value.customAvatarUri)
    }

    @Test
    fun saveProfilePersistsDataAndEmitsSavedEffect() = runTest {
        val store = FakeProfileStore()
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.NicknameChanged(" 丹恒 "))
        viewModel.onAction(ProfileAction.AvatarChanged("file:///test/avatar.png"))
        
        val effect = async { viewModel.effects.first { it is ProfileEffect.ProfileSaved } }
        
        viewModel.onAction(ProfileAction.SaveClicked)
        advanceUntilIdle()

        assertEquals(
            UserProfile(
                nickname = "丹恒",
                customAvatarUri = "file:///test/avatar.png"
            ),
            store.saved
        )
        assertEquals(ProfileEffect.ProfileSaved, effect.await())
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun saveEmptyNicknameEmitsErrorEffect() = runTest {
        val store = FakeProfileStore()
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.NicknameChanged("   "))
        
        val effects = async { viewModel.effects.take(1).toList() }
        
        viewModel.onAction(ProfileAction.SaveClicked)
        advanceUntilIdle()

        assertEquals(
            listOf(ProfileEffect.ShowMessage(ProfileEffectMessage.NICKNAME_EMPTY)),
            effects.await()
        )
        assertNull(store.saved)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun backWithChangesShowsDiscardDialog() = runTest {
        val viewModel = createViewModel(scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.NicknameChanged("新昵称"))
        viewModel.onAction(ProfileAction.BackClicked)

        assertTrue(viewModel.uiState.value.isDiscardDialogOpen)
    }

    @Test
    fun backWithoutChangesEmitsSavedEffect() = runTest {
        val viewModel = createViewModel(scope = this)
        runCurrent()

        val effect = async { viewModel.effects.first { it is ProfileEffect.ProfileSaved } }
        viewModel.onAction(ProfileAction.BackClicked)

        assertEquals(ProfileEffect.ProfileSaved, effect.await())
    }

    @Test
    fun confirmDiscardRestoresOriginalAndEmitsSavedEffect() = runTest {
        val store = FakeProfileStore(UserProfile("原本的", "file:///old.png"))
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        viewModel.onAction(ProfileAction.NicknameChanged("改变后的"))
        viewModel.onAction(ProfileAction.AvatarChanged(null))
        viewModel.onAction(ProfileAction.BackClicked)
        
        val effect = async { viewModel.effects.first { it is ProfileEffect.ProfileSaved } }
        viewModel.onAction(ProfileAction.ConfirmDiscard)

        assertEquals(ProfileEffect.ProfileSaved, effect.await())
        val state = viewModel.uiState.value
        assertEquals("原本的", state.nickname)
        assertEquals("file:///old.png", state.customAvatarUri)
        assertFalse(state.isDiscardDialogOpen)
    }

    private fun createViewModel(
        store: ProfileStore = FakeProfileStore(),
        scope: kotlinx.coroutines.CoroutineScope
    ) = ProfileViewModel(
        profileStore = store,
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
