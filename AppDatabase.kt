package ro.optometrist57.app.data

import android.content.Context
import androidx.room.*

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients WHERE name = :name AND phone = :phone LIMIT 1")
    suspend fun find(name: String, phone: String): PatientEntity?
    @Insert suspend fun insert(patient: PatientEntity): Long
    @Update suspend fun update(patient: PatientEntity)
}

@Dao
interface ConsultationDao {
    @Query("SELECT COALESCE(MAX(receiptNumber), 0) + 1 FROM consultations")
    suspend fun nextReceiptNumber(): Int
    @Insert suspend fun insert(item: ConsultationEntity): Long
    @Transaction
    @Query("SELECT * FROM consultations ORDER BY dateTime DESC")
    suspend fun allWithPatients(): List<ConsultationWithPatient>
    @Transaction
    @Query("SELECT * FROM consultations WHERE id = :id")
    suspend fun getWithPatient(id: Long): ConsultationWithPatient?
}

@Database(entities = [PatientEntity::class, ConsultationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patients(): PatientDao
    abstract fun consultations(): ConsultationDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "optometrist57.db").build().also { INSTANCE = it }
        }
    }
}
