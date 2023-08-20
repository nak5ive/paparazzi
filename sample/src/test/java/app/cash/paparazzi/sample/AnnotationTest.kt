package app.cash.paparazzi.sample

import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.annotation.paparazziAnnotations
import app.cash.paparazzi.annotation.snapshot
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class AnnotationTest {

  @get:Rule val paparazzi = Paparazzi()

  @Test
  fun annotations() {
    paparazzi.snapshot(paparazziAnnotations)
  }
}
