# `@Paparazzi`
An annotation to generate Paparazzi tests for composable functions.

## Basic Usage
In your test directory, define a **no-arg** composable method and apply the annotation. The annotation processor will generate a test class for this composable.

```kotlin
@Paparazzi
@Composable
fun MyViewTest() {
  MyView(title = "Hello, Annotation")
}
```

## Composable Previews
If you're already using preview functions to visualize your composable UI, then you can simply annotate that function to create a test.

```kotlin
@Paparazzi
@Preview
@Composable
fun MyViewPreview() {
  MyView(title = "Hello, Annotation")
}
```

## Test Parameter Injection
If you've applied the annotation to a `@Preview` function that accepts a parameter using `@PreviewParameter`, then the values of that provider will be converted to injected parameters sent through your test.

```kotlin
@Paparazzi
@Preview
@Composable
fun MyViewPreview(@PreviewParameter(MyTitleProvider::class) title: String) {
  MyView(title = title)
}

class MyTitleProvider : PreviewParameterProvider<String> {
  override val values: Sequence<String> = sequenceOf("Hello", "Paparazzi")
}
```

## Sample
See [the sample](../../sample/src/main/java/app/cash/paparazzi/sample) for working implementations
