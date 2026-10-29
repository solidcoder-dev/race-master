package org.solidcoder.trackchecker.scoring.domain.model

import org.solidcoder.trackchecker.routes.domain.model.RoutePointId

data class PointContribution(val pointId: RoutePointId, val points: Int)

data class Score(val total: Int, val contributions: List<PointContribution>) {
    init { require(total == contributions.sumOf { it.points }) }
}
