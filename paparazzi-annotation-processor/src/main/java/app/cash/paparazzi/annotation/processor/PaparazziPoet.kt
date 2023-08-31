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
            addStatement("functionName = %S,", functionClassName.canonicalName)

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

            val previews = func.findDistinctPreviews()
            if (previews.isNotEmpty()) {
              addStatement("previews = listOf(")
              indent()

              previews.forEach { preview ->
                addStatement("PreviewData(")
                indent()

                preview.fontScale.takeIf { it != 1f }
                  ?.let { addStatement("fontScale = %Lf,", it) }

                preview.device.takeIf { it.isNotEmpty() }
                  ?.let { addStatement("device = %S,", it) }

                preview.widthDp.takeIf { it > -1 }
                  ?.let { addStatement("widthDp = %L,", it) }

                preview.heightDp.takeIf { it > -1 }
                  ?.let { addStatement("heightDp = %L,", it) }

                preview.uiMode.takeIf { it != 0 }
                  ?.let { addStatement("uiMode = %L,", it) }

                preview.locale.takeIf { it.isNotEmpty() }
                  ?.let { addStatement("locale = %S,", it) }

                preview.backgroundColor.takeIf { it != 0L && preview.showBackground }
                  ?.let { addStatement("backgroundColor = %S", it.toString(16)) }

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
