package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.step.FileCleanupTasklet;
import io.dsub.discogsdata.batch.step.FileCopyTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MasterFlowConfigurer {

    private final Step masterStep;
    private final Step masterArtistStep;
    private final Step masterGenreStep;
    private final Step masterStyleStep;
    private final Step masterVideoPreStep;
    private final Step masterVideoStep;
    private final TaskExecutor taskExecutor;
    private final DumpService dumpService;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    @JobScope
    public Flow masterFlow(@Value("#{jobParameters['master']}") String etag) {
        if (etag == null) {
            log.debug("etag is empty. skipping...");
            return new FlowBuilder<Flow>(this.toString()).build();
        }

        return new FlowBuilder<Flow>("masterFlow " + etag)
                .start(masterSourceStep(null))
                .next(masterStep)
                .split(taskExecutor)
                .add(masterArtistFlow(), masterStyleFlow(), masterGenreFlow(), masterVideoPreFlow())
                .next(masterVideoStep)
                .next(masterSourceCleanupStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step masterSourceStep(@Value("#{jobParameters['master']}") String etag) {
        DiscogsDump dump;
        if (etag == null) {
            dump = dumpService.getMostRecentDumpByType(DumpType.ARTIST);
            etag = dump.getEtag();
        } else {
            dump = dumpService.getDumpByEtag(etag);
        }
        return stepBuilderFactory.get("masterSourceStep " + etag)
                .tasklet(new FileCopyTasklet(dump))
                .throttleLimit(1)
                .build();
    }

    @Bean
    @JobScope
    public Step masterSourceCleanupStep(@Value("#{jobParameters['master']}") String etag) {
        return stepBuilderFactory.get("masterSourceCleanupStep")
                .tasklet(new FileCleanupTasklet(dumpService.getDumpByEtag(etag)))
                .throttleLimit(1)
                .build();
    }

    @Bean
    @StepScope
    public Flow masterArtistFlow() {
        return new FlowBuilder<Flow>("masterArtistFlow")
                .next(masterArtistStep)
                .build();
    }

    @Bean
    @StepScope
    public Flow masterVideoPreFlow() {
        return new FlowBuilder<Flow>("masterVideoPreFlow")
                .next(masterVideoPreStep)
                .build();
    }

    @Bean
    @StepScope
    public Flow masterStyleFlow() {
        return new FlowBuilder<Flow>("masterStyleFlow")
                .start(masterStyleStep)
                .build();
    }

    @Bean
    @StepScope
    public Flow masterGenreFlow() {
        return new FlowBuilder<Flow>("masterGenreFlow")
                .next(masterGenreStep)
                .build();
    }
}
