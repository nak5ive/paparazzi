package app.cash.paparazzi.annotation.processor

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec

object PaparazziPoet {

  fun buildDefaultFiles(functions: Sequence<KSFunctionDeclaration>) =
    listOf(
      buildDataClassFile(),
      buildAnnotationsFile(functions, false),
    )

  fun buildTestFiles(functions: Sequence<KSFunctionDeclaration>) =
    listOf(
      buildSnapshotFile(),
      buildAnnotationsFile(functions, true),
    )

  private fun buildDataClassFile() =
    FileSpec.scriptBuilder(DATA_CLASS_NAME, PACKAGE_NAME)
      .addStatement(
        dataClassDefinition,
        ClassName("androidx.compose.runtime", "Composable"),
        ClassName("androidx.compose.ui.tooling.preview", "PreviewParameterProvider"),
      )
      .build()

  private fun buildAnnotationsFile(functions: Sequence<KSFunctionDeclaration>, isTest: Boolean) =
    FileSpec.scriptBuilder(if (isTest) "testAnnotations" else "annotations", PACKAGE_NAME)
      .addManifest(functions, isTest)
      .apply {
        if (isTest) {
          addStatement("")
          addManifestCombination()
        }
      }
      .build()

  private fun buildSnapshotFile() =
    FileSpec.scriptBuilder("snapshot", PACKAGE_NAME)
      .addStatement(
        snapshotDefinition,
        ClassName("app.cash.paparazzi", "Paparazzi")
      )
      .build()

  private fun FileSpec.Builder.addManifest(functions: Sequence<KSFunctionDeclaration>, isTest: Boolean) =
    addCode(CodeBlock.builder()
      .addStatement("val paparazzi${if (isTest) "Test" else "Preview"}Annotations = listOf<$DATA_CLASS_NAME>(")
      .indent()
      .apply {
        functions.forEach { func ->
          val functionClassName = ClassName(func.packageName.asString(), func.simpleName.asString())
          val previewParam = func.previewParam()
          val paramName = previewParam?.name?.asString()
          val paramType = previewParam?.type?.resolve()?.declaration?.qualifiedName?.let {
            ClassName(it.getQualifier(), it.getShortName())
          }
          val paramProvider = previewParam?.previewParamProvider()?.declaration?.qualifiedName?.let {
            ClassName(it.getQualifier(), it.getShortName())
          }

          addStatement("$DATA_CLASS_NAME(")
          indent()
          addStatement("name = %S,", functionClassName.simpleName)
          if (previewParam != null) {
            addStatement("method = { %T(it as %T) },", functionClassName, paramType)
            addStatement("previewParameterName = %S,", paramName)
            addStatement("previewParameterProvider = %T(),", paramProvider)
          } else {
            addStatement("method = { %T() },", functionClassName)
          }
          unindent()
          addStatement("),")
        }
      }
      .unindent()
      .addStatement(")")
      .build())

  private fun FileSpec.Builder.addManifestCombination() = addStatement("val paparazziAnnotations = paparazziPreviewAnnotations + paparazziTestAnnotations")

  private fun KSFunctionDeclaration.previewParam() = parameters.firstOrNull { param ->
    param.annotations.any { it.shortName.asString() == "PreviewParameter" }
  }

  private fun KSValueParameter.previewParamProvider() = annotations
    .first { it.shortName.asString() == "PreviewParameter" }
    .arguments
    .first { arg -> arg.name?.asString() == "provider" }
    .let { it.value as KSType }
}
