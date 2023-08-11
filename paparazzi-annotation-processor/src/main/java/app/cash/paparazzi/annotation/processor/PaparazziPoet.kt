package app.cash.paparazzi.annotation.processor

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec

object PaparazziPoet {

  fun buildFile(projectInfo: ProjectInfo, functions: Sequence<KSFunctionDeclaration>): FileSpec {
    val fileName = "paparazziManifest${if (projectInfo.isTest) "Test" else ""}"
    return FileSpec.scriptBuilder(fileName, "app.cash.paparazzi.annotation")
      .addStatement("val $fileName = %S", functions.toManifest())
      .build()
  }

  private fun Sequence<KSFunctionDeclaration>.toManifest(): String {
    val stringBuilder = StringBuilder()

    forEach {
      val javaClassName = it.javaClassName()
      val functionName = it.simpleName.asString()

      val previewParam = it.previewParam()
      val paramType = previewParam?.type?.resolve()?.declaration?.qualifiedName?.asString() ?: ""
      val paramProvider =
        previewParam?.previewParamProvider()?.declaration?.qualifiedName?.asString() ?: ""

      val line = listOf(javaClassName, functionName, paramType, paramProvider).joinToString(",")

      stringBuilder.appendLine(line)
    }

    return stringBuilder.toString()
  }

  private fun CodeBlock.Builder.addFunctions(functions: Sequence<KSFunctionDeclaration>) = apply {
    functions.forEach {
      val javaClassName = it.javaClassName()
      val functionName = it.simpleName.asString()

      val previewParam = it.previewParam()
      val paramType = previewParam?.type?.resolve()?.declaration?.qualifiedName?.asString() ?: ""
      val paramProvider = previewParam?.previewParamProvider()?.declaration?.qualifiedName?.asString() ?: ""

      val line = listOf(javaClassName, functionName, paramType, paramProvider).joinToString(",")

      addStatement(line)
    }
  }

  private fun KSFunctionDeclaration.javaClassName(): String {
    return containingFile?.let { "${it.packageName.asString()}.${it.fileName.replace(".kt", "Kt")}" } ?: ""
  }

  private fun KSFunctionDeclaration.previewParam() = parameters.firstOrNull { param ->
    param.annotations.any { it.shortName.asString() == "PreviewParameter" }
  }

  private fun KSValueParameter.previewParamProvider() = annotations
    .first { it.shortName.asString() == "PreviewParameter" }
    .arguments
    .first { arg -> arg.name?.asString() == "provider" }
    .let { it.value as KSType }
}
