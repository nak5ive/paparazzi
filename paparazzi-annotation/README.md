# `@Paparazzi`
An annotation used to generate Paparazzi snapshots for composable preview functions.

## Basic Usage
Apply the annotation alongside an existing preview method. The annotation processor will generate a manifest of information about this method and the previews applied.

```kotlin
@Paparazzi
@Preview
@Composable
fun MyViewPreview() {
  MyView(title = "Hello, Paparazzi Annotation")
}
```

From a test suite of your choice, call the `Paparazzi.snapshot(paparazziAnnotations)` extension method to generate snapshots for all previews

```kotlin
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.annotation.paparazziAnnotations
import app.cash.paparazzi.annotation.snapshot
import org.junit.Rule
import org.junit.Test

class AnnotationTest {
  @get:Rule
  val paparazzi = Paparazzi()

  @Test
  fun annotations() {
    paparazzi.snapshot(paparazziAnnotations)
  }
}
```

## Preview Parameter
If your preview function accepts a parameter using `@PreviewParameter`, then snapshots will be created for each combination of preview / param.

```kotlin
@Paparazzi
@Preview
@Composable
fun MyViewPreview(@PreviewParameter(MyTitleProvider::class) title: String) {
  MyView(title = title)
}

class MyTitleProvider : PreviewParameterProvider<String> {
  override val values = sequenceOf("Hello", "Paparazzi", "Annotation")
}
```

## Composable Wrapping
If you need to apply additional UI treatment around your previews, you may provide a composable wrapper within the test.

```kotlin
paparazzi.snapshot(paparazziAnnotations) { content ->
  Box(modifier = Modifier.background(Color.Gray)) {
    content()
  }
}
```

## Preview Composition
If you have multiple preview annotations applied to a function, or have them nested behind a custom annotation, they will all be included in the snapshot manifest.

```kotlin
@Paparazzi
@ScaledThemedPreviews
@Composable
fun MyViewPreview() {
  MyView(title = "Hello, Paparazzi Annotation")
}

@Preview(name = "small light", fontScale = 1f, uiMode = Configuration.UI_MODE_NIGHT_NO, device = PIXEL_3_XL)
@Preview(name = "small dark", fontScale = 1f, uiMode = Configuration.UI_MODE_NIGHT_YES, device = PIXEL_3_XL)
@Preview(name = "large light", fontScale = 2f, uiMode = Configuration.UI_MODE_NIGHT_NO, device = PIXEL_3_XL)
@Preview(name = "large dark", fontScale = 2f, uiMode = Configuration.UI_MODE_NIGHT_YES, device = PIXEL_3_XL)
annotation class ScaledThemedPreviews
```

## Sample
See [the sample](../sample/src/main/java/app/cash/paparazzi/sample) for working implementations
