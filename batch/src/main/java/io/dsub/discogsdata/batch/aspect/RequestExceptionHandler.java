package io.dsub.discogsdata.batch.aspect;

import io.dsub.discogsdata.batch.exception.DumpNotFoundException;
import io.dsub.discogsdata.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.JobParametersNotFoundException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionIsRunningException;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Log4j2
@Component
@ControllerAdvice(basePackages = {"io.dsub.discogsdata"})
@RequiredArgsConstructor
public class RequestExceptionHandler extends ResponseEntityExceptionHandler {

    private HttpServletRequest givenRequest() {
        return ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes())
                .getRequest();
    }

    private URI getRequestURI() {
        return URI.create(givenRequest().getRequestURI());
    }

    private ResponseEntity<Object> makeResponse(String exceptionReason, HttpStatus httpStatus) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", exceptionReason);
        responseBody.put("time", OffsetDateTime.now(ZoneId.of("UTC")).toString());
        return ResponseEntity.status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .location(getRequestURI())
                .body(responseBody);
    }

    private ResponseEntity<Object> makeResponse(String exceptionReason, HttpStatus httpStatus, HttpServletRequest request) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", exceptionReason);
        responseBody.put("time", OffsetDateTime.now(ZoneId.of("UTC")).toString());

        return ResponseEntity.status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .location(URI.create(request.getRequestURI()))
                .body(responseBody);
    }

    private ResponseEntity<Object> makeResponse(BaseException e) {
        return makeResponse(e.getMessage(), e.getHttpStatus());
    }

    @ExceptionHandler({BaseException.class})
    public ResponseEntity<?> handleBaseException(BaseException e) {
        return makeResponse(e);
    }


    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String cause = String.format("Parameter type mismatch: [%s] expected type: [%s]", ex.getValue(), Objects.requireNonNull(ex.getRequiredType()).getSimpleName());
        return makeResponse(cause, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler({
            JobParametersInvalidException.class,
            JobParametersNotFoundException.class,
            JobInstanceAlreadyExistsException.class,
            JobExecutionNotRunningException.class,
            JobInterruptedException.class,
            JobExecutionAlreadyCompleteException.class,
            javax.batch.operations.NoSuchJobExecutionException.class,
            org.springframework.batch.core.launch.NoSuchJobExecutionException.class,
            JobInstanceAlreadyCompleteException.class,
            JobExecutionIsRunningException.class
    })

    public ResponseEntity<?> handleBatchException(Exception e, HttpServletRequest request) {
        return makeResponse(e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String cause = ex.getMessage();
        if (ex instanceof NoSuchJobException || ex instanceof DumpNotFoundException) {
            return makeResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
        }

        if (ex instanceof JobParametersInvalidException || ex instanceof JobParametersNotFoundException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cause);
        }

        if (ex instanceof JobInstanceAlreadyExistsException ||
                ex instanceof JobExecutionNotRunningException ||
                ex instanceof JobInterruptedException ||
                ex instanceof JobExecutionAlreadyCompleteException ||
                ex instanceof JobExecutionIsRunningException) {
            return makeResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        if (ex instanceof SQLException) {
            SQLException sqlException = (SQLException) ex;
            while (sqlException.getNextException() != null) {
                sqlException = sqlException.getNextException();
            }
            return makeResponse(sqlException.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return makeResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
