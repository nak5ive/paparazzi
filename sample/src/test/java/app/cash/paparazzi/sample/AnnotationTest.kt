package app.cash.paparazzi.sample

import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.annotation.paparazziAnnotations
import app.cash.paparazzi.annotation.snapshot
import org.junit.Rule
import org.junit.Test

class AnnotationTest {

  @get:Rule val paparazzi = Paparazzi()

  @Test
  fun annotations() {
    paparazzi.snapshot(paparazziAnnotations)
  }
}
