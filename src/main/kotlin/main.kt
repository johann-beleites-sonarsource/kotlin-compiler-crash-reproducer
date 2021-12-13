import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

fun main() {
    val codeToCompile = """
        abstract class MyClass {
            abstract fun <P1> foo(): (P1) -> Unknown<String>

            private fun callTryConvertConstant() {
                println(foo<String>())
            }
        }
        """.trimIndent()

    val disposable = Disposer.newDisposable()
    val configuration = CompilerConfiguration().apply {
        put(
            CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl(
                LanguageVersion.KOTLIN_1_6,
                ApiVersion.createByLanguageVersion(LanguageVersion.KOTLIN_1_6),
            )
        )
        put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
        put(
            CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE
        )
        put(CommonConfigurationKeys.MODULE_NAME, "")
    }

    val env = KotlinCoreEnvironment.createForProduction(
        disposable,
        configuration,
        EnvironmentConfigFiles.JVM_CONFIG_FILES,
    )

    val ktPsiFactory = KtPsiFactory(env.project, false)
    val psiFile: KtFile = ktPsiFactory.createFile("/tmp/foo.kt", codeToCompile)

    TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
        env.project,
        listOf(psiFile),
        NoScopeRecordCliBindingTrace(),
        configuration,
        env::createPackagePartProvider,
        ::FileBasedDeclarationProviderFactory
    )
}
