package org.solidcoder.trackchecker.scoring.infrastructure.persistence

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RouteRecord
import org.solidcoder.trackchecker.tracks.infrastructure.persistence.TrackRecord

@Entity(
    tableName = "track_evaluations",
    foreignKeys = [
        ForeignKey(entity = RouteRecord::class, parentColumns = ["id"], childColumns = ["routeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TrackRecord::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["routeId", "trackId"], unique = true), Index("trackId")]
)
data class EvaluationRecord(
    @PrimaryKey val id: String,
    val routeId: String,
    val trackId: String,
    val routeFingerprint: String,
    val evaluatedAtEpochMs: Long,
    val validStart: Boolean,
    val validFinish: Boolean,
    val scoreTotal: Int?
)

@Entity(
    tableName = "evaluated_points",
    primaryKeys = ["evaluationId", "pointId"],
    foreignKeys = [ForeignKey(
        entity = EvaluationRecord::class, parentColumns = ["id"], childColumns = ["evaluationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("evaluationId")]
)
data class EvaluatedPointRecord(
    val evaluationId: String,
    val pointId: String,
    val status: String,
    val minimumDistanceMeters: Double,
    val detectionRadiusMeters: Double,
    val scoreContribution: Int?
)

data class EvaluationWithPoints(
    @Embedded val evaluation: EvaluationRecord,
    @Relation(parentColumn = "id", entityColumn = "evaluationId")
    val points: List<EvaluatedPointRecord>
)
