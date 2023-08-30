package app.cash.paparazzi.sample

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.cash.paparazzi.annotation.api.Paparazzi
import app.cash.paparazzi.sample.theme.PaparazziTheme

@Composable
fun HelloPaparazzi(
  text: String = "Hello, Paparazzi"
) {
  Column(
    Modifier
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
@ScaledThemedPreviews
@Composable
fun HelloPaparazziPreview() {
  PaparazziTheme {
    Surface {
      HelloPaparazzi(
        text = "Hello, Paparazzi Preview"
      )
    }
  }
}

@Paparazzi
@ScaledThemedPreviews
@Composable
fun HelloPaparazziProvided(@PreviewParameter(TextProvider::class) text: String) {
  PaparazziTheme {
    Surface {
      HelloPaparazzi(
        text = text
      )
    }
  }
}

class TextProvider : PreviewParameterProvider<String> {
  override val values = sequenceOf(
    "Hello, Provided Preview Text",
    "Nice to meet you, Paparazzi"
  )
}

@Paparazzi
@Preview(backgroundColor = 0xFFFFFF00, showBackground = true, widthDp = 200, heightDp = 200)
@Preview(backgroundColor = 0xFF00FF00, showBackground = true, widthDp = 200, heightDp = 200)
@Composable
fun HelloPaparazziBackgroundPreview() {
  HelloPaparazzi(
    text = "Hello, Custom Background"
  )
}

@Preview(fontScale = 1f, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(fontScale = 1f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(fontScale = 2f, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(fontScale = 2f, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ScaledThemedPreviews
