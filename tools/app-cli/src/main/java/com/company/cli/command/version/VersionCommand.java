package com.company.cli.command.version;

import com.company.cli.support.CliOutputWriter;
import com.company.cli.support.OutputOptionMixin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.Optional;
import java.util.concurrent.Callable;

@Component
@Command(name = "version", description = "Print CLI version and runtime information.")
public class VersionCommand implements Callable<Integer> {

    private final CliOutputWriter outputWriter;
    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @Mixin
    private OutputOptionMixin outputOptionMixin;

    @Spec
    private CommandSpec spec;

    public VersionCommand(CliOutputWriter outputWriter,
                          Environment environment,
                          ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.outputWriter = outputWriter;
        this.environment = environment;
        this.buildPropertiesProvider = buildPropertiesProvider;
    }

    @Override
    public Integer call() throws Exception {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        VersionView view = new VersionView(
            environment.getProperty("spring.application.name", "app-cli"),
            Optional.ofNullable(buildProperties).map(BuildProperties::getVersion).orElse("dev"),
            System.getProperty("java.version"),
            Optional.ofNullable(buildProperties).map(it -> String.valueOf(it.getTime())).orElse("unknown")
        );

        outputWriter.write(
            spec,
            outputOptionMixin.getOutputFormat(),
            view,
            """
                application: %s
                version: %s
                javaVersion: %s
                buildTime: %s
                """.formatted(view.application(), view.version(), view.javaVersion(), view.buildTime()).trim());
        return 0;
    }

    private record VersionView(String application, String version, String javaVersion, String buildTime) {
    }
}

