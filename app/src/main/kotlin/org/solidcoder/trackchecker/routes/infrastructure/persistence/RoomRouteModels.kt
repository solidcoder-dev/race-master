package org.solidcoder.trackchecker.routes.infrastructure.persistence

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "routes")
data class RouteRecord(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val importedAtEpochMs: Long
)

@Entity(
    tableName = "route_points",
    foreignKeys = [ForeignKey(
        entity = RouteRecord::class,
        parentColumns = ["id"],
        childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routeId")]
)
data class RoutePointRecord(
    @PrimaryKey val id: String,
    val routeId: String,
    val sequence: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: String
)

data class RouteWithPoints(
    @Embedded val route: RouteRecord,
    @Relation(parentColumn = "id", entityColumn = "routeId")
    val points: List<RoutePointRecord>
)
