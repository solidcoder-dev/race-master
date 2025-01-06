package infrastructure

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import org.junit.jupiter.api.assertThrows
import org.picolobruno.racing.domain.Score
import org.picolobruno.racing.infrastructure.KmlParser

/**
 * Unit tests for [KmlParser].
 */
class KmlParserTest {

// @Test
// fun `parseTrack with valid LineString returns expected Track`() {
//  // Arrange: Locate a test KML with a <LineString> in src/test/resources
//  val resourceUrl = javaClass.classLoader.getResource("test_track.kml")
//  assertNotNull(resourceUrl, "test_track.kml file not found in test resources.")
//  val testFilePath = Paths.get(resourceUrl!!.toURI()).toFile().absolutePath
//
//  // Act: Parse the track
//  val track = KmlParser.parseTrack(testFilePath)
//
//  // Assert: Check that we got the correct number of coordinates (adjust for your test data)
//  assertFalse(track.coordinates.isEmpty(), "Track should not be empty.")
//  assertEquals(3, track.coordinates.size, "We expect 3 coordinates in the line string.")
//
//  // Optionally verify specific lat/lon if you know them
//  val firstCoord = track.coordinates.first()
//  // For example, if the KML has <coordinates>-120.1,38.5</coordinates>, etc.
//  assertEquals(38.5, firstCoord.latitude, 0.0001)
//  assertEquals(-120.1, firstCoord.longitude, 0.0001)
// }
//
// @Test
// fun `parseTrack with no LineString returns empty Track`() {
//  // Arrange: Suppose test_track_empty.kml has no <LineString> placemark
//  val resourceUrl = javaClass.classLoader.getResource("test_track_empty.kml")
//  assertNotNull(resourceUrl, "test_track_empty.kml file not found.")
//  val testFilePath = Paths.get(resourceUrl!!.toURI()).toFile().absolutePath
//
//  // Act
//  val track = KmlParser.parseTrack(testFilePath)
//
//  // Assert
//  assertTrue(track.coordinates.isEmpty(), "Track should be empty if no <LineString> is present.")
// }

 @Test
 fun `parseRaceRoute with valid file returns proper RaceRoute`() {
  // Arrange
  val resourceUrl = javaClass.classLoader.getResource("test_race_route.kml")
  assertNotNull(resourceUrl, "test_route.kml file not found in test resources.")
  val testFilePath = Paths.get(resourceUrl!!.toURI()).toFile().absolutePath

  // Sample waypoint scores from your domain
  val waypointScores = mapOf(
   "WP1" to Score(10),
   "WP2" to Score(15),
   "WP3" to Score(20)
  )

  // Act
  val raceRoute = KmlParser.parseRaceRoute(testFilePath, waypointScores)

  // Assert
  // 1) We expect a start and end
  assertNotNull(raceRoute.start, "Start should be parsed (SALIDA ET1).")
  assertNotNull(raceRoute.end, "End should be parsed (META ET1).")

  // 2) Check basic waypoint logic
  //    Adjust expected counts for your test data
  assertEquals(3, raceRoute.waypoints.size, "Should parse exactly 3 waypoints.")
  // Maybe check if WP1 got the correct score
  val wp1 = raceRoute.waypoints.find { it.name == "WP1" }
  assertNotNull(wp1)
  assertEquals(10, wp1?.score?.points)

  // 3) Check checkpoints
  //    E.g., if your file has CP1 and CP2
  assertEquals(2, raceRoute.checkpoints.size, "Should parse exactly 2 checkpoints.")
  assertNotNull(raceRoute.checkpoints.find { it.name == "CP1" })

  // 4) Verify the coordinates for start, end, etc., if known
  // Suppose we know the start is (38.1234, -120.5678)
  assertEquals(38.1234, raceRoute.start.latitude, 0.0001)
  assertEquals(-120.5678, raceRoute.start.longitude, 0.0001)
 }

//
// @Test
// fun `parseRaceRoute throws exception when SALIDA ET1 is missing`() {
//  // Arrange: test_route_no_start.kml is missing SALIDA ET1
//  val resourceUrl = javaClass.classLoader.getResource("test_route_no_start.kml")
//  assertNotNull(resourceUrl, "test_route_no_start.kml not found in test resources.")
//  val testFilePath = Paths.get(resourceUrl!!.toURI()).toFile().absolutePath
//
//  // Act & Assert
//  val waypointScores = emptyMap<String, Score>()
//  val exception = assertThrows<IllegalStateException> {
//   KmlParser.parseRaceRoute(testFilePath, waypointScores)
//  }
//  assertTrue(exception.message?.contains("No placemark named 'SALIDA ET1'") == true)
// }
//
// @Test
// fun `parseRaceRoute throws exception when META ET1 is missing`() {
//  // Arrange: test_route_no_end.kml is missing META ET1
//  val resourceUrl = javaClass.classLoader.getResource("test_route_no_end.kml")
//  assertNotNull(resourceUrl, "test_route_no_end.kml not found in test resources.")
//  val testFilePath = Paths.get(resourceUrl!!.toURI()).toFile().absolutePath
//
//  // Act & Assert
//  val waypointScores = emptyMap<String, Score>()
//  val exception = assertThrows<IllegalStateException> {
//   KmlParser.parseRaceRoute(testFilePath, waypointScores)
//  }
//  assertTrue(exception.message?.contains("No placemark named 'META ET1'") == true)
// }
}
