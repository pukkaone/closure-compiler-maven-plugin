package com.github.pukkaone.closurecompilermavenplugin;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.WarningLevel;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.DirectoryScanner;

/**
 * @goal compile
 * @phase generate-sources
 */
public class ClosureCompilerMojo extends AbstractMojo {

    /**
     * @parameter expression="WARNING"
     * @required
     */
    private String loggingLevel;

    /**
     * @parameter expression="SIMPLE_OPTIMIZATIONS"
     * @required
     */
    private String compilationLevel;

    /**
     * @parameter expression="VERBOSE"
     * @required
     */
    private String warningLevel;

    /**
     * @parameter expression="null"
     * @required
     */
    private String formatting;

    /**
     * @parameter
     * @required
     */
    private String[] entryPoints;

    /**
     * @parameter expression="false"
     * @required
     */
    private boolean generateExports;

    /**
     * @parameter expression="src/main/webapp/js"
     * @required
     */
    private File externsSourceDirectory;

    /**
     * @parameter expression="src/main/js"
     * @required
     */
    private File sourceDirectory;

    /**
     * @parameter expression="${project.build.directory}/${project.artifactId}-${project.version}/js/${project.artifactId}.js"
     * @required
     */
    private File outputFile;

    /**
     * @parameter expression=true
     * @required
     */
    private boolean merge;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * @parameter expression="false"
     * @required
     */
    private boolean logSourceFiles;

    /**
     * @parameter expression="false"
     * @required
     */
    private boolean logExternFiles;

    /**
     * @parameter expression="true"
     * @required
     */
    private boolean addDefaultExterns;

    /**
     * @parameter expression="false"
     * @required
     */
    private boolean stopOnWarnings;

    /**
     * @parameter expression="false"
     * @required
     */
    private boolean stopOnErrors;

