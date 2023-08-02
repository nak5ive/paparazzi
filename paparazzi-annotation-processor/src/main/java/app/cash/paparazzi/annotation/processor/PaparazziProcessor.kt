package app.cash.paparazzi.annotation.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.FunctionKind.TOP_LEVEL
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.validate
import java.io.File
import java.nio.charset.StandardCharsets

class PaparazziProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment) = PaparazziProcessor(environment.options, environment.logger)
}

class PaparazziProcessor(private val options: Map<String, String>, private val logger: KSPLogger) : SymbolProcessor {

  companion object {
    const val OUTPUT_DIR_OPTION = "paparazziOutputDir"
    const val ANNOTATION_NAME = "app.cash.paparazzi.annotation.api.Paparazzi"
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    return resolver.findPaparazziFunctions()
      .also { it.writeManifest() }
      .filterNot { it.validate() }
      .toList()
  }

  private fun Sequence<KSFunctionDeclaration>.writeManifest() {
    val stringBuilder = StringBuilder()

    forEach { function ->
      PaparazziModel(
        javaClassName = function.javaClassName(),
        functionName = function.simpleName.asString(),
        packageName = function.packageName.asString(),
        previewParam = function.previewParam()?.let { param ->
          PreviewParamModel(
            type = param.type.resolve(),
            provider = param.previewParamProvider()
          )
        }
      )
        .serialize()
        .let(stringBuilder::appendLine)
    }

    val manifest = stringBuilder.toString()
    // logger.info("PAPA - manifest: $manifest")

    val outputDir = outputDirectory()
    outputDir.mkdirs()

    val outputFile = File(outputDir, "annotations.txt")
    outputFile.createNewFile()
    outputFile.writeText(manifest, StandardCharsets.UTF_8)
  }

  private fun Resolver.findPaparazziFunctions() =
    getSymbolsWithAnnotation("androidx.compose.runtime.Composable")
      .filterIsInstance<KSFunctionDeclaration>()
      .filter {
        it.annotations.hasPaparazzi() &&
          it.functionKind == TOP_LEVEL
      }

  private fun KSFunctionDeclaration.javaClassName(): String {
    val jClassName = containingFile?.let { "${it.packageName.asString()}.${it.fileName.replace(".kt", "Kt")}" } ?: ""
    logger.info("PAPA - java class: $jClassName")

    return jClassName
  }

  private fun KSFunctionDeclaration.previewParam() = parameters.firstOrNull { param ->
    param.annotations.any { it.shortName.asString() == "PreviewParameter" }
  }

  private fun KSValueParameter.previewParamProvider() = annotations
    .first { it.shortName.asString() == "PreviewParameter" }
    .arguments
    .first { arg -> arg.name?.asString() == "provider" }
    .let { it.value as KSType }

  // private fun PaparazziModel.writeFile() {
  //   val fileSpec = PaparazziPoet.buildFile(this)
  //
  //   val outputDir = fileSpec.outputDirectory()
  //   outputDir.mkdirs()
  //
  //   val outputFile = File(outputDir, "${fileSpec.name}.kt")
  //   outputFile.createNewFile()
  //
  //   OutputStreamWriter(outputFile.outputStream(), StandardCharsets.UTF_8).use(fileSpec::writeTo)
  // }

  private fun outputDirectory(): File {
    val baseDir = options[OUTPUT_DIR_OPTION] + File.separator
    return File(baseDir)
  }

  private fun Sequence<KSAnnotation>.hasPaparazzi() = filter { it.isPaparazzi() }.count() > 0
  private fun KSAnnotation.isPaparazzi() = qualifiedName() == ANNOTATION_NAME
  private fun KSAnnotation.qualifiedName() = declaration().qualifiedName?.asString() ?: ""
  private fun KSAnnotation.declaration() = annotationType.resolve().declaration
}
