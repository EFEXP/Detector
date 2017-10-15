package xyz.donot.detector.model


import android.arch.persistence.room.*
import android.content.Context
import twitter4j.Status
import twitter4j.Twitter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream




@Entity(tableName = "user")
data class UserEntity(
        val account: Twitter,
        @PrimaryKey(autoGenerate = false) val id: Long
)

@Dao
interface UserRoomDao {
    @Query("SELECT * FROM user WHERE id =:userId LIMIT 1")
    fun findById(userId: Long): UserEntity

    @Query("SELECT * FROM user LIMIT 1")
    fun getMyAccount(): UserEntity

    @Query("DELETE FROM user")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(userEntity: UserEntity)
}

@Database(entities = arrayOf(UserEntity::class,StatusEntity::class), version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userRoomDao(): UserRoomDao
    abstract fun statusDao():StatusDao
    companion object {

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(context.applicationContext,
                        AppDatabase::class.java, "user.db")
                        .build()
    }

}

class Converters {
    companion object {
    @TypeConverter
    @JvmStatic fun serialize(value: Twitter): ByteArray {
        ByteArrayOutputStream().use {
            ObjectOutputStream(it).use {
                it.writeObject(value)
            }
            return it.toByteArray()
        }
    }


    @TypeConverter
    @JvmStatic fun statusSerialize(value: Status): ByteArray {
        ByteArrayOutputStream().use {
            ObjectOutputStream(it).use {
                it.writeObject(value)
            }
            return it.toByteArray()
        }
    }

    @TypeConverter
    @JvmStatic fun statusDeserialize(byteArray: ByteArray): Status {
        @Suppress("UNCHECKED_CAST")
        ByteArrayInputStream(byteArray).use { stream ->
            ObjectInputStream(stream).use {
                return it.readObject() as Status
            }
        }
    }

    @TypeConverter
    @JvmStatic fun deserialize(byteArray: ByteArray): Twitter {
        @Suppress("UNCHECKED_CAST")
        ByteArrayInputStream(byteArray).use { stream ->
            ObjectInputStream(stream).use {
                return it.readObject() as Twitter
            }
        }
    }
    }}