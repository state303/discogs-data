package io.dsub.discogsdata.batch.step.dump;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.jsr.step.DecisionStep;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Configuration;

import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;

@Configuration
@RequiredArgsConstructor
public class DumpStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;


}
