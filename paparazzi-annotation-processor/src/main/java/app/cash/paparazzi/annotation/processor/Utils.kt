package app.cash.paparazzi.annotation.processor

const val PACKAGE_NAME = "app.cash.paparazzi.annotation"
const val ANNOTATION_QUALIFIED_NAME = "app.cash.paparazzi.annotation.api.Paparazzi"
const val DATA_CLASS_FILE_NAME = "PaparazziAnnotationData"
const val SNAPSHOT_FILE_NAME = "snapshot"
const val PREVIEW_ANNOTATIONS_FILE_NAME = "previewAnnotations"
const val PREVIEW_ANNOTATIONS_VALUE_NAME = "paparazziPreviewAnnotations"
const val TEST_ANNOTATIONS_FILE_NAME = "testAnnotations"
const val TEST_ANNOTATIONS_VALUE_NAME = "paparazziTestAnnotations"

val dataClassFileDefinition = """
  data class $DATA_CLASS_FILE_NAME(
    val packageName: String,
    val name: String,
    val composable: (@%T (Any?) -> Unit),
    val previewParameterName: String? = null,
    val previewParameterProvider: %T<out Any>? = null,
  )
""".trimIndent()

val snapshotFileDefinition = """
  val paparazziAnnotations = $PREVIEW_ANNOTATIONS_VALUE_NAME + $TEST_ANNOTATIONS_VALUE_NAME

  fun %T.snapshot(annotations: List<$DATA_CLASS_FILE_NAME>) {
    annotations.forEach {
      if (it.previewParameterProvider != null) {
        it.previewParameterProvider!!.values.forEachIndexed { i, value ->
          snapshot("${'$'}{it.name}[${'$'}{it.previewParameterName}${'$'}i]") {
            it.composable.invoke(value)
          }
        }
      } else {
        snapshot(it.name) {
          it.composable.invoke(null)
        }
      }
    }
  }
""".trimIndent()
