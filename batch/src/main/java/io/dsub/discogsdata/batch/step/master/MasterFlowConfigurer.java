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
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MasterFlowConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final DumpService dumpService;

    private final Step masterJdbcPreStep;
    private final Step masterJdbcStep;

    @Bean
    @JobScope
    public Flow masterFlow(@Value("#{jobParameters['master']}") String etag) {
        if (etag == null) {
            log.debug("etag is empty. skipping...");
            return new FlowBuilder<Flow>(this.toString()).build();
        }

        return new FlowBuilder<Flow>("masterFlow " + etag)
                .start(masterSourceStep(null))
                .next(masterJdbcPreStep)
                .next(masterJdbcStep)
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
}
