package xyz.donot.detector.model

import android.arch.persistence.room.*
import twitter4j.Status


@Entity(tableName = "status")
class StatusEntity(
        val status: Status,
        @PrimaryKey(autoGenerate = false) val  statusId: Long
)

@Dao
interface StatusDao {
    @Query("SELECT * FROM status WHERE statusId =:statusId LIMIT 1")
    fun findById(statusId: Long):StatusEntity

    @Query("DELETE FROM status")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStatus(status:StatusEntity)
}



