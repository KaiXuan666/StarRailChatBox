package com.kaixuan.starrailchatbox.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val nickname: String,
    val customAvatarUri: String? = null
)

interface ProfileStore {
    val profile: Flow<UserProfile?>
    suspend fun load(): UserProfile?
    suspend fun save(profile: UserProfile)
}

class InMemoryProfileStore(
    initialProfile: UserProfile? = UserProfile("星空旅人", null)
) : ProfileStore {
    private val _profile = MutableStateFlow(initialProfile)
    override val profile: Flow<UserProfile?> = _profile.asStateFlow()

    override suspend fun load(): UserProfile? = _profile.value
    override suspend fun save(profile: UserProfile) {
        this._profile.value = profile
    }
}


expect fun createProfileStore(path: String? = null, context: Any? = null): ProfileStore
