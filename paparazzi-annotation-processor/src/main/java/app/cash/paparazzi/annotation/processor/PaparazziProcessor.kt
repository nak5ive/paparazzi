package app.cash.paparazzi.annotation.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.FunctionKind.TOP_LEVEL
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Visibility.PUBLIC
import com.google.devtools.ksp.symbol.Visibility.INTERNAL
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.writeTo

class PaparazziProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment) = PaparazziProcessor(environment)
}

class PaparazziProcessor(
  private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

  private var invoked = false

  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (invoked) return emptyList()
    invoked = true

    val dependencies = Dependencies(true, *resolver.getAllFiles().toList().toTypedArray())
    val projectInfo = environment.collectProjectInfo(dependencies, PACKAGE_NAME)

    environment.logger.info("PAPARAZZI - running in ${projectInfo.variantName}")

    return resolver.findPaparazziFunctions()
      .also {
        environment.logger.info("PAPARAZZI - found ${it.count()} annotated function(s)")
        if (projectInfo.isTest) {
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
}

private fun Resolver.findPaparazziFunctions() =
  getSymbolsWithAnnotation("androidx.compose.runtime.Composable")
    .filterIsInstance<KSFunctionDeclaration>()
    .filter {
      it.annotations.hasPaparazzi() &&
        it.functionKind == TOP_LEVEL &&
        it.getVisibility() in listOf(PUBLIC, INTERNAL)
    }

private fun Sequence<KSAnnotation>.hasPaparazzi() = filter { it.isPaparazzi() }.count() > 0
private fun KSAnnotation.isPaparazzi() = qualifiedName() == ANNOTATION_QUALIFIED_NAME
private fun KSAnnotation.qualifiedName() = declaration().qualifiedName?.asString() ?: ""
private fun KSAnnotation.declaration() = annotationType.resolve().declaration

private fun SymbolProcessorEnvironment.collectProjectInfo(dependencies: Dependencies, fileName: String): ProjectInfo {
  codeGenerator.createNewFileByPath(dependencies, fileName, "txt")
  return codeGenerator.generatedFile.first().run {
    val path = absolutePath

    val variantRegex = Regex("ksp/(.+)/resources")
    val (variantName) = variantRegex.find(path)!!.destructured

    ProjectInfo(
      variantName = variantName,
      isTest = variantName.endsWith("UnitTest"),
    ).also { writeText("$it") }
  }
}

data class ProjectInfo(
  val variantName: String,
  val isTest: Boolean,
)
