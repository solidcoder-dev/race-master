package domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.picolobruno.racing.domain.Score

class ScoreTest {

 @Test
 fun `Score with non-negative points is valid`() {
  val score = Score(10)
  assertEquals(10, score.points)
 }

 @Test
 fun `Score with zero points is valid`() {
  val score = Score(0)
  assertEquals(0, score.points)
 }

 @Test
 fun `Score with negative points throws an exception`() {
  val exception = assertThrows<IllegalArgumentException> {
   Score(-5)
  }
  assertTrue(exception.message?.contains("Score cannot be negative") == true)
 }

 @Test
 fun `plus operator adds two scores together`() {
  // Arrange
  val s1 = Score(10)
  val s2 = Score(5)

  // Act
  val total = s1 + s2

  // Assert
  assertEquals(15, total.points)
 }
}
