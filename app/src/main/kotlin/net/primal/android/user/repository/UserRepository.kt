package net.primal.android.user.repository

import javax.inject.Inject
import kotlinx.coroutines.withContext
import net.primal.android.core.coroutines.CoroutineDispatcherProvider
import net.primal.android.core.files.FileUploader
import net.primal.android.core.files.error.UnsuccessfulFileUpload
import net.primal.android.core.serialization.json.NostrJson
import net.primal.android.core.serialization.json.decodeFromStringOrNull
import net.primal.android.core.utils.authorNameUiFriendly
import net.primal.android.core.utils.usernameUiFriendly
import net.primal.android.db.PrimalDatabase
import net.primal.android.networking.sockets.errors.WssException
import net.primal.android.nostr.ext.asProfileDataPO
import net.primal.android.nostr.model.content.ContentMetadata
import net.primal.android.profile.domain.ProfileMetadata
import net.primal.android.user.accounts.UserAccountFetcher
import net.primal.android.user.accounts.UserAccountsStore
import net.primal.android.user.accounts.copyContactsIfNotNull
import net.primal.android.user.accounts.copyIfNotNull
import net.primal.android.user.api.UsersApi
import net.primal.android.user.domain.NostrWalletConnect
import net.primal.android.user.domain.UserAccount
import net.primal.android.user.domain.WalletPreference

class UserRepository @Inject constructor(
    private val dispatcherProvider: CoroutineDispatcherProvider,
    private val database: PrimalDatabase,
    private val userAccountFetcher: UserAccountFetcher,
    private val accountsStore: UserAccountsStore,
    private val fileUploader: FileUploader,
    private val usersApi: UsersApi,
) {

    suspend fun createNewUserAccount(userId: String): UserAccount {
        val account = UserAccount.buildLocal(pubkey = userId)
        accountsStore.upsertAccount(account)
        return account
    }

    suspend fun fetchAndUpdateUserAccount(userId: String): UserAccount {
        val userProfile = userAccountFetcher.fetchUserProfileOrNull(userId = userId)
        val userStats = userProfile?.takeIf {
            it.followersCount != null && it.followingCount != null && it.notesCount != null
        }
        val userContacts = userAccountFetcher.fetchUserContactsOrNull(userId = userId)
        return accountsStore.getAndUpdateAccount(userId = userId) {
            copyIfNotNull(
                profile = userProfile,
                stats = userStats,
                contacts = userContacts,
            )
        }
    }

    suspend fun updateContacts(userId: String, contactsUserAccount: UserAccount) {
        accountsStore.getAndUpdateAccount(userId = userId) {
            copyContactsIfNotNull(contacts = contactsUserAccount)
                .copy(followingCount = contactsUserAccount.following.size)
        }
    }

    suspend fun connectNostrWallet(userId: String, nostrWalletConnect: NostrWalletConnect) {
        accountsStore.getAndUpdateAccount(userId = userId) {
            copy(nostrWallet = nostrWalletConnect)
        }
    }

    suspend fun disconnectNostrWallet(userId: String) {
        accountsStore.getAndUpdateAccount(userId = userId) {
            copy(nostrWallet = null)
        }
    }

    suspend fun removeAllUserAccounts() {
        accountsStore.clearAllAccounts()
    }

    @Throws(UnsuccessfulFileUpload::class)
    suspend fun setProfileMetadata(userId: String, profileMetadata: ProfileMetadata) {
        val pictureUrl = if (profileMetadata.localPictureUri != null) {
            fileUploader.uploadFile(userId = userId, uri = profileMetadata.localPictureUri)
        } else {
            profileMetadata.remotePictureUrl
        }

        val bannerUrl = if (profileMetadata.localBannerUri != null) {
            fileUploader.uploadFile(userId = userId, uri = profileMetadata.localBannerUri)
        } else {
            profileMetadata.remoteBannerUrl
        }

        withContext(dispatcherProvider.io()) {
            setUserProfileAndUpdateLocally(
                userId = userId,
                contentMetadata = ContentMetadata(
                    displayName = profileMetadata.displayName,
                    name = profileMetadata.username,
                    website = profileMetadata.website,
                    about = profileMetadata.about,
                    lud16 = profileMetadata.lightningAddress,
                    nip05 = profileMetadata.nostrVerification,
                    picture = pictureUrl,
                    banner = bannerUrl,
                ),
            )
        }
    }

    suspend fun setLightningAddress(userId: String, lightningAddress: String) {
        withContext(dispatcherProvider.io()) {
            val userProfileResponse = usersApi.getUserProfile(pubkey = userId)
            val metadata = NostrJson.decodeFromStringOrNull<ContentMetadata>(userProfileResponse.metadata?.content)
                ?: throw WssException("Profile Content Metadata not found.")

            setUserProfileAndUpdateLocally(
                userId = userId,
                contentMetadata = metadata.copy(lud16 = lightningAddress),
            )
        }
    }

    private suspend fun setUserProfileAndUpdateLocally(userId: String, contentMetadata: ContentMetadata) {
        val profileMetadataNostrEvent = usersApi.setUserProfile(ownerId = userId, contentMetadata = contentMetadata)
        val profileData = profileMetadataNostrEvent.asProfileDataPO(cdnResources = emptyMap())
        database.profiles().upsertAll(data = listOf(profileData))

        accountsStore.getAndUpdateAccount(userId = userId) {
            this.copy(
                authorDisplayName = profileData.authorNameUiFriendly(),
                userDisplayName = profileData.usernameUiFriendly(),
                avatarCdnImage = profileData.avatarCdnImage,
                internetIdentifier = profileData.internetIdentifier,
                lightningAddress = profileData.lightningAddress,
            )
        }
    }

    suspend fun updateWalletPreference(userId: String, walletPreference: WalletPreference) {
        accountsStore.getAndUpdateAccount(userId = userId) {
            copy(walletPreference = walletPreference)
        }
    }

    suspend fun updatePrimalWalletBalance(userId: String, balanceInBtc: String) {
        accountsStore.getAndUpdateAccount(userId = userId) {
            copy(primalWalletBalanceInBtc = balanceInBtc)
        }
    }
}
