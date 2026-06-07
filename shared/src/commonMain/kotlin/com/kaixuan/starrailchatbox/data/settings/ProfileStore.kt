package com.kaixuan.starrailchatbox.data.settings

data class UserProfile(
    val nickname: String,
    val customAvatarBase64: String? = null
)

interface ProfileStore {
    suspend fun load(): UserProfile?
    suspend fun save(profile: UserProfile)
}

class InMemoryProfileStore(
    private var profile: UserProfile? = UserProfile("星空旅人", null)
) : ProfileStore {
    override suspend fun load(): UserProfile? = profile
    override suspend fun save(profile: UserProfile) {
        this.profile = profile
    }
}

expect fun createProfileStore(path: String? = null): ProfileStore
