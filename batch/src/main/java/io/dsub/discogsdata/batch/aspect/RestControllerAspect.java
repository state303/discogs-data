package io.dsub.discogsdata.batch.aspect;

import org.aspectj.lang.annotation.Pointcut;

public abstract class RestControllerAspect {
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllers() {
    }

    ;
}
