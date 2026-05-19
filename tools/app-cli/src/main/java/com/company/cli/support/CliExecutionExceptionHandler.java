package com.company.cli.support;

import com.company.common.core.enums.CommonErrorCode;
import com.company.common.core.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

@Component
@Slf4j
@RequiredArgsConstructor
public class CliExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    private final CliOutputWriter outputWriter;

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
