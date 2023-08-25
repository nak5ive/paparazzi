package app.cash.paparazzi.sample

import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.annotation.paparazziAnnotations
import app.cash.paparazzi.annotation.snapshot
import org.junit.Rule
import org.junit.Test

class HelloComposeTest {
  @get:Rule
  val paparazzi = Paparazzi()

  @Test
  fun compose() {
    paparazzi.snapshot { HelloPaparazzi() }
  }

  @Test
  fun annotations() {
    paparazzi.snapshot(paparazziAnnotations)
  }
}
