package com.company.cli.support;

import com.company.cli.command.RootCommand;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.PrintWriter;

@Component
public class CliCommandLineFactory {

    private final RootCommand rootCommand;
    private final SpringCommandFactory springCommandFactory;
    private final CliExecutionExceptionHandler executionExceptionHandler;
    private final CliParameterExceptionHandler parameterExceptionHandler;

    public CliCommandLineFactory(RootCommand rootCommand,
                                 SpringCommandFactory springCommandFactory,
                                 CliExecutionExceptionHandler executionExceptionHandler,
                                 CliParameterExceptionHandler parameterExceptionHandler) {
        this.rootCommand = rootCommand;
        this.springCommandFactory = springCommandFactory;
        this.executionExceptionHandler = executionExceptionHandler;
        this.parameterExceptionHandler = parameterExceptionHandler;
    }

    public CommandLine create(PrintWriter out, PrintWriter err) {
        CommandLine commandLine = new CommandLine(rootCommand, springCommandFactory);
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        commandLine.setOut(out);
        commandLine.setErr(err);
        commandLine.setExecutionExceptionHandler(executionExceptionHandler);
        commandLine.setParameterExceptionHandler(parameterExceptionHandler);
        return commandLine;
    }
}
