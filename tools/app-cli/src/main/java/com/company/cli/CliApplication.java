package com.company.cli;

import com.company.cli.command.RootCommand;
import com.company.cli.support.CliCommandLineFactory;
import com.company.user.application.service.CreateUserAppService;
import com.company.user.infrastructure.config.UserDomainConfiguration;
import com.company.user.infrastructure.convert.UserInfrastructureConverter;
import com.company.user.infrastructure.observability.ObservedOperationAspect;
import com.company.user.infrastructure.persistence.InMemoryUserRepository;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.PrintWriter;

@SpringBootApplication(scanBasePackageClasses = {
    CliApplication.class,
    RootCommand.class,
    CreateUserAppService.class,
    InMemoryUserRepository.class,
    UserInfrastructureConverter.class,
    ObservedOperationAspect.class,
    UserDomainConfiguration.class
})
public class CliApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(CliApplication.class)
            .web(WebApplicationType.NONE)
            .logStartupInfo(false)
            .run(args);

        int exitCode;
        try {
            CliCommandLineFactory commandLineFactory = context.getBean(CliCommandLineFactory.class);
            exitCode = commandLineFactory
                .create(new PrintWriter(System.out, true), new PrintWriter(System.err, true))
                .execute(args);
        } finally {
            context.close();
        }

        System.exit(exitCode);
    }
}

