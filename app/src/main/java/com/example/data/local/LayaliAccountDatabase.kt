package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "registered_accounts")
data class RegisteredAccountEntity(
    @PrimaryKey val accountId: String,
    val username: String,
    val email: String,
    val displayName: String,
    val role: String = "customer",
    val passwordSaltHex: String,
    val passwordHashHex: String,
    val bcryptPreview: String,
    val bio: String = "",
    val cityName: String = "",
    val country: String = "",
    val avatarUri: String = "",
    val selectedFrameId: String = "",
    val level: Int = 1,
    val xpPoints: Int = 0,
    val nextLevelXp: Int = 0,
    val walletBalanceUsd: Int = 0,
    val chatPriceUsd: Int = 0,
    val isPaidChatEnabled: Boolean = false,
    val updatedAtLabel: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Dao
interface AccountDao {

    @Query("SELECT * FROM registered_accounts ORDER BY updatedAtEpochMs DESC")
    fun observeAllAccounts(): Flow<List<RegisteredAccountEntity>>

    @Query("SELECT * FROM registered_accounts ORDER BY updatedAtEpochMs DESC")
    fun getAllAccountsList(): List<RegisteredAccountEntity>

    @Query(
        """
        SELECT * FROM registered_accounts
        WHERE LOWER(email) = LOWER(:identifier)
           OR LOWER(username) = LOWER(:identifier)
           OR LOWER(username) = LOWER('@' || :identifier)
           OR LOWER(accountId) = LOWER(:identifier)
        LIMIT 1
        """
    )
    fun findByIdentifier(identifier: String): RegisteredAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAccountSync(account: RegisteredAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: RegisteredAccountEntity)

    @Query(
        """
        UPDATE registered_accounts
        SET passwordSaltHex = :saltHex,
            passwordHashHex = :hashHex,
            bcryptPreview = :bcryptPreview,
            updatedAtLabel = :updatedAtLabel,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE accountId = :accountId
        """
    )
    fun updateAccountPasswordSync(
        accountId: String,
        saltHex: String,
        hashHex: String,
        bcryptPreview: String,
        updatedAtLabel: String,
        updatedAtEpochMs: Long
    )

    @Query(
        """
        UPDATE registered_accounts
        SET displayName = :displayName,
            username = :username,
            bio = :bio,
            cityName = :cityName,
            country = :country,
            avatarUri = :avatarUri,
            selectedFrameId = :selectedFrameId,
            level = :level,
            xpPoints = :xpPoints,
            nextLevelXp = :nextLevelXp,
            walletBalanceUsd = :walletBalanceUsd,
            chatPriceUsd = :chatPriceUsd,
            isPaidChatEnabled = :isPaidChatEnabled,
            updatedAtLabel = :updatedAtLabel,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE accountId = :accountId
        """
    )
    suspend fun updateAccountInfrastructure(
        accountId: String,
        displayName: String,
        username: String,
        bio: String,
        cityName: String,
        country: String,
        avatarUri: String,
        selectedFrameId: String,
        level: Int,
        xpPoints: Int,
        nextLevelXp: Int,
        walletBalanceUsd: Int,
        chatPriceUsd: Int,
        isPaidChatEnabled: Boolean,
        updatedAtLabel: String,
        updatedAtEpochMs: Long
    )

    @Query("DELETE FROM registered_accounts WHERE accountId = :accountId")
    suspend fun deleteAccount(accountId: String)
}

@Database(
    entities = [RegisteredAccountEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LayaliAccountDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: LayaliAccountDatabase? = null

        fun getInstance(context: Context): LayaliAccountDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LayaliAccountDatabase::class.java,
                    "layali_accounts_infrastructure.db"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
