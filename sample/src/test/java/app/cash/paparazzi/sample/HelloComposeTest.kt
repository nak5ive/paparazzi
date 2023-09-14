package app.cash.paparazzi.sample

import androidx.compose.material.Surface
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.preview.paparazziAnnotations
import app.cash.paparazzi.preview.snapshot
import app.cash.paparazzi.sample.theme.PaparazziTheme
import org.junit.Rule
import org.junit.Test

class HelloComposeTest {
  @get:Rule
  val paparazzi = Paparazzi()

  @Test
  fun compose() {
    paparazzi.snapshot {
      PaparazziTheme {
        Surface {
          HelloPaparazzi()
        }
      }
    }
  }

  @Test
  fun annotations() {
    paparazzi.snapshot(
      annotations = paparazziAnnotations,
      defaultDeviceConfig = DeviceConfig.PIXEL_6_PRO
    )
  }
}
