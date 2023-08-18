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
    val previews: List<PreviewData> = emptyList(),
    val previewParameter: PreviewParameterData? = null,
  )

  data class PreviewData(
    val name: String? = null,
    val group: String? = null,
    val fontScale: Float? = null,
    val uiMode: Int? = null,
  )

  data class PreviewParameterData(
    val name: String,
    val provider: PreviewParameterProvider<out Any>,
  )
""".trimIndent()

val snapshotFileDefinition = """
  val paparazziAnnotations = $PREVIEW_ANNOTATIONS_VALUE_NAME + $TEST_ANNOTATIONS_VALUE_NAME

  fun Paparazzi.snapshot(
    annotations: List<PaparazziAnnotationData>,
    wrapper: (@Composable (@Composable () -> Unit) -> Unit) = { it() },
  ) {
    annotations.flatMap { data ->
      val previews = if (data.previews.isEmpty()) listOf<PreviewData?>(null) else data.previews
      previews.map { data to it }
    }.forEach { (data, preview) ->
      // TODO use preview param args
      if (data.previewParameter != null) {
        data.previewParameter!!.provider.values.forEachIndexed { i, value ->
          snapshot("${'$'}{data.functionName}[${'$'}{data.previewParameter!!.name}${'$'}i]") {
            wrapper { data.composable(value) }
          }
        }
      } else {
        snapshot(data.functionName) {
          wrapper { data.composable(null) }
        }
      }
    }
  }
""".trimIndent()
