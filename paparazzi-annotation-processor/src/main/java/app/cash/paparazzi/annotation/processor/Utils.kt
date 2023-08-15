package app.cash.paparazzi.annotation.processor

const val PACKAGE_NAME = "app.cash.paparazzi.annotation"
const val ANNOTATION_QUALIFIED_NAME = "app.cash.paparazzi.annotation.api.Paparazzi"
const val DATA_CLASS_NAME = "PaparazziAnnotationData"

val dataClassDefinition = """
    data class $DATA_CLASS_NAME(
      val name: String,
      val method: (@%T (Any?) -> Unit),
      val previewParameterName: String? = null,
      val previewParameterProvider: %T<out Any>? = null,
    )
  """.trimIndent()

val snapshotDefinition = """
    fun %T.snapshot(annotations: List<$DATA_CLASS_NAME>) {
      annotations.forEach {
        if (it.previewParameterProvider != null) {
          it.previewParameterProvider!!.values.forEachIndexed { i, value ->
            snapshot("${'$'}{it.name}[${'$'}{it.previewParameterName}${'$'}i]") {
              it.method.invoke(value)
            }
          }
        } else {
          snapshot(it.name) {
            it.method.invoke(null)
          }
        }
      }
    }
  """.trimIndent()
