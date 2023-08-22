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
      buildAnnotationsFile(functions, "previewAnnotations", "paparazziPreviewAnnotations")
    )

  fun buildTestFiles(functions: Sequence<KSFunctionDeclaration>) =
    listOf(
      buildAnnotationsFile(functions, "testAnnotations", "paparazziTestAnnotations"),
      buildSnapshotFile(),
      buildUtilsFile()
    )

  private fun buildDataClassFile() =
    FileSpec.scriptBuilder("metadata", PACKAGE_NAME)
      .addCode(readResourceFile("files/metadata.txt"))
      .build()

  private fun buildSnapshotFile() =
    FileSpec.scriptBuilder("snapshot", PACKAGE_NAME)
      .addCode(readResourceFile("files/snapshot.txt"))
      .build()

  private fun buildUtilsFile() =
    FileSpec.scriptBuilder("utils", PACKAGE_NAME)
      .addCode(readResourceFile("files/utils.txt"))
      .build()

  private fun buildAnnotationsFile(functions: Sequence<KSFunctionDeclaration>, fileName: String, valueName: String) =
    FileSpec.scriptBuilder(fileName, PACKAGE_NAME)
      .addCode(
        buildCodeBlock {
          addStatement("val $valueName = listOf<PaparazziAnnotationData>(")
          indent()

          functions.forEach { func ->
            addStatement("PaparazziAnnotationData(")
            indent()

            val functionClassName = ClassName(func.packageName.asString(), func.simpleName.asString())
            addStatement("packageName = %S,", functionClassName.packageName)
            addStatement("functionName = %S,", functionClassName.simpleName)

            val previewParam = func.previewParam()
            if (previewParam != null) {
              addStatement("composable = { %T(it as %T) },", functionClassName, previewParam.previewParamTypeClassName())
              addStatement("previewParameter = PreviewParameterData(")
              indent()
              addStatement("name = %S,", previewParam.name?.asString())
              addStatement("provider = %T(),", previewParam.previewParamProviderClassName())
              unindent()
              addStatement("),")
            } else {
              addStatement("composable = { %T() },", functionClassName)
            }

            val previews = func.previews()
            if (previews.isNotEmpty()) {
              addStatement("previews = listOf(")
              indent()

              previews.forEach { preview ->
                addStatement("PreviewData(")
                indent()

                preview.previewArg<String>("name")
                  .takeIf { it.isNotEmpty() }
                  ?.let { addStatement("name = %S,", it) }

                preview.previewArg<Float>("fontScale")
                  .takeIf { it != 1f }
                  ?.let { addStatement("fontScale = %Lf,", it) }

                preview.previewArg<String>("device")
                  .takeIf { it.isNotEmpty() }
                  ?.let { addStatement("device = %S,", it) }

                preview.previewArg<Int>("uiMode")
                  .takeIf { it != 0 }
                  ?.let { addStatement("uiMode = %L,", it) }

                preview.previewArg<String>("locale")
                  .takeIf { it.isNotEmpty() }
                  ?.let { addStatement("locale = %S,", it) }

                unindent()
                addStatement("),")
              }

              unindent()
              addStatement("),")
            }

            unindent()
            addStatement("),")
          }

          unindent()
          addStatement(")")
        }
      )
      .build()

  private fun readResourceFile(fileName: String) = javaClass.classLoader?.getResource(fileName)?.readText() ?: ""

  private fun KSFunctionDeclaration.previewParam() = parameters.firstOrNull { param ->
    param.annotations.any { it.isPreviewParameter() }
  }

  private fun KSFunctionDeclaration.previews() = annotations.findPreviews().toList()

  /**
   * when the same annotations are applied higher in the tree, an endless recursive lookup can occur.
   * using a stack to keep to a record of each symbol lets us break when we hit one we've already encountered
   *
   * ie:
   * @Bottom
   * annotation class Top
   *
   * @Top
   * annotation class Bottom
   *
   * @Bottom
   * fun SomeFun()
   */
  private fun Sequence<KSAnnotation>.findPreviews(stack: Set<KSAnnotation> = setOf()): Sequence<KSAnnotation> {
    val direct = filter { it.isPreview() }
    val indirect = filterNot { it.isPreview() || stack.contains(it) }
      .map { it.parentAnnotations().findPreviews(stack.plus(it)) }
      .flatten()
    return direct.plus(indirect)
  }

  private fun KSAnnotation.isPreview() = qualifiedName() == "androidx.compose.ui.tooling.preview.Preview"
  private fun KSAnnotation.isPreviewParameter() = qualifiedName() == "androidx.compose.ui.tooling.preview.PreviewParameter"

  private fun KSAnnotation.parentAnnotations() = declaration().annotations

  private fun <T> KSAnnotation.previewArg(name: String): T = arguments
    .first { it.name?.asString() == name }
    .let { it.value as T }

  private fun KSValueParameter.previewParamProviderClassName() = annotations
    .first { it.isPreviewParameter() }
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
