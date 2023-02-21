package app.cash.paparazzi.annotation.processor

import app.cash.paparazzi.annotation.api.Paparazzi
import app.cash.paparazzi.annotation.processor.models.PaparazziModel
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class PaparazziProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment) =
    PaparazziProcessor(environment.options)
}

class PaparazziProcessor(private val options: Map<String, String>) : SymbolProcessor {

  companion object {
    const val OUTPUT_DIR_OPTION = "paparazziOutputDir"

    private val separator = File.separator
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    return resolver.findPaparazziFunctions()
      .onEach { function ->
        function.accept(PaparazziVisitor(), Unit)
          ?.writeFile()
      }
      .filterNot { it.validate() }
      .toList()
  }

  private fun Resolver.findPaparazziFunctions() =
    getSymbolsWithAnnotation("androidx.compose.runtime.Composable")
      .filterIsInstance<KSFunctionDeclaration>()
      .filter { it.annotations.hasPaparazzi() }

  private fun PaparazziModel.writeFile() {
    val fileSpec = PaparazziPoet.buildFile(this)

    val outputDir = fileSpec.outputDirectory()
    outputDir.mkdirs()

    val outputFile = File(outputDir, "${fileSpec.name}.kt")
    outputFile.createNewFile()

    OutputStreamWriter(outputFile.outputStream(), StandardCharsets.UTF_8).use(fileSpec::writeTo)
  }

  private fun FileSpec.outputDirectory(): File {
    val baseDir = options[OUTPUT_DIR_OPTION] + separator
    val packageDirs =
      if (packageName != "") "${packageName.split(".").joinToString(separator)}$separator" else ""
    return File("$baseDir$packageDirs")
  }

  private fun Sequence<KSAnnotation>.hasPaparazzi() = filter { it.isPaparazzi() }.count() > 0

  private fun KSAnnotation.isPaparazzi() =
    qualifiedName() == Paparazzi::class.qualifiedName.toString()

  private fun KSAnnotation.qualifiedName() = declaration().qualifiedName?.asString() ?: ""

  private fun KSAnnotation.declaration() = annotationType.resolve().declaration
}
