package app.cash.paparazzi.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.reflect.getDeclaredComposableMethod
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.annotation.paparazziManifest
import app.cash.paparazzi.annotation.paparazziManifestTest
import org.junit.Rule
import org.junit.Test

class PreviewAnnotationTest {

  @get:Rule val paparazzi = Paparazzi()

  @Test
  fun `test annotations`() {
    paparazzi.annotationSnapshots()
  }
}

@app.cash.paparazzi.annotation.api.Paparazzi
@Composable
fun HelloPaparazziTest() {
  HelloPaparazzi("I'm just a test")
}

fun Paparazzi.annotationSnapshots() {
  val manifest = Manifest.deserialize(paparazziManifest + paparazziManifestTest)
  manifest.previews.forEach { preview ->
    val clazz = Class.forName(preview.className)

    val argTypes = mutableListOf<String>().apply {
      preview.argType.takeIf { it.isNotEmpty() }?.let(::add)
    }
      .map { it.classForName() }
      .toTypedArray()

    val composableMethod = clazz.getDeclaredComposableMethod(preview.methodName, *argTypes)
    composableMethod.asMethod().isAccessible = true

    if (preview.argProvider.isNotEmpty()) {
      val previewParamProviderClass = Class.forName(preview.argProvider)
      val ppp = previewParamProviderClass.constructors.first()
        .newInstance() as PreviewParameterProvider<*>

      ppp.values.forEachIndexed { i, value ->
        snapshot("${preview.methodName}[$i]") {
          composableMethod.invoke(currentComposer, null, value)
        }
      }
    } else {
      snapshot(preview.methodName) {
        composableMethod.invoke(currentComposer, null)
      }
    }
  }
}

/**
 * `getDeclaredComposableMethod()` requires the java class names associated with any
 * parameters on the composable function. The manifest is rendered via KSP, which resolves
 * primitives by their kotlin namespace (ie: kotlin.String instead of java.lang.String).
 *
 * This method maps kotlin qualified names to their respective java class types to avoid
 * errors thrown when searching for a Class by name.
 */
private fun String.classForName() = when (this) {
  String::class.qualifiedName -> String::class.java
  Double::class.qualifiedName -> Double::class.java
  Float::class.qualifiedName -> Float::class.java
  Long::class.qualifiedName -> Long::class.java
  Int::class.qualifiedName -> Int::class.java
  Short::class.qualifiedName -> Short::class.java
  Byte::class.qualifiedName -> Byte::class.java

  else -> Class.forName(this)
}

data class Manifest(
  val previews: List<PreviewMethod>
) {
  data class PreviewMethod(
    val methodName: String,
    val className: String,
    val argType: String,
    val argProvider: String,
  )

  companion object {
    fun deserialize(rawManifest: String) = rawManifest.lines()
        .filter { it.isNotEmpty() }
        .map {
          it.split(",").let { parts ->
            PreviewMethod(
              className = parts[0],
              methodName = parts[1],
              argType = parts[2],
              argProvider = parts[3],
            )
          }
        }
        .let(::Manifest)
  }
}

