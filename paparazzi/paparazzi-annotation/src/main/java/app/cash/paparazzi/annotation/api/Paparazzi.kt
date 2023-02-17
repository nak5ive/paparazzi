package app.cash.paparazzi.annotation.api

@Target(
  AnnotationTarget.ANNOTATION_CLASS,
  AnnotationTarget.FUNCTION
)
@Retention(AnnotationRetention.BINARY)
annotation class Paparazzi
