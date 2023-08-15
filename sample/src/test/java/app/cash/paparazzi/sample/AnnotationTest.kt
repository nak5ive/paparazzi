package app.cash.paparazzi.sample

import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.annotation.paparazziPreviewAnnotations
import app.cash.paparazzi.annotation.paparazziTestAnnotations
import app.cash.paparazzi.annotation.snapshot
import org.junit.Rule
import org.junit.Test

class AnnotationTest {

  @get:Rule val paparazzi = Paparazzi()

  @Test
  fun `preview annotations`() {
    paparazzi.snapshot(paparazziPreviewAnnotations)
  }

  @Test
  fun `test annotations`() {
    paparazzi.snapshot(paparazziTestAnnotations)
  }
}
