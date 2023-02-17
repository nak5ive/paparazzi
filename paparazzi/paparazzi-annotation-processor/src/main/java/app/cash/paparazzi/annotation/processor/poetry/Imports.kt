package app.cash.paparazzi.annotation.processor.poetry

import com.squareup.kotlinpoet.ClassName

object Imports {

  object JUnit {
    val rule = ClassName("org.junit", "Rule")
    val test = ClassName("org.junit", "Test")
    val runWith = ClassName("org.junit.runner", "RunWith")
  }

  object TestInject {
    val testParameterInjector =
      ClassName("com.google.testing.junit.testparameterinjector", "TestParameterInjector")
    val testParameter =
      ClassName("com.google.testing.junit.testparameterinjector", "TestParameter")
    val testParameterValuesProvider =
      ClassName(
        "com.google.testing.junit.testparameterinjector.TestParameter",
        "TestParameterValuesProvider"
      )
  }

  object Paparazzi {
    val paparazzi = ClassName("app.cash.paparazzi", "Paparazzi")
  }
}
