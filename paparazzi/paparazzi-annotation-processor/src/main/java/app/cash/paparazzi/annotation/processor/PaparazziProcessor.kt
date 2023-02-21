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
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class PaparazziProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment) =
    PaparazziProcessor(environment.options)
}

class PaparazziProcessor(private val options: Map<String, String>) : SymbolProcessor {
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
    val file = PaparazziPoet.buildFile(this)

    val outputPath = dirOf(file.packageName)
    val outputDir = File(outputPath)
    outputDir.mkdirs()
    val outputFile = File(outputDir, "${file.name}.kt")
    outputFile.createNewFile()
    OutputStreamWriter(outputFile.outputStream(), StandardCharsets.UTF_8).use(file::writeTo)
  }

  private val separator = File.separator

  fun dirOf(packageName: String): String {
    val baseDir = options["paparazziTestDir"] + separator
    val packageDirs =
      if (packageName != "") "${packageName.split(".").joinToString(separator)}$separator" else ""
    return "$baseDir$packageDirs"
  }

  private fun Sequence<KSAnnotation>.hasPaparazzi() = filter { it.isPaparazzi() }.count() > 0

  private fun KSAnnotation.isPaparazzi() =
    qualifiedName() == Paparazzi::class.qualifiedName.toString()

  private fun KSAnnotation.qualifiedName() = declaration().qualifiedName?.asString() ?: ""

  private fun KSAnnotation.declaration() = annotationType.resolve().declaration
}
