package ro.optometrist57.app.data

import androidx.room.*

@Entity(tableName = "patients", indices = [Index(value = ["name", "phone"], unique = true)])
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "consultations",
    foreignKeys = [ForeignKey(entity = PatientEntity::class, parentColumns = ["id"], childColumns = ["patientId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("patientId"), Index(value = ["receiptNumber"], unique = true)]
)
data class ConsultationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val receiptNumber: Int,
    val dateTime: Long,
    val location: String,
    val odSph: String = "", val odCyl: String = "", val odAxis: String = "",
    val osSph: String = "", val osCyl: String = "", val osAxis: String = "",
    val dp: String = "", val add: String = "", val vd: String = "", val va: String = "",
    val notes: String = "",
    val originalPhotoPath: String? = null
)

data class ConsultationWithPatient(
    @Embedded val consultation: ConsultationEntity,
    @Relation(parentColumn = "patientId", entityColumn = "id") val patient: PatientEntity
)
