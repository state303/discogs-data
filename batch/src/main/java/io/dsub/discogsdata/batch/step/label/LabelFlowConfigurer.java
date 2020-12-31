package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
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

import java.io.IOException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LabelFlowConfigurer {

    private final Step labelStep;
    private final Step subLabelStep;
    private final DumpService dumpService;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    @JobScope
    public Flow labelFlow(@Value("#{jobParameters['label']}") String etag) throws IOException {

        if (etag == null) {
            log.debug("etag is empty. skipping...");
            return new FlowBuilder<Flow>(this.toString()).build();
        }

        return new FlowBuilder<Flow>("labelFlow " + etag)
                .start(labelSourceStep(null))
                .next(labelStep)
                .next(subLabelStep)
                .next(labelSourceCleanupStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step labelSourceStep(@Value("#{jobParameters['label']}") String etag) {
        DiscogsDump dump;
        if (etag == null) {
            dump = dumpService.getMostRecentDumpByType(DumpType.ARTIST);
            etag = dump.getEtag();
        } else {
            dump = dumpService.getDumpByEtag(etag);
        }
        return stepBuilderFactory.get("labelSourceStep " + etag)
                .tasklet(new FileCopyTasklet(dump))
                .throttleLimit(1)
                .build();
    }

    @Bean
    @JobScope
    public Step labelSourceCleanupStep(@Value("#{jobParameters['label']}") String etag) throws IOException {
        return stepBuilderFactory.get("labelSourceCleanupStep")
                .tasklet(new FileCopyTasklet(dumpService.getDumpByEtag(etag)))
                .throttleLimit(1)
                .build();
    }
}
