package app.cash.paparazzi.annotation.processor

const val PACKAGE_NAME = "app.cash.paparazzi.annotation"
const val ANNOTATION_QUALIFIED_NAME = "app.cash.paparazzi.annotation.api.Paparazzi"
const val METADATA_FILE_NAME = "metadata"
const val SNAPSHOT_FILE_NAME = "snapshot"
const val PREVIEW_ANNOTATIONS_FILE_NAME = "previewAnnotations"
const val PREVIEW_ANNOTATIONS_VALUE_NAME = "paparazziPreviewAnnotations"
const val TEST_ANNOTATIONS_FILE_NAME = "testAnnotations"
const val TEST_ANNOTATIONS_VALUE_NAME = "paparazziTestAnnotations"

val metadataFileDefinition = """
  data class PaparazziAnnotationData(
    val packageName: String,
    val functionName: String,
    val composable: (@Composable (Any?) -> Unit),
    val previewParameter: PreviewParameterData? = null,
    val previews: List<PreviewData> = emptyList(),
  )

  data class PreviewParameterData(
    val name: String,
    val provider: PreviewParameterProvider<out Any>,
  )

  data class PreviewData(
    val name: String? = null,
    val fontScale: Float? = null,
  )
""".trimIndent()

val snapshotFileDefinition = """
  val paparazziAnnotations = $PREVIEW_ANNOTATIONS_VALUE_NAME + $TEST_ANNOTATIONS_VALUE_NAME

  fun Paparazzi.snapshot(
    annotations: List<PaparazziAnnotationData>,
    wrapper: (@Composable (@Composable () -> Unit) -> Unit)? = null,
  ) {
    annotations.forEach { data ->
      if (data.previews.size > 0) {
        data.previews.forEach { preview ->
          val name = "${'$'}{data.functionName}${'$'}{if (preview.name != null) "-${'$'}{preview.name}" else ""}"
          if (data.previewParameter != null) {
            data.previewParameter!!.provider.values.forEachIndexed { i, value ->
              snapshot("${'$'}name[${'$'}{data.previewParameter!!.name}${'$'}i]") {
                wrapper?.let { it { data.composable(value) } } ?: data.composable(value)
              }
            }
          } else {
            snapshot(name) {
              wrapper?.let { it { data.composable(null) } } ?: data.composable(null)
            }
          }
        }
      } else {
        snapshot(data.functionName) {
          wrapper?.let { it { data.composable(null) } } ?: data.composable(null)
        }
      }
    }
  }
""".trimIndent()
