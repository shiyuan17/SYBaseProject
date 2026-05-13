package com.company.cli.support;

import com.company.common.core.enums.CommonErrorCode;
import com.company.common.core.exception.BaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

@Component
public class CliExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CliExecutionExceptionHandler.class);

    private final CliOutputWriter outputWriter;

    public CliExecutionExceptionHandler(CliOutputWriter outputWriter) {
        this.outputWriter = outputWriter;
    }

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) throws Exception {
        OutputFormat outputFormat = OutputFormat.from(parseResult);
        if (ex instanceof BaseException baseException) {
            log.warn("CLI business error, code={}, message={}",
                baseException.getErrorCode().code(), ex.getMessage());
            outputWriter.writeError(
                commandLine,
                outputFormat,
                new CliErrorResponse(baseException.getErrorCode().code(), ex.getMessage()));
            return 3;
        }

        log.error("CLI unexpected error", ex);
        outputWriter.writeError(
            commandLine,
            outputFormat,
            new CliErrorResponse(CommonErrorCode.INTERNAL_ERROR.code(), CommonErrorCode.INTERNAL_ERROR.message()));
        return 4;
    }
}

