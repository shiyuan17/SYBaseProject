package com.company.cli.command.health;

import com.company.cli.support.CliOutputWriter;
import com.company.cli.support.OutputOptionMixin;
import com.company.user.application.service.CreateUserAppService;
import com.company.user.application.service.GetUserAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "health", description = "Check CLI context and critical beans.")
@RequiredArgsConstructor
public class HealthCommand implements Callable<Integer> {

    private final CliOutputWriter outputWriter;
    private final ApplicationContext applicationContext;
    private final Environment environment;
    private final CreateUserAppService createUserAppService;
    private final GetUserAppService getUserAppService;

    @Mixin
    private OutputOptionMixin outputOptionMixin;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        HealthView view = new HealthView(
            "UP",
            environment.getProperty("spring.application.name", "app-cli"),
            applicationContext.getBeanDefinitionCount(),
            List.of(
                createUserAppService.getClass().getSimpleName(),
                getUserAppService.getClass().getSimpleName()
            )
        );

        outputWriter.write(
            spec,
            outputOptionMixin.getOutputFormat(),
            view,
            """
                status: %s
                application: %s
                beanCount: %s
                checks: %s
                """.formatted(view.status(), view.application(), view.beanCount(), String.join(", ", view.checks())).trim());
        return 0;
    }

    private record HealthView(String status, String application, int beanCount, List<String> checks) {
    }
}
