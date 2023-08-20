package app.cash.paparazzi.annotation.processor

const val PACKAGE_NAME = "app.cash.paparazzi.annotation"
const val ANNOTATION_QUALIFIED_NAME = "app.cash.paparazzi.annotation.api.Paparazzi"
const val METADATA_FILE_NAME = "metadata"
const val SNAPSHOT_FILE_NAME = "snapshot"
const val UTILS_FILE_NAME = "utils"
const val PREVIEW_ANNOTATIONS_FILE_NAME = "previewAnnotations"
const val PREVIEW_ANNOTATIONS_VALUE_NAME = "paparazziPreviewAnnotations"
const val TEST_ANNOTATIONS_FILE_NAME = "testAnnotations"
const val TEST_ANNOTATIONS_VALUE_NAME = "paparazziTestAnnotations"

val metadataFileDefinition = """
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.tooling.preview.PreviewParameterProvider

  data class PaparazziAnnotationData(
    val packageName: String,
    val functionName: String,
    val composable: (@Composable (Any?) -> Unit),
    val previews: List<PreviewData> = emptyList(),
    val previewParameter: PreviewParameterData? = null,
  )

  data class PreviewData(
    val name: String? = null,
    val fontScale: Float? = null,
    val device: String? = null,
    val uiMode: Int? = null,
    val locale: String? = null,
  )

  data class PreviewParameterData(
    val name: String,
    val provider: PreviewParameterProvider<out Any>,
  )
""".trimIndent()

val snapshotFileDefinition = """
  import androidx.compose.runtime.Composable
  import app.cash.paparazzi.DeviceConfig
  import app.cash.paparazzi.Paparazzi
  import kotlin.math.absoluteValue

  fun Paparazzi.snapshot(
    annotations: List<PaparazziAnnotationData>,
    wrapper: (@Composable (@Composable () -> Unit) -> Unit) = { it() },
  ) {
    annotations.flatMap { data ->
      val previews = if (data.previews.isEmpty()) listOf<PreviewData?>(null) else data.previews
      previews.map { data to it }
    }.forEach { pair ->
      val (data, preview) = pair
      unsafeUpdateConfig(preview.deviceConfig())

      data.previewParameter?.let { pp ->
        pp.provider.values.forEachIndexed { i, value ->
          val paramName = "${'$'}{data.previewParameter!!.name}${'$'}i"
          snapshot(pair.snapshotName(paramName)) {
            wrapper { data.composable(value) }
          }
        }
      } ?: snapshot(pair.snapshotName()) { wrapper { data.composable(null) } }
    }
  }

  private fun PreviewData?.deviceConfig() = this?.device.let { deviceConfigForId(it) }
    .let { config ->
      this?.fontScale?.let { config.copy(fontScale = it) } ?: config
    }
    .let { config ->
      this?.uiMode?.let {
        config.copy(
          uiMode = uiModeForPreview(it),
          nightMode = nightModeForPreview(it),
        )
      } ?: config
    }
    .let { config ->
      this?.locale?.let { config.copy(locale = it) } ?: config
    }

  private fun Pair<PaparazziAnnotationData, PreviewData?>.snapshotName(paramName: String? = null): String {
    val (data, preview) = this

    return buildList<String> {
      add(data.functionName)
      preview?.name?.let(::add)

      buildList<String> {
        preview?.fontScale?.let { add("scale${'$'}it") }
        paramName?.let(::add)
      }.takeIf { it.isNotEmpty() }
        ?.joinToString(",", "[", "]")
        ?.let(::add)

      val dataHash = data.copy(previews = emptyList()).hashCode()
      val previewHash = preview.hashCode()
      val paramHash = paramName.hashCode()
      (dataHash xor previewHash xor paramHash).toString(16).padStart(8, '0').let(::add)
    }.joinToString("_")
  }
""".trimIndent()

val utilsFileDefinition = """
  import android.content.res.Configuration
  import app.cash.paparazzi.DeviceConfig
  import com.android.resources.NightMode
  import com.android.resources.UiMode

  val paparazziAnnotations = paparazziPreviewAnnotations + paparazziTestAnnotations

  internal fun deviceConfigForId(id: String?) = when (id) {
    "id:Nexus 7" -> DeviceConfig.NEXUS_7
    "id:Nexus 7 2013" -> DeviceConfig.NEXUS_7_2012
    "id:Nexus 5" -> DeviceConfig.NEXUS_5
    "id:Nexus 6" -> DeviceConfig.NEXUS_7
    "id:Nexus 9" -> DeviceConfig.NEXUS_10
    "name:Nexus 10" -> DeviceConfig.NEXUS_10
    "id:Nexus 5X" -> DeviceConfig.NEXUS_5
    "id:Nexus 6P" -> DeviceConfig.NEXUS_7
    "id:pixel_c" -> DeviceConfig.PIXEL_C
    "id:pixel" -> DeviceConfig.PIXEL
    "id:pixel_xl" -> DeviceConfig.PIXEL_XL
    "id:pixel_2" -> DeviceConfig.PIXEL_2
    "id:pixel_2_xl" -> DeviceConfig.PIXEL_2_XL
    "id:pixel_3" -> DeviceConfig.PIXEL_3
    "id:pixel_3_xl" -> DeviceConfig.PIXEL_3_XL
    "id:pixel_3a" -> DeviceConfig.PIXEL_3A
    "id:pixel_3a_xl" -> DeviceConfig.PIXEL_3A_XL
    "id:pixel_4" -> DeviceConfig.PIXEL_4
    "id:pixel_4_xl" -> DeviceConfig.PIXEL_4_XL
    "id:wearos_small_round" -> DeviceConfig.WEAR_OS_SMALL_ROUND
    "id:wearos_square" -> DeviceConfig.WEAR_OS_SQUARE
    else -> DeviceConfig.NEXUS_5
  }

  internal fun uiModeForPreview(mode: Int) = when(mode and Configuration.UI_MODE_TYPE_MASK) {
    Configuration.UI_MODE_TYPE_CAR -> UiMode.CAR
    Configuration.UI_MODE_TYPE_DESK -> UiMode.DESK
    Configuration.UI_MODE_TYPE_APPLIANCE -> UiMode.APPLIANCE
    Configuration.UI_MODE_TYPE_WATCH -> UiMode.WATCH
    Configuration.UI_MODE_TYPE_VR_HEADSET -> UiMode.VR_HEADSET
    else -> UiMode.NORMAL
  }

  internal fun nightModeForPreview(mode: Int) = when(mode and Configuration.UI_MODE_NIGHT_MASK) {
    Configuration.UI_MODE_NIGHT_YES -> NightMode.NIGHT
    else -> NightMode.NOTNIGHT
  }
""".trimIndent()
