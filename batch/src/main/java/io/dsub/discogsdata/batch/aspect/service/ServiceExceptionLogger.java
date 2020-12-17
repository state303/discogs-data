package io.dsub.discogsdata.batch.aspect.service;

import io.dsub.discogsdata.batch.aspect.ServiceAspect;
import io.dsub.discogsdata.batch.exception.DumpNotFoundException;
import io.dsub.discogsdata.batch.exception.UnknownDumpTypeException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class ServiceExceptionLogger extends ServiceAspect {

    @Around("services()")
    public Object handleError(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed(joinPoint.getArgs());
        } catch (JobParametersInvalidException | JobRestartException | JobInstanceAlreadyCompleteException | NoSuchJobExecutionException | NoSuchJobException | UnknownDumpTypeException | DumpNotFoundException e) {
            Map<String, Object> args = Arrays.stream(joinPoint.getArgs())
                    .collect(Collectors.toMap(
                            o -> o.getClass().getSimpleName(),
                            o -> String.valueOf(o).replaceAll("[\n\t ]", "")));
            log.debug("{} thrown from class {{}} on method {{} {}} with params {}",
                    e.getClass().getSimpleName(),
                    joinPoint.getSignature().getDeclaringType().getName(),
                    Modifier.toString(joinPoint.getSignature().getModifiers()),
                    joinPoint.getSignature().getName(),
                    args);

            throw e;
        }
    }

}

