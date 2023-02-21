# `@Paparazzi`
An annotation to generate Paparazzi tests for composable functions.

## Installation
The annotation processor is build using the [Kotlin Symbol Processing API](https://kotlinlang.org/docs/ksp-overview.html). It's required that you include the KSP plugin in your app.
```groovy
plugins {
  id('com.google.devtools.ksp') version '1.7.20-1.0.8'
}
```

Add the following dependencies to your app:
```groovy
implementation 'app.cash.paparazzi:paparazzi-annotation:1.2.0'
ksp 'app.cash.paparazzi:paparazzi-annotation-processor:1.2.0'
```

Lastly, the processor needs to know where to put your generated test files. Add the directory path to the KSP args. Be sure to also include that path in your test source sets.
```groovy
ksp {
  arg("paparazziOutputDir", "$projectDir/build/generated/source/paparazzi")
}

android {
  sourceSets {
    test.java.srcDir 'build/generated/source/paparazzi'
  }
}
```

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
