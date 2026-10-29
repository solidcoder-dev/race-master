package org.solidcoder.trackchecker.routes.domain.model

/** Keep distinguishes an untouched field from an explicit reset to route defaults. */
sealed interface OverrideUpdate<out T> {
    data object Keep : OverrideUpdate<Nothing>
    data class Set<T>(val value: T?) : OverrideUpdate<T>
}

data class PointChanges(
    val type: RoutePointType? = null,
    val score: OverrideUpdate<Int> = OverrideUpdate.Keep,
    val radius: OverrideUpdate<DetectionRadius> = OverrideUpdate.Keep
) {
    init {
        require(type != null || score != OverrideUpdate.Keep || radius != OverrideUpdate.Keep) {
            "At least one point property must change"
        }
    }

    fun applyTo(point: RoutePoint): RoutePoint {
        val typed = type?.let(point::changeType) ?: point
        val scored = when (score) {
            OverrideUpdate.Keep -> typed
            is OverrideUpdate.Set -> typed.withScoreValueOverride(score.value)
        }
        return when (radius) {
            OverrideUpdate.Keep -> scored
            is OverrideUpdate.Set -> scored.withDetectionRadiusOverride(radius.value)
        }
    }
}
