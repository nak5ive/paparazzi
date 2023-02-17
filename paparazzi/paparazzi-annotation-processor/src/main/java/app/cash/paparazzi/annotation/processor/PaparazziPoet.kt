package app.cash.paparazzi.annotation.processor

import app.cash.paparazzi.annotation.api.Paparazzi
import app.cash.paparazzi.annotation.processor.models.PaparazziModel
import app.cash.paparazzi.annotation.processor.poetry.Imports
import app.cash.paparazzi.annotation.processor.poetry.buildConstructorParams
import app.cash.paparazzi.annotation.processor.poetry.buildInitializer
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

object PaparazziPoet {

  fun buildFile(model: PaparazziModel): FileSpec {
    val testSuffix = if (model.functionName.endsWith("Test")) "" else "Test"
    val className = "${Paparazzi::class.simpleName}_${model.functionName}$testSuffix"

    return FileSpec.builder(model.packageName, className)
      .addType(
        TypeSpec.classBuilder(className)
          .addRunWithAnnotation()
          .addInjectedConstructor(model)
          .addPaparazziProperty(model)
          .addTestFunction(model)
          .build()
      )
      .build()
  }

  private fun TypeSpec.Builder.addRunWithAnnotation() = addAnnotation(
    AnnotationSpec.builder(Imports.JUnit.runWith)
      .addMember("%T::class", Imports.TestInject.testParameterInjector)
      .build()
  )

  private fun TypeSpec.Builder.addInjectedConstructor(model: PaparazziModel) = apply {
    model.buildConstructorParams().let { (params, properties, types) ->
      FunSpec.constructorBuilder()
        .addParameters(params)
        .build()
        .let(::primaryConstructor)

      addProperties(properties)
      addTypes(types)
    }
  }

  private fun TypeSpec.Builder.addPaparazziProperty(model: PaparazziModel) = apply {
    PropertySpec.builder("paparazzi", Imports.Paparazzi.paparazzi)
      .addAnnotation(
        AnnotationSpec.builder(Imports.JUnit.rule)
          .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
          .build()
      )
      .initializer(model.buildInitializer())
      .build()
      .let(::addProperty)
  }

  private fun TypeSpec.Builder.addTestFunction(model: PaparazziModel) = apply {
    val codeBuilder = CodeBlock.builder()

    codeBuilder
      .addStatement("paparazzi.snapshot {")
      .indent()
      .addStatement(
        "%L(${if (model.previewParam != null) "previewParam" else ""})",
        model.functionName
      )
      .unindent()
      .addStatement("}")

    FunSpec.builder("default")
      .addAnnotation(Imports.JUnit.test)
      .addCode(codeBuilder.build())
      .build()
      .let(::addFunction)
  }
}
