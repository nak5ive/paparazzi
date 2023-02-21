package app.cash.paparazzi.annotation.processor.models

data class PaparazziModel(
  val functionName: String,
  val packageName: String,
  val previewParam: PreviewParamModel?
)
