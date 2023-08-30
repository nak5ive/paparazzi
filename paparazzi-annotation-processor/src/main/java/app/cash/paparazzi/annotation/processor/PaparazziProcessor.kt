package app.cash.paparazzi.annotation.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.FunctionKind.TOP_LEVEL
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Visibility.INTERNAL
import com.google.devtools.ksp.symbol.Visibility.PUBLIC
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.File

class PaparazziProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment) = PaparazziProcessor(environment)
}

class PaparazziProcessor(
  private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

  private var invoked = false

  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (invoked) return emptyList()
    invoked = true

    val dependencies = Dependencies(true, *resolver.getAllFiles().toList().toTypedArray())
    val isTest = isTestSourceSet(dependencies)

    return resolver.getSymbolsWithAnnotation("androidx.compose.runtime.Composable")
      .findPaparazzi()
      .also { functions ->
        "found ${functions.count()} function(s)".log()

        PaparazziPoet.buildFiles(functions, isTest).forEach { file ->
          "writing file: ${file.packageName}.${file.name}".log()
          file.writeTo(environment.codeGenerator, dependencies)
        }
      }
      .filterNot { it.validate() }
      .toList()
  }

  private fun Sequence<KSAnnotated>.findPaparazzi() =
    filterIsInstance<KSFunctionDeclaration>()
      .filter {
        it.annotations.hasPaparazzi() &&
          it.functionKind == TOP_LEVEL &&
          it.getVisibility() in listOf(PUBLIC, INTERNAL)
      }

  private fun isTestSourceSet(dependencies: Dependencies): Boolean {
    environment.codeGenerator.createNewFile(dependencies, PACKAGE_NAME, "environment", "txt")
    val file = environment.codeGenerator.generatedFile.first()

    val sep = File.separator
    val variantName = Regex("ksp${sep}(.+)${sep}resources")
      .find(file.absolutePath)?.groups?.get(1)?.value ?: ""
    val isTest = variantName.endsWith("UnitTest")

    "variant: $variantName, test: $isTest".log()
      .also { file.writeText(it) }

    return isTest
  }

  private fun String.log() = apply { environment.logger.info("PaparazziProcessor - $this") }
}
