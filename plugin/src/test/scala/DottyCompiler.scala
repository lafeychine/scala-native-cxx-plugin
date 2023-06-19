import java.io.File
import java.nio.file.{Files, StandardOpenOption, Path}
import java.nio.charset.StandardCharsets
import java.util.{Comparator, UUID}
import java.util.function.Supplier

import scala.io.Codec
import scala.jdk.CollectionConverters.*

import dotty.tools.io.AbstractFile
import dotty.tools.dotc.*
import dotty.tools.dotc.config.*
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.reporting.*
import dotty.tools.dotc.util.*

class DottyCompiler private (private val outputDir: Path):

    private val cxxlib = sys.props("scalanative.cxxlib.jar")
    private val cxxplugin = sys.props("scalanative.cxxplugin.jar")
    private val nsclib = sys.props("scalanative.nsclib.cp")
    private val nscplugin = sys.props("scalanative.nscplugin.jar")

    def this() =
        this(Files.createTempDirectory("scala-native-cxx-test-" + UUID.randomUUID()).toAbsolutePath)

    private def compile(files: Map[String, String], isPluginEnabled: Boolean): Array[String] =
        val classpath = if isPluginEnabled then s"$cxxlib:$nsclib" else nsclib
        val plugins = if isPluginEnabled then s"$cxxplugin,$nscplugin" else nscplugin

        val printTree = "-Xprint-types -Xprint:scalanative-genNIR"
        val sourceFiles = writeFiles(files, isPluginEnabled)

        val args = CommandLineParser.tokenize(s"-d $outputDir $printTree -Xplugin:$plugins -cp $classpath")
            ++ sourceFiles.map(_.file.absolutePath)

        val reporter = TastyReporter()
        val dotty = Driver().process(args.toArray, reporter)

        if dotty.allErrors.nonEmpty then throw new CompilationFailedException(dotty.allErrors.head)

        reporter.tastyTree

    def withPlugin(files: (String, String)*): Array[String] =
        compile(files.toMap, true)

    def withoutPlugin(files: (String, String)*): Array[String] =
        compile(files.toMap, false)

    private def writeFiles(files: Map[String, String], isPluginEnabled: Boolean): Iterable[SourceFile] =
        Files.walk(outputDir).sorted(Comparator.reverseOrder()).iterator().asScala.foreach(Files.delete)

        for (name, content) <- files yield
            val path = outputDir.resolve(name)

            Files.createDirectories(path.getParent)
            Files.writeString(path, "import scala.scalanative.unsafe.*" + System.lineSeparator(), StandardOpenOption.CREATE)

            if isPluginEnabled then
                Files.writeString(path, "import scala.scalanative.unsafe.cxx.*" + System.lineSeparator(), StandardOpenOption.APPEND)

            Files.write(path, content.stripMargin.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND)

            SourceFile(AbstractFile.getFile(path), Codec.default)

    private class CompilationFailedException(diagnostics: Diagnostic) extends Exception(diagnostics.message)

    private class TastyReporter() extends AbstractReporter:

        private val _tastyTree = new StringBuilder

        override def doReport(diagnostics: Diagnostic)(using Context): Unit =
            import dotty.tools.dotc.interfaces.Diagnostic.ERROR

            if diagnostics.level == ERROR then throw new CompilationFailedException(diagnostics)

            if diagnostics.message.startsWith("[[syntax trees at end of        scalanative-genNIR]]") then
                _tastyTree.append(diagnostics.message)

        def tastyTree: Array[String] =
            if _tastyTree.isEmpty then throw new IllegalStateException("No tasty tree found")

            _tastyTree.toString.split("\n").filter(_.nonEmpty)
