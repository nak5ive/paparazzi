package app.cash.paparazzi.annotation.processor

import com.google.devtools.ksp.symbol.KSType

data class PaparazziModel(
  val javaClassName: String,
  val functionName: String,
  val packageName: String,
  val previewParam: PreviewParamModel?
) {
  fun serialize() = listOf(
    javaClassName,
    functionName,
    previewParam?.type?.declaration?.qualifiedName?.asString() ?: "",
    previewParam?.provider?.declaration?.qualifiedName?.asString() ?: "",
  ).joinToString(",")
}

data class PreviewParamModel(
  val type: KSType,
  val provider: KSType
)
