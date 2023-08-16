package app.cash.paparazzi.annotation.processor

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.buildCodeBlock

object PaparazziPoet {

  fun buildPreviewFiles(functions: Sequence<KSFunctionDeclaration>) =
    listOf(
      buildDataClassFile(),
      buildAnnotationsFile(functions, PREVIEW_ANNOTATIONS_FILE_NAME, PREVIEW_ANNOTATIONS_VALUE_NAME),
    )

  fun buildTestFiles(functions: Sequence<KSFunctionDeclaration>) =
    listOf(
      buildAnnotationsFile(functions, TEST_ANNOTATIONS_FILE_NAME, TEST_ANNOTATIONS_VALUE_NAME),
      buildSnapshotFile(),
    )

  private fun buildDataClassFile() =
    FileSpec.scriptBuilder(DATA_CLASS_FILE_NAME, PACKAGE_NAME)
      .addImport("androidx.compose.runtime", "Composable")
      .addImport("androidx.compose.ui.tooling.preview", "PreviewParameterProvider")
      .addCode(dataClassFileDefinition)
      .build()

  private fun buildSnapshotFile() =
    FileSpec.scriptBuilder(SNAPSHOT_FILE_NAME, PACKAGE_NAME)
      .addImport("androidx.compose.runtime", "Composable")
      .addImport("app.cash.paparazzi", "Paparazzi")
      .addCode(snapshotFileDefinition)
      .build()

  private fun buildAnnotationsFile(functions: Sequence<KSFunctionDeclaration>, fileName: String, valueName: String) =
    FileSpec.scriptBuilder(fileName, PACKAGE_NAME)
      .addCode(
        buildCodeBlock {
          addStatement("val $valueName = listOf<$DATA_CLASS_FILE_NAME>(")
          indent()

          functions.forEach { func ->
            addStatement("$DATA_CLASS_FILE_NAME(")
            indent()

            val functionClassName = ClassName(func.packageName.asString(), func.simpleName.asString())
            addStatement("packageName = %S,", functionClassName.packageName)
            addStatement("name = %S,", functionClassName.simpleName)

            val preview = func.preview()
            if (preview != null) {
              addStatement("previewName = %S,", preview.previewArg("name", ""))
            }

            val previewParam = func.previewParam()
            if (previewParam != null) {
              addStatement("composable = { %T(it as %T) },", functionClassName, previewParam.previewParamTypeClassName())
              addStatement("previewParameterName = %S,", previewParam.name?.asString())
              addStatement("previewParameterProvider = %T(),", previewParam.previewParamProviderClassName())
            } else {
              addStatement("composable = { %T() },", functionClassName)
            }

            unindent()
            addStatement("),")
          }

          unindent()
          addStatement(")")
        }
      )
      .build()

  private fun KSFunctionDeclaration.previewParam() = parameters.firstOrNull { param ->
    param.annotations.any { it.shortName.asString() == "PreviewParameter" }
  }

  private fun KSFunctionDeclaration.preview() = annotations
    .firstOrNull { it.qualifiedName() == "androidx.compose.ui.tooling.preview.Preview" }

  private fun <T> KSAnnotation.previewArg(name: String, default: T): T = arguments
    .firstOrNull { it.name?.asString() == name }
    ?.let { it.value as T } ?: default

  private fun KSValueParameter.previewParamProviderClassName() = annotations
    .first { it.shortName.asString() == "PreviewParameter" }
    .arguments
    .first { arg -> arg.name?.asString() == "provider" }
    .let { it.value as KSType }
    .declaration.qualifiedName?.let {
    ClassName(it.getQualifier(), it.getShortName())
  }

  private fun KSValueParameter.previewParamTypeClassName() = type.resolve().declaration.qualifiedName?.let {
    ClassName(it.getQualifier(), it.getShortName())
  }
}
