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
    val previewName: String? = null,
    val composable: (@Composable (Any?) -> Unit),
    val previewParameterName: String? = null,
    val previewParameterProvider: PreviewParameterProvider<out Any>? = null,
  )
""".trimIndent()

val snapshotFileDefinition = """
  val paparazziAnnotations = $PREVIEW_ANNOTATIONS_VALUE_NAME + $TEST_ANNOTATIONS_VALUE_NAME

  fun Paparazzi.snapshot(
    annotations: List<$DATA_CLASS_FILE_NAME>,
    wrapper: (@Composable (@Composable () -> Unit) -> Unit)? = null,
  ) {
    annotations.forEach { data ->
      if (data.previewParameterProvider != null) {
        data.previewParameterProvider!!.values.forEachIndexed { i, value ->
          snapshot("${'$'}{data.name}[${'$'}{data.previewParameterName}${'$'}i]") {
            wrapper?.let { it { data.composable(value) } } ?: data.composable(value)
          }
        }
      } else {
        snapshot(data.name) {
          wrapper?.let { it { data.composable(null) } } ?: data.composable(null)
        }
      }
    }
  }
""".trimIndent()
