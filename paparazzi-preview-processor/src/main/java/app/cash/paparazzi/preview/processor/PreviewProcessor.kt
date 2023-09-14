package app.cash.paparazzi.preview.processor

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.File

class PreviewProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment) = PreviewProcessor(environment)
}

class PreviewProcessor(
  private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

  private var invoked = false

  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (invoked) return emptyList()
    invoked = true

    val dependencies = Dependencies(true, *resolver.getAllFiles().toList().toTypedArray())
    val isTestSourceSet = discoverVariant(dependencies).endsWith("UnitTest")

    return resolver.getSymbolsWithAnnotation("androidx.compose.runtime.Composable")
      .findPaparazzi()
      .also { functions ->
        "found ${functions.count()} function(s)".log()
        PaparazziPoet.buildFiles(functions, isTestSourceSet).forEach { file ->
          "writing file: ${file.packageName}.${file.name}".log()
          file.writeTo(environment.codeGenerator, dependencies)
        }
      }
      .filterNot { it.validate() }
      .toList()
  }

  private fun discoverVariant(dependencies: Dependencies): String {
    environment.codeGenerator.createNewFile(dependencies, PACKAGE_NAME, "variant", "txt")
    val file = environment.codeGenerator.generatedFile.first()
    val fileSeparator = Regex.escape(File.separator)
    val variantNameRegex = Regex("ksp$fileSeparator(.+)${fileSeparator}resources")
    return (variantNameRegex.find(file.absolutePath)?.groups?.get(1)?.value ?: "")
      .also {
        "variant: $it".log()
        file.writeText(it)
      }
  }

  private fun String.log() = environment.logger.info("PaparazziProcessor - $this")
}
