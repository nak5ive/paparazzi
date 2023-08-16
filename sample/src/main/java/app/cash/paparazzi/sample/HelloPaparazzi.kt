package app.cash.paparazzi.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.cash.paparazzi.annotation.api.Paparazzi

@Composable
fun HelloPaparazzi(
  text: String = "Hello, Paparazzi",
) {
  Column(
    Modifier
      .background(Color.White)
      .fillMaxSize()
      .wrapContentSize()
  ) {
    Text(text)
    Text(text, style = TextStyle(fontFamily = FontFamily.Cursive))
    Text(
      text = text,
      style = TextStyle(textDecoration = TextDecoration.LineThrough)
    )
    Text(
      text = text,
      style = TextStyle(textDecoration = TextDecoration.Underline)
    )
    Text(
      text = text,
      style = TextStyle(
        textDecoration = TextDecoration.combine(
          listOf(
            TextDecoration.Underline,
            TextDecoration.LineThrough
          )
        ),
        fontWeight = FontWeight.Bold
      )
    )
  }
}

@Paparazzi
@Preview(name = "basic preview")
@Composable
fun HelloPaparazziPreview() {
  HelloPaparazzi(
    text = "Hello, Paparazzi Preview",
  )
}

@Paparazzi
@Preview
@Composable
internal fun HelloPaparazziPreview(@PreviewParameter(TextProvider::class) text: String) {
  HelloPaparazzi(
    text = text,
  )
}

class TextProvider : PreviewParameterProvider<String> {
  override val values = sequenceOf(
    "Hello, Provided Preview Text",
    "Nice to meet you, Paparazzi",
  )
}
