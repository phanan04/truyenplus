import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.psi.KtPsiFactory;

/** Parses changed Kotlin sources with Kotlin's own parser, without replacing a build. */
public class KotlinSyntaxCheck {
    public static void main(String[] args) throws Exception {
        var disposable = Disposer.newDisposable();
        try {
            var environment = KotlinCoreEnvironment.createForProduction(
                disposable, new CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES);
            var factory = new KtPsiFactory(environment.getProject(), false);
            int errors = 0;
            for (var file : args) {
                var source = factory.createFile(Path.of(file).getFileName().toString(), Files.readString(Path.of(file)));
                for (var error : PsiTreeUtil.collectElementsOfType(source, PsiErrorElement.class)) {
                    System.err.println(file + ":" + error.getTextOffset() + ": " + error.getErrorDescription());
                    errors++;
                }
            }
            if (errors > 0) throw new IllegalStateException(errors + " Kotlin syntax errors");
            System.out.println("PASS: Kotlin parser checked " + args.length + " changed/new source files");
        } finally {
            Disposer.dispose(disposable);
        }
    }
}