    @Override
    public void execute() throws MojoFailureException {
        CompilerOptions compilerOptions = new CompilerOptions();

        parseCompilationLevel(compilerOptions);
        parseWarningLevel(compilerOptions);
        parseEntryPoints(compilerOptions);
        compilerOptions.setGenerateExports(generateExports);
        parseFormattingOptions(compilerOptions);
        parseLoggingLevel();

        List<SourceFile> externs = parseExterns();
        List<String> sources = parseSources();

        try {
            if (merge) {
                String source = compile(
                        compilerOptions,
                        externs,
                        filenamesToSourceFiles(sourceDirectory, sources));
                Files.createParentDirs(outputFile);
                Files.touch(outputFile);
                Files.write(source, outputFile, StandardCharsets.UTF_8);
            } else {
                for (String curSourceFile : sources) {
                    String source = compile(
                            compilerOptions,
                            externs,
                            ImmutableList.of(
                                    SourceFile.fromFile(
                                            new File(sourceDirectory, curSourceFile))));
                    File curOuputFile = sourceToDest(curSourceFile);
                    Files.createParentDirs(curOuputFile);
                    Files.touch(curOuputFile);
                    Files.write(source, curOuputFile, StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private File sourceToDest(String sourceFile) {
        return new File(
                outputDirectory,
                sourceFile.substring(0, sourceFile.length() - 3) + ".min.js");
    }

    private String compile(
            CompilerOptions compilerOptions,
            List<SourceFile> externs,
            List<SourceFile> sources)
        throws MojoFailureException
    {
        Compiler compiler = new Compiler();
        Result result = compiler.compile(externs, sources, compilerOptions);

        logWarnings(result);
        logErrors(result);

        if (!result.success) {
            throw new MojoFailureException("Compilation failure");
        }

        return compiler.toSource();
    }

    private void logWarnings(Result result) throws MojoFailureException {
        boolean hasWarnings = false;
        for (JSError warning : result.warnings) {
            getLog().warn(warning.toString());
            hasWarnings = true;
        }

        if (stopOnWarnings && hasWarnings) {
            throw new MojoFailureException("Compilation failed: has warnings");
        }
    }

    private void logErrors(Result result) throws MojoFailureException {
        boolean hasErrors = false;
        for (JSError error : result.errors) {
            getLog().error(error.toString());
            hasErrors = true;
        }

        if (stopOnErrors && hasErrors) {
            throw new MojoFailureException("Compilation failed: has errors");
        }
    }

    private void parseCompilationLevel(CompilerOptions compilerOptions)
        throws MojoFailureException
    {
        CompilationLevel level = null;
        try {
            level = CompilationLevel.valueOf(this.compilationLevel);
            level.setOptionsForCompilationLevel(compilerOptions);
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException(
                    "Compilation level invalid (values: " + Arrays.asList(CompilationLevel.values()) + ")",
                    e);
        }
    }

    private void parseWarningLevel(CompilerOptions compilerOptions)
        throws MojoFailureException
    {
        WarningLevel level = null;
        try {
            level = WarningLevel.valueOf(this.warningLevel);
            level.setOptionsForWarningLevel(compilerOptions);
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException(
                    "Warning level invalid (values: " + Arrays.asList(WarningLevel.values()) + ")",
                    e);
        }
    }

    private void parseEntryPoints(CompilerOptions compilerOptions) {
        if (entryPoints != null) {
            compilerOptions.setManageClosureDependencies(
                    Arrays.asList(entryPoints));
        }
    }

    private void parseFormattingOptions(CompilerOptions compilerOptions)
        throws MojoFailureException
    {
        FormattingOption formattingOption = null;
        if (this.formatting != null && !this.formatting.equals("null")) {
            try {
                formattingOption = FormattingOption.valueOf(this.formatting);
                formattingOption.applyToOptions(compilerOptions);
            } catch (IllegalArgumentException e) {
                throw new MojoFailureException(
                        "Formatting invalid (values: " + Arrays.asList(FormattingOption.values()) + ")", e);
            }
        }
    }

    private void parseLoggingLevel() throws MojoFailureException {
        Level level = null;
        try {
            level = Level.parse(this.loggingLevel);
            Compiler.setLoggingLevel(level);
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException(
                    "Logging level invalid (values: [ALL, CONFIG, FINE, FINER, FINEST, INFO, OFF, SEVERE, WARNING])",
                    e);
        }
    }

    private List<SourceFile> parseExterns() throws MojoFailureException {
        List<SourceFile> externs = new ArrayList<SourceFile>();
        if (addDefaultExterns) {
            try {
                externs.addAll(CommandLineRunner.getDefaultExterns());
            } catch (IOException e) {
                throw new MojoFailureException("Default externs adding error", e);
            }
        }

        List<File> externFiles = listFiles(externsSourceDirectory);
        Collections.sort(externFiles);
        externs.addAll(filesToSourceFiles(externFiles));
        if (logExternFiles) {
            getLog().info("Extern files:");
            for (SourceFile f : externs) {
                getLog().info(f.getOriginalPath());
            }
        }
        return externs;
    }

    private List<String> parseSources() {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(sourceDirectory);
        scanner.setIncludes(new String[] { "**/*.js" });
        scanner.addDefaultExcludes();
        scanner.scan();

        List<String> sources = ImmutableList.copyOf(scanner.getIncludedFiles());
        if (logSourceFiles) {
            getLog().info("Source files:");
            for (String f : sources) {
                getLog().info(f);
            }
        }
        return sources;
    }

    private List<SourceFile> filesToSourceFiles(List<File> files) {
        return Lists.transform(files, new Function<File, SourceFile>() {
            @Override
            public SourceFile apply(File input) {
                return SourceFile.fromFile(input);
            }
        });
    }

    private List<SourceFile> filenamesToSourceFiles(
            final File path, List<String> filenames)
    {
        return Lists.transform(filenames, new Function<String, SourceFile>() {
            @Override
            public SourceFile apply(String input) {
                return SourceFile.fromFile(new File(path, input));
            }
        });
    }

    private List<File> listFiles(File directory) {
        return listFiles(new ArrayList<File>(), directory);
    }

    private List<File> listFiles(List<File> foundFiles, File directory) {
        if (directory != null) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : directory.listFiles()) {
                    if (file.isFile()) {
                        if (file.getName().endsWith(".js")) {
                            foundFiles.add(file);
                        }
                    } else {
                        listFiles(foundFiles, file);
                    }
                }
            }
        }
        return foundFiles;
    }

    private static enum FormattingOption {
        PRETTY_PRINT,
        PRINT_INPUT_DELIMITER;

        private void applyToOptions(CompilerOptions options) {
            switch (this) {
            case PRETTY_PRINT:
                options.prettyPrint = true;
                break;
            case PRINT_INPUT_DELIMITER:
                options.printInputDelimiter = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown formatting option: " + this);
            }
        }
    }
}
