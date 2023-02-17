package app.cash.paparazzi.annotation.processor.poetry

import app.cash.paparazzi.annotation.processor.models.PaparazziModel
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName

fun PaparazziModel.buildConstructorParams(): Triple<List<ParameterSpec>, List<PropertySpec>, List<TypeSpec>> {
  val params = mutableListOf<ParameterSpec>()
  val properties = mutableListOf<PropertySpec>()
  val types = mutableListOf<TypeSpec>()

  val providerStatement = "return %T().values.toList()"

  if (previewParam != null) {
    val paramName = "previewParam"
    val providerName = "PreviewProvider"

    ParameterSpec.builder(paramName, previewParam.type.toTypeName())
      .build()
      .let(params::add)

    buildConstructorProperty(paramName, providerName, previewParam.type.toTypeName())
      .let(properties::add)

    buildValuesProvider(providerName, providerStatement, previewParam.provider.toTypeName())
      .let(types::add)
  }

  return Triple(params, properties, types)
}

private fun buildConstructorProperty(
  name: String,
  providerName: String,
  type: TypeName
) =
  PropertySpec.builder(name, type)
    .initializer(name)
    .addModifiers(KModifier.PRIVATE)
    .addAnnotation(
      AnnotationSpec.builder(Imports.TestInject.testParameter)
        .addMember("valuesProvider = $providerName::class")
        .build()
    )
    .build()

private fun buildValuesProvider(
  providerName: String,
  statementFormat: String,
  vararg args: Any
) = TypeSpec.classBuilder(providerName)
  .addSuperinterface(Imports.TestInject.testParameterValuesProvider)
  .addFunction(
    FunSpec.builder("provideValues")
      .addModifiers(OVERRIDE)
      .addStatement(statementFormat, *args)
      .build()
  )
  .build()

fun PaparazziModel.buildInitializer(): CodeBlock {
  return CodeBlock.builder()
    .addStatement("%T()", Imports.Paparazzi.paparazzi)
    .build()
}
