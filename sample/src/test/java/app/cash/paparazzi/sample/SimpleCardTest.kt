package app.cash.paparazzi.sample

import androidx.compose.runtime.Composable
import app.cash.paparazzi.annotation.api.Paparazzi

@Paparazzi
@Composable
fun SimpleCardTest() {
  val model = SimpleCardModel(
    title = "Hello World",
    desc = "This is a simple card test"
  )

  SimpleCard(model = model)
}
