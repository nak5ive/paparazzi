package app.cash.paparazzi.annotation.processor

import app.cash.paparazzi.annotation.processor.models.PaparazziModel
import app.cash.paparazzi.annotation.processor.models.PreviewParamModel
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.visitor.KSEmptyVisitor

class PaparazziVisitor : KSEmptyVisitor<Unit, PaparazziModel?>() {

  override fun defaultHandler(
    node: KSNode,
    data: Unit
  ) = null

  override fun visitFunctionDeclaration(
    function: KSFunctionDeclaration,
    data: Unit
  ): PaparazziModel {
    return PaparazziModel(
      functionName = function.simpleName.asString(),
      packageName = function.packageName.asString(),
      previewParam = function.previewParam()?.let {
        PreviewParamModel(
          type = it.type(),
          provider = it.previewParamProvider()
        )
      }
    )
  }

  private fun KSValueParameter.type() = type.resolve()

  private fun KSFunctionDeclaration.previewParam() = parameters.firstOrNull { param ->
    param.annotations.any { it.shortName.asString() == "PreviewParameter" }
  }

  private fun KSValueParameter.previewParamProvider() = annotations
    .first { it.shortName.asString() == "PreviewParameter" }
    .arguments
    .first { arg -> arg.name?.asString() == "provider" }
    .let { it.value as KSType }
}
