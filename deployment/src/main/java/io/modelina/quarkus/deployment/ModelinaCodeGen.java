package io.modelina.quarkus.deployment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;

import com.github.eirslett.maven.plugins.frontend.lib.DirectoryCacheResolver;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.quarkus.logging.Log;

public abstract class ModelinaCodeGen implements CodeGenProvider {

    @Override
    public String providerId() {
        return "modelina";
    }

    @Override
    public abstract String inputExtension();

    @Override
    public String inputDirectory() {
        return "asyncapi";
    }

    private Stream<Path> findDescriptions(Path sourceDir) {
        try {
            return Files.find(
                    sourceDir,
                    Integer.MAX_VALUE,
                    (filePath, fileAttr) -> filePath.toString().endsWith(inputExtension())
                            && fileAttr.isRegularFile(),
                    FileVisitOption.FOLLOW_LINKS)
                    .distinct();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to visit the folder: " + sourceDir.toFile().getAbsolutePath(), e);
        }
    }

    private String getFolderMd5(Path source) throws CodeGenException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            Files.find(
                    source,
                    Integer.MAX_VALUE,
                    (filePath, fileAttr) -> filePath.toString().endsWith(inputExtension())
                            && fileAttr.isRegularFile())
                    .distinct()
                    .sorted()
                    .forEachOrdered(
                            f -> {
                                try {
                                    digest.update(Files.readAllBytes(f));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
            return new String(digest.digest(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CodeGenException(
                    "Failed to calculate the contentHash of generated sources", e);
        } catch (NoSuchAlgorithmException e) {
            throw new CodeGenException("Cannot find MD5 algorithm", e);
        }
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        Log.debug("Running trigger logic.");
        String folderHashBefore = getFolderMd5(context.outDir());

        for (Path spec : findDescriptions(context.inputDir()).collect(Collectors.toList())) {
            executeModelina(
                    spec,
                    context.outDir(),
                    context.config());
        }

        String folderHashAfter = getFolderMd5(context.outDir());
        return !folderHashBefore.equals(folderHashAfter);
    }

    @Override
    public boolean shouldRun(Path sourceDir, Config config) {
        Log.debug("Should run inspecting the source dir: " + sourceDir);
        if (Files.isDirectory(sourceDir)) {
            return findDescriptions(sourceDir).count() > 0;
        }
        return false;
    }

    private void executeModelina(
            Path asyncApiSpec, Path outputDir, Config config) throws CodeGenException {
        if (!asyncApiSpec.toFile().exists()) {
            throw new IllegalArgumentException(
                    "Spec file not found on the path: " + asyncApiSpec.toFile().getAbsolutePath());
        }
        outputDir.toFile().mkdirs();

        // TODO: make proper configuration for those!
        Path targetRoot = outputDir.getParent().resolveSibling("quarkus-modelina");
        File workingDirectory = targetRoot.resolve("node-install").toFile();
        workingDirectory.mkdirs();

        var factory = new FrontendPluginFactory(workingDirectory, workingDirectory, new DirectoryCacheResolver(
                workingDirectory.toPath().resolve("cache").toFile()));
        // TODO: implement proper handling of proxy etc:
        // https://github.com/eirslett/frontend-maven-plugin/blob/c052e6b72ac22fceeb4128ab63a3b4702042e026/frontend-maven-plugin/src/main/java/com/github/eirslett/maven/plugins/frontend/mojo/InstallNodeAndNpmMojo.java#L75
        var proxyConfig = new ProxyConfig(Collections.emptyList());
        var nodeVersion = "v20.15.1"; // Latest LTS: https://nodejs.org/en/download/
        var npmVersion = "provided";

        var packageName = "com.mycompany.app";

        try {
            factory.getNodeInstaller(proxyConfig)
                    .setNodeVersion(nodeVersion)
                    .setNodeDownloadRoot("https://nodejs.org/dist/")
                    .setNpmVersion(npmVersion)
                    .install();
            factory.getNPMInstaller(proxyConfig)
                    .setNodeVersion(nodeVersion)
                    .setNpmVersion(npmVersion)
                    .setNpmDownloadRoot("https://registry.npmjs.org/npm/-/")
                    .install();
        } catch (InstallationException ie) {
            throw new CodeGenException("Failed to install node or npm", ie);
        }

        try {
            var npmRunner = factory.getNpmRunner(proxyConfig, null /* TODO: fixme */);
            npmRunner.execute("install @asyncapi/modelina", new HashMap<>() /* TODO: fixme */);

            // running Modelina as a Node library, alternatively the runnable process is available at:
            // node_modules/@asyncapi/cli/bin/run

            // writing to disk a JS script
            File jsFile = workingDirectory.toPath().resolve("exec-modelina.js").toFile();
            FileWriter fileWriter = new FileWriter(jsFile);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            var outPath = outputDir;
            for (var p : packageName.split("\\.")) {
                outPath = outPath.resolve(p);
            }
            printWriter
                    .println(execModelinaScript(asyncApiSpec.toFile().getAbsolutePath(), outPath.toFile().getAbsolutePath(),
                            packageName));
            printWriter.flush();
            printWriter.close();

            npmRunner.execute("exec node " + jsFile.getAbsolutePath(), new HashMap<>() /* TODO: fixme */);
        } catch (TaskRunnerException e) {
            throw new CodeGenException("Failed to install the asyncapi CLI", e);
        } catch (IOException e) {
            throw new CodeGenException("Failed to install the asyncapi CLI", e);
        }

    }

    private String execModelinaScript(String input, String outdir, String packageName) {
        return "const { JAVA_JACKSON_PRESET, JavaFileGenerator } = require(\"@asyncapi/modelina\");\n" +
                "(async () => {\n" +
                "const generator = new JavaFileGenerator({ presets: [JAVA_JACKSON_PRESET] });\n" +
                "const result = await generator.generateToFiles(\"file://" + input + "\", \"" + outdir
                + "\", { packageName: \"" + packageName + "\" });\n" +
                "console.log(result);\n" +
                "})()\n";
    }

    //    const { JAVA_JACKSON_PRESET, JavaFileGenerator } = require("@asyncapi/modelina");
    //(async () => {
    //    const generator = new JavaFileGenerator({ presets: [JAVA_JACKSON_PRESET] })
    //    const result = await generator.generateToFiles(
    //        "/home/aperuffo/workspace/quarkus-modelina/integration-tests/basic/src/main/asyncapi/asyncapi.json", "/home/aperuffo/workspace/quarkus-modelina/integration-tests/basic/target/generated-sources/modelina");
    //    console.log(result);
    //})();
}
