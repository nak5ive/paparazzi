package app.cash.paparazzi.annotation.processor

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName

fun KSAnnotation.isPaparazzi() = qualifiedName() == ANNOTATION_QUALIFIED_NAME
fun KSAnnotation.isPreview() = qualifiedName() == "androidx.compose.ui.tooling.preview.Preview"
fun KSAnnotation.isPreviewParameter() = qualifiedName() == "androidx.compose.ui.tooling.preview.PreviewParameter"

fun KSAnnotation.qualifiedName() = declaration().qualifiedName?.asString() ?: ""
fun KSAnnotation.declaration() = annotationType.resolve().declaration

@Suppress("UNCHECKED_CAST")
fun <T> KSAnnotation.previewArg(name: String): T = arguments
  .first { it.name?.asString() == name }
  .let { it.value as T }

fun Sequence<KSAnnotation>.hasPaparazzi() = filter { it.isPaparazzi() }.count() > 0

/**
 * when the same annotations are applied higher in the tree, an endless recursive lookup can occur.
 * using a stack to keep to a record of each symbol lets us break when we hit one we've already encountered
 */
fun Sequence<KSAnnotation>.findPreviews(stack: Set<KSAnnotation> = setOf()): Sequence<KSAnnotation> {
  val direct = filter { it.isPreview() }
  val indirect = filterNot { it.isPreview() || stack.contains(it) }
    .map { it.declaration().annotations.findPreviews(stack.plus(it)) }
    .flatten()
  return direct.plus(indirect)
}

fun KSFunctionDeclaration.findPreviews() = annotations.findPreviews().toList()

fun KSFunctionDeclaration.previewParam() = parameters.firstOrNull { param ->
  param.annotations.any { it.isPreviewParameter() }
}

fun KSValueParameter.previewParamProviderClassName() = annotations
  .first { it.isPreviewParameter() }
  .arguments
  .first { arg -> arg.name?.asString() == "provider" }
  .let { it.value as KSType }
  .declaration.qualifiedName?.let {
    ClassName(it.getQualifier(), it.getShortName())
  }

fun KSValueParameter.previewParamTypeClassName() = type.resolve().declaration.qualifiedName?.let {
  ClassName(it.getQualifier(), it.getShortName())
}
