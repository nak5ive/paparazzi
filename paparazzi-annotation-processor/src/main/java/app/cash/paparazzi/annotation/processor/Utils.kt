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
  val paparazziAnnotations = $PREVIEW_ANNOTATIONS_VALUE_NAME + $TEST_ANNOTATIONS_VALUE_NAME

  fun Paparazzi.snapshot(
    annotations: List<PaparazziAnnotationData>,
    wrapper: (@Composable (@Composable () -> Unit) -> Unit) = { it() },
  ) {
    annotations.flatMap { data ->
      val previews = if (data.previews.isEmpty()) listOf<PreviewData?>(null) else data.previews
      previews.map { data to it }
    }.forEach { (data, preview) ->
      var deviceConfig = preview?.device.let { deviceConfigForId(it ?: "") }
      preview?.fontScale?.let {
        deviceConfig = deviceConfig.copy(fontScale = it)
      }
      preview?.uiMode?.let {
        deviceConfig = deviceConfig.copy(
          uiMode = uiModeForPreview(it),
          nightMode = nightModeForPreview(it),
        )
      }
      preview?.locale?.let {
        deviceConfig = deviceConfig.copy(locale = it)
      }

      unsafeUpdateConfig(deviceConfig)

      val name = buildList {
        (
          data.functionName +
          if (data.previews.size > 1) {
            data.previews.indexOf(preview)
          } else ""
        ).let(::add)

        preview?.name?.let { add(it) }
        preview?.fontScale?.let { add("scale${'$'}it") }
      }.joinToString("_")

      if (data.previewParameter != null) {
        data.previewParameter!!.provider.values.forEachIndexed { i, value ->
          snapshot("${'$'}name[${'$'}{data.previewParameter!!.name}${'$'}i]") {
            wrapper { data.composable(value) }
          }
        }
      } else {
        snapshot(name) {
          wrapper { data.composable(null) }
        }
      }
    }
  }
""".trimIndent()

val utilsFileDefinition = """
  fun deviceConfigForId(id: String) = when (id) {
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

  fun uiModeForPreview(mode: Int) = when(mode and Configuration.UI_MODE_TYPE_MASK) {
    Configuration.UI_MODE_TYPE_CAR -> UiMode.CAR
    Configuration.UI_MODE_TYPE_DESK -> UiMode.DESK
    Configuration.UI_MODE_TYPE_APPLIANCE -> UiMode.APPLIANCE
    Configuration.UI_MODE_TYPE_WATCH -> UiMode.WATCH
    Configuration.UI_MODE_TYPE_VR_HEADSET -> UiMode.VR_HEADSET
    else -> UiMode.NORMAL
  }

  fun nightModeForPreview(mode: Int) = when(mode and Configuration.UI_MODE_NIGHT_MASK) {
    Configuration.UI_MODE_NIGHT_YES -> NightMode.NIGHT
    else -> NightMode.NOTNIGHT
  }
""".trimIndent()
