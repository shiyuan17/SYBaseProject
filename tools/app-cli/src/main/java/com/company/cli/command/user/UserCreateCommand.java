package com.company.cli.command.user;

import com.company.cli.support.CliOutputWriter;
import com.company.cli.support.OutputOptionMixin;
import com.company.user.application.command.CreateUserCommand;
import com.company.user.application.service.CreateUserAppService;
import com.company.user.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Component
@Command(name = "create", description = "Create a user.")
@RequiredArgsConstructor
public class UserCreateCommand implements Callable<Integer> {

    private final CreateUserAppService createUserAppService;
    private final CliOutputWriter outputWriter;

    @Option(names = "--name", required = true, description = "User name.")
    private String name;

    @Option(names = "--email", required = true, description = "User email.")
    private String email;

    @Mixin
    private OutputOptionMixin outputOptionMixin;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        UserId userId = createUserAppService.create(new CreateUserCommand(name, email));
        UserCreateView view = new UserCreateView(userId.value());
        outputWriter.write(
            spec,
            outputOptionMixin.getOutputFormat(),
            view,
            "userId: %s".formatted(view.userId()));
        return 0;
    }

    private record UserCreateView(String userId) {
    }
}
