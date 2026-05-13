package com.company.cli.command.user;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Component
@Command(
    name = "user",
    description = "User domain commands.",
    subcommands = {
        UserCreateCommand.class,
        UserGetCommand.class
    }
)
public class UserCommand implements Runnable {

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}
