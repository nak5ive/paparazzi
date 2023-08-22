package app.cash.paparazzi.annotation.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.CodeGenerator
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

const val PACKAGE_NAME = "app.cash.paparazzi.annotation"
const val ANNOTATION_QUALIFIED_NAME = "app.cash.paparazzi.annotation.api.Paparazzi"

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
    val (variantName, isTest) = collectProjectInfo(environment.codeGenerator, dependencies)

    environment.logger.info("PAPARAZZI - running in $variantName")

    return findPaparazziFunctions(resolver)
      .also {
        environment.logger.info("PAPARAZZI - found ${it.count()} annotated function(s)")
        if (isTest) {
          PaparazziPoet.buildTestFiles(it)
        } else {
          PaparazziPoet.buildPreviewFiles(it)
        }.forEach { file ->
          file.writeTo(environment.codeGenerator, dependencies)
        }
      }
      .filterNot { it.validate() }
      .toList()
  }

  private fun findPaparazziFunctions(resolver: Resolver) =
    resolver.getSymbolsWithAnnotation("androidx.compose.runtime.Composable")
      .filterIsInstance<KSFunctionDeclaration>()
      .filter {
        it.annotations.hasPaparazzi() &&
          it.functionKind == TOP_LEVEL &&
          it.getVisibility() in listOf(PUBLIC, INTERNAL)
      }

  private fun collectProjectInfo(codeGenerator: CodeGenerator, dependencies: Dependencies): Pair<String, Boolean> {
    codeGenerator.createNewFile(dependencies, PACKAGE_NAME, "environment", "kt")

    val file = codeGenerator.generatedFile.first()
    val path = file.absolutePath

    val variantRegex = Regex("ksp/(.+)/kotlin")
    val (variantName) = variantRegex.find(path)!!.destructured
    val isTest = variantName.endsWith("UnitTest")

    return (variantName to isTest).also { file.writeText("// variant: $variantName, test: $isTest") }
  }
}
