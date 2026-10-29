package org.solidcoder.trackchecker.tracks.infrastructure.persistence

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import org.solidcoder.trackchecker.routes.infrastructure.persistence.RouteRecord

@Entity(
    tableName = "tracks",
    foreignKeys = [ForeignKey(
        entity = RouteRecord::class, parentColumns = ["id"], childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routeId")]
)
data class TrackRecord(
    @PrimaryKey val id: String,
    val routeId: String,
    val name: String,
    val importedAtEpochMs: Long
)

@Entity(
    tableName = "track_samples",
    primaryKeys = ["trackId", "segmentIndex", "sampleIndex"],
    foreignKeys = [ForeignKey(
        entity = TrackRecord::class,
        parentColumns = ["id"],
        childColumns = ["trackId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trackId")]
)
data class TrackSampleRecord(
    val trackId: String,
    val segmentIndex: Int,
    val sampleIndex: Int,
    val latitude: Double,
    val longitude: Double,
    val timestampEpochMs: Long?
)

data class TrackWithSamples(
    @Embedded val track: TrackRecord,
    @Relation(parentColumn = "id", entityColumn = "trackId")
    val samples: List<TrackSampleRecord>
)
