package org.solidcoder.trackchecker.routes.domain.model

@JvmInline
value class DetectionRadius(val meters: Double) {
    init {
        require(meters.isFinite() && meters > 0.0) { "Detection radius must be positive and finite" }
    }

    companion object {
        val DEFAULT = DetectionRadius(50.0)
    }
}
