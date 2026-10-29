package org.solidcoder.trackchecker.routes.domain.model

/** A score is applied only after the geographical passage has been evaluated. */
data class PointScoreEffect(val whenPassed: Int = 0, val whenMissed: Int = 0)

/** Point-based presets, not the time penalties of any official rally regulations. */
enum class ScoringPattern(val waypointReward: Int, val missedCheckpointPenalty: Int) {
    PASSED_POINTS(10, 0),
    MISSED_PENALTIES(0, 20),
    MIXED(10, 20);

    fun effectFor(type: RoutePointType, pointValueOverride: Int? = null): PointScoreEffect? = when (type) {
        RoutePointType.WAYPOINT -> PointScoreEffect(whenPassed = pointValueOverride ?: waypointReward)
        RoutePointType.CHECKPOINT -> PointScoreEffect(whenMissed = -(pointValueOverride ?: missedCheckpointPenalty))
        // WPE needs an explicit rule; start, finish and unclassified points are not scored.
        RoutePointType.ECLIPSE_WAYPOINT, RoutePointType.START, RoutePointType.FINISH,
        RoutePointType.OTHER -> null
    }
}
