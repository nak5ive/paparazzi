package app.cash.paparazzi.annotation.processor

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.buildCodeBlock

object PaparazziPoet {

  fun buildFiles(functions: Sequence<KSFunctionDeclaration>, isTest: Boolean) =
    if (isTest) {
      listOf(
        buildFileFromResource("snapshot"),
        buildFileFromResource("utils"),
        buildAnnotationsFile(functions, "paparazziTestAnnotations")
      )
    } else {
      listOf(
        buildFileFromResource("data"),
        buildAnnotationsFile(functions, "paparazziPreviewAnnotations")
      )
    }

  private fun buildFileFromResource(fileName: String) =
    FileSpec.scriptBuilder(fileName, PACKAGE_NAME)
      .apply {
        javaClass.classLoader
          ?.getResource("files/$fileName.txt")
          ?.readText()
          ?.let(::addCode)
      }
      .build()

  private fun buildAnnotationsFile(functions: Sequence<KSFunctionDeclaration>, name: String) =
    FileSpec.scriptBuilder(name, PACKAGE_NAME)
      .addCode(
        buildCodeBlock {
          addStatement("val $name = listOf<PaparazziAnnotationData>(")
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

            val previews = func.findPreviews()
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
}
