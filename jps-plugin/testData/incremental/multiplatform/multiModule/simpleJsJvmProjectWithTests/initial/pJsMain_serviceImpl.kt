//
// DON'T EDIT! This file is GENERATED by `MppJpsIncTestsGenerator` (called in generateTests)
// from `incremental/multiplatform/multiModule/simpleJsJvmProjectWithTests/dependencies.txt`
//

actual fun cMain_platformDependentCMain(): String = "pJsMain"
fun pJsMain_platformOnly() = "pJsMain"

fun TestPJsMain() {
  pJsMain_platformOnly()
  cMain_platformIndependentCMain()
  cMain_platformDependentCMain()
}
