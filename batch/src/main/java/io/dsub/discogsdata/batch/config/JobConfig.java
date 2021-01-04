package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final JobParametersValidator discogsJobParametersValidator;
    private final Flow artistFlow;
    private final Flow labelFlow;
    private final Flow masterFlow;
    private final Flow releaseFlow;
    private final Step masterMainReleaseStep;

    @Bean
    public Job job() {
        return jobBuilderFactory.get("job")
                .incrementer(new RunIdIncrementer())
                .validator(discogsJobParametersValidator)
//                .start(artistFlow)
//                .start(labelFlow)
                .start(masterFlow)
                .next(releaseFlow)
                .next(masterMainReleaseStep)
                .end()
                .listener(new JobExecutionListener() {
                    public void beforeJob(JobExecution jobExecution) {
                    }
                    public void afterJob(JobExecution jobExecution) {
                        System.exit(0);
                    }
                })

                .build();
    }
}
