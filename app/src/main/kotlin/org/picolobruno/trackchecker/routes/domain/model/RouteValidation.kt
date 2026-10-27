package org.picolobruno.trackchecker.routes.domain.model

enum class IssueSeverity { ERROR, WARNING }
enum class RouteIssueCode {
    NO_POINTS, MISSING_START, MULTIPLE_START, MISSING_FINISH, MULTIPLE_FINISH, DUPLICATE_NAME
}

data class RouteIssue(
    val code: RouteIssueCode,
    val severity: IssueSeverity,
    val message: String,
    val pointIds: List<RoutePointId> = emptyList()
)

data class RouteValidation(val issues: List<RouteIssue>) {
    val canBeReady: Boolean get() = issues.none { it.severity == IssueSeverity.ERROR }
}
