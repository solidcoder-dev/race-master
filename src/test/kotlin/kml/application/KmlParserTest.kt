package kml.application

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.picolobruno.racing.kml.application.KmlParser

class KmlParserTest {

 private lateinit var kmlString: String

 @BeforeEach
 fun setUp() {
  // Read your KML file from the resources folder
  val kmlPath = Paths.get("src/test/resources/ET 1 WP NOMAD 2024.kml")
  kmlString = Files.readString(kmlPath)
 }

 @Test
 fun `should parse document name`() {
  val raceRoute = KmlParser.parse(kmlString)
  assertEquals("ET 1 WP NOMAD 2024.kml", raceRoute.name)
 }

 @Test
 fun `should parse all expected placemarks`() {
  // GIVEN: we have a map of expected placemark names and their coords
  val expectedCoordinates = mapOf(
   "CP1" to Pair(-5.0600646, 33.383061),
   "META_ET1" to Pair(-5.0413897, 32.919376),
   "SALIDA ET1" to Pair(-5.2438827, 33.776443),
   "WP01" to Pair(-5.2348927, 33.775121),
   "WP02" to Pair(-5.2118297, 33.754056),
   "WP03" to Pair(-5.2006958, 33.668613),
   // ... continue for each named placemark you want to verify ...
   "WP50" to Pair(-5.0514758, 32.930327),
   "WPE8" to Pair(-4.9544033, 33.208879),
   "WPE7" to Pair(-5.0941451, 33.617761),
   // And so on...
   "CP2" to Pair(-5.0436874, 33.028366)
  )

  // WHEN: we parse the KML
  val raceRoute = KmlParser.parse(kmlString)

  // THEN: assert that *all* expected placemarks exist with correct coords
  for ((placemarkName, coords) in expectedCoordinates) {
   val actualPlacemark = raceRoute.placemarks.find { it.name == placemarkName }
   assertNotNull(actualPlacemark, "Placemark '$placemarkName' should be present in the KML")

   actualPlacemark?.let {
    assertEquals(coords.first, it.longitude, 1e-6,
     "Longitude for '$placemarkName' should match")
    assertEquals(coords.second, it.latitude, 1e-6,
     "Latitude for '$placemarkName' should match")
   }
  }

  // Optionally, check that the total number of parsed placemarks
  // matches exactly the ones we expect (if you want a 1:1 match).
  // assertEquals(expectedCoordinates.size, raceRoute.placemarks.size)
 }

}
