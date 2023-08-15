package app.cash.paparazzi.sample.compose

import androidx.compose.runtime.Composable
import app.cash.paparazzi.annotation.api.Paparazzi
import app.cash.paparazzi.sample.HelloPaparazzi

@Paparazzi
@Composable
fun HelloPaparazziTest() {
  HelloPaparazzi("I'm just a test")
}
