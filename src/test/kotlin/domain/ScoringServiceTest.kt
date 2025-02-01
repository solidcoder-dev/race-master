package domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

// TODO: use instantio
class ScoringServiceTest {

 private val scoringService = ScoringService()

 @Test
 fun `calculateScore returns total for waypoints within tolerance`() {
  // Arrange
  val track = Track(
   coordinates = listOf(
    Coordinate(latitude = 38.0, longitude = -120.0),
    Coordinate(latitude = 38.001, longitude = -120.0005)
   )
  )

  val waypoints = listOf(
   Waypoint(
    name = "WP1",
    coordinate = Coordinate(latitude = 38.0, longitude = -120.0),
    score = Score(10)
   ),
   Waypoint(
    name = "WP2",
    coordinate = Coordinate(latitude = 40.0, longitude = -122.0),
    score = Score(20)
   )
  )

  val raceRoute = RaceRoute(
   start = Coordinate(latitude = 38.0, longitude = -120.0),
   end = Coordinate(latitude = 40.0, longitude = -122.0),
   waypoints = waypoints,
   checkpoints = emptyList()
  )

  // Tolerance of ~2 km should allow WP1 to be hit, but not WP2
  val tolerance = Tolerance(2.0)

  // Act
  val resultScore = scoringService.calculateScore(track, raceRoute, tolerance)

  // Assert
  assertEquals(10, resultScore.points, "Only WP1 should be within tolerance => total = 10.")
 }

 @Test
 fun `calculateScore returns zero when no waypoints are within tolerance`() {
  // Arrange
  val track = Track(
   coordinates = listOf(
    Coordinate(latitude = 0.0, longitude = 0.0)
   )
  )

  val waypoints = listOf(
   Waypoint(
    name = "WP1",
    coordinate = Coordinate(latitude = 10.0, longitude = 10.0),
    score = Score(50)
   ),
   Waypoint(
    name = "WP2",
    coordinate = Coordinate(latitude = -5.0, longitude = 110.0),
    score = Score(50)
   )
  )
  val raceRoute = RaceRoute(
   start = Coordinate(latitude = 0.0, longitude = 0.0),
   end = Coordinate(latitude = 10.0, longitude = 10.0),
   waypoints = waypoints,
   checkpoints = emptyList()
  )
  val tolerance = Tolerance(distanceInKm = 1.0)  // 1 km

  // Act
  val resultScore = scoringService.calculateScore(track, raceRoute, tolerance)

  // Assert
  assertEquals(0, resultScore.points, "No waypoints are within 1 km => total = 0.")
 }

 @Test
 fun `calculateScore accumulates multiple hit waypoints`() {
  // Arrange
  val track = Track(
   coordinates = listOf(
    // Coordinates near WP1 and WP2
    Coordinate(latitude = 38.000, longitude = -120.000),
    Coordinate(latitude = 38.002, longitude = -120.002)
   )
  )

  val waypoints = listOf(
   Waypoint(
    name = "WP1",
    coordinate = Coordinate(latitude = 38.0, longitude = -120.0),
    score = Score(10)
   ),
   Waypoint(
    name = "WP2",
    coordinate = Coordinate(latitude = 38.002, longitude = -120.002),
    score = Score(20)
   ),
   Waypoint(
    name = "WP3",
    coordinate = Coordinate(latitude = 40.0, longitude = -122.0),
    score = Score(30)
   )
  )

  val raceRoute = RaceRoute(
   start = Coordinate(latitude = 38.0, longitude = -120.0),
   end = Coordinate(latitude = 40.0, longitude = -122.0),
   waypoints = waypoints,
   checkpoints = emptyList()
  )
  val tolerance = Tolerance(0.5) // ~0.5 km radius

  // Act
  val resultScore = scoringService.calculateScore(track, raceRoute, tolerance)

  // WP1 and WP2 are close enough, WP3 is far away
  assertEquals(30, resultScore.points, "WP1(10) + WP2(20) = 30 total.")
 }

 @Test
 fun `calculateScore handles edge-case tolerance`() {
  // Suppose the distance from the track to WP1 is exactly 1.000 km
  val track = Track(
   coordinates = listOf(
    Coordinate(latitude = 38.0, longitude = -120.0),
   )
  )
  val waypoints = listOf(
   Waypoint(
    name = "WP1",
    coordinate = Coordinate(latitude = 38.008993, longitude = -120.0),
    score = Score(10)
   )
  )
  val raceRoute = RaceRoute(
   start = Coordinate(latitude = 38.0, longitude = -120.0),
   end = Coordinate(latitude = 40.0, longitude = -122.0),
   waypoints = waypoints,
   checkpoints = emptyList()
  )
  // If our distance is ~1.0, let's test a tolerance of exactly 1.0
  val tolerance = Tolerance(1.0)

  // Act
  val resultScore = scoringService.calculateScore(track, raceRoute, tolerance)

  // Assert (with floating point, we allow a small margin)
  // If the distance is exactly 1.0, WP1 should be counted
  assertTrue(abs(resultScore.points - 10) < 0.0001, "WP1 should be within 1 km => Score 10.")
 }
}
