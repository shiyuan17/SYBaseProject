package com.company.cli.command;

import com.company.cli.database.DatabaseCommand;
import com.company.cli.command.health.HealthCommand;
import com.company.cli.command.user.UserCommand;
import com.company.cli.command.version.VersionCommand;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Component
@Command(
    name = "sybase",
    description = "SY Base Project standard CLI entrypoint.",
    mixinStandardHelpOptions = true,
    subcommands = {
        VersionCommand.class,
        HealthCommand.class,
        UserCommand.class,
        DatabaseCommand.class
    }
)
public class RootCommand implements Runnable {

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}
