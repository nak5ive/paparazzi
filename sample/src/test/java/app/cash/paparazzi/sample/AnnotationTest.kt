package app.cash.paparazzi.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.annotation.paparazziPreviewAnnotations
import app.cash.paparazzi.annotation.paparazziTestAnnotations
import app.cash.paparazzi.annotation.snapshot
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class AnnotationTest {

  @get:Rule val paparazzi = Paparazzi()

  @Test
  fun `preview annotations`() {
    paparazzi.snapshot(paparazziPreviewAnnotations)
  }

  @Test
  fun `test annotations`() {
    paparazzi.snapshot(paparazziTestAnnotations) { content ->
      Box(
        modifier = Modifier
          .background(Color.Magenta)
          .padding(24.dp)
      ) {
        content()
      }
    }
  }
}
