package app.cash.paparazzi.annotation.processor

import app.cash.paparazzi.annotation.processor.PaparazziProcessor.Companion
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.FunctionKind.TOP_LEVEL
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.writeTo
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class PaparazziProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment) = PaparazziProcessor(
    environment.codeGenerator,
    environment.options,
    environment.logger
  )
}

class PaparazziProcessor(
  private val codeGenerator: CodeGenerator,
  private val options: Map<String, String>,
  private val logger: KSPLogger,
) : SymbolProcessor {

  var invoked = false

  companion object {
    const val ANNOTATION_NAME = "app.cash.paparazzi.annotation.api.Paparazzi"
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    if (invoked) {
      return emptyList()
    }
    invoked = true

    val projectInfo = codeGenerator.collectProjectInfo()

    val manifestLocation = projectInfo.buildDir + File.separator +
      "generated" + File.separator +
      "paparazzi" + File.separator +
      projectInfo.variantName + "UnitTest"

    val manifestDir = File(manifestLocation).apply { mkdirs() }

    return resolver.findPaparazziFunctions()
      .also {
        val fileSpec = PaparazziPoet.buildFile(projectInfo, it)

        val manifestFile = File(manifestDir, "${fileSpec.name}.kt").apply { createNewFile() }
        OutputStreamWriter(manifestFile.outputStream(), StandardCharsets.UTF_8).use(fileSpec::writeTo)
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
        it.functionKind == TOP_LEVEL
    }

private fun Sequence<KSAnnotation>.hasPaparazzi() = filter { it.isPaparazzi() }.count() > 0
private fun KSAnnotation.isPaparazzi() = qualifiedName() == PaparazziProcessor.ANNOTATION_NAME
private fun KSAnnotation.qualifiedName() = declaration().qualifiedName?.asString() ?: ""
private fun KSAnnotation.declaration() = annotationType.resolve().declaration

private fun CodeGenerator.collectProjectInfo(): ProjectInfo {
  createNewFileByPath(Dependencies(false), "app.cash.paparazzi", "annotation")
  return generatedFile.first().run {
    val path = absolutePath

    val variantRegex = Regex("ksp/(.+?)(UnitTest)?/resources")
    val (variantName, testName) = variantRegex.find(path)!!.destructured

    ProjectInfo(
      buildDir = path.substring(0, path.indexOf("/generated")),
      variantName = variantName,
      isTest = testName.isNotEmpty(),
    ).also { writeText("$it") }
  }
}

data class ProjectInfo(
  val buildDir: String,
  val variantName: String,
  val isTest: Boolean,
)
