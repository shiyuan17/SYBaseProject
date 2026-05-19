package com.company.cli.command.user;

import com.company.cli.support.CliOutputWriter;
import com.company.cli.support.OutputOptionMixin;
import com.company.user.application.query.GetUserByIdQuery;
import com.company.user.application.service.GetUserAppService;
import com.company.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Component
@Command(name = "get", description = "Get a user by id.")
@RequiredArgsConstructor
public class UserGetCommand implements Callable<Integer> {

    private final GetUserAppService getUserAppService;
    private final CliOutputWriter outputWriter;

    @Option(names = "--id", required = true, description = "User id.")
    private String userId;

    @Mixin
    private OutputOptionMixin outputOptionMixin;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        User user = getUserAppService.getById(new GetUserByIdQuery(userId));
        UserDetailView view = new UserDetailView(
            user.getId().value(),
            user.getName().value(),
            user.getEmail().value(),
            user.getStatus().name(),
            String.valueOf(user.getCreatedAt()));
        outputWriter.write(
            spec,
            outputOptionMixin.getOutputFormat(),
            view,
            """
                id: %s
                name: %s
                email: %s
                status: %s
                createdAt: %s
                """.formatted(
                view.id(),
                view.name(),
                view.email(),
                view.status(),
                view.createdAt()).trim());
        return 0;
    }

    private record UserDetailView(String id, String name, String email, String status, String createdAt) {
    }
}
