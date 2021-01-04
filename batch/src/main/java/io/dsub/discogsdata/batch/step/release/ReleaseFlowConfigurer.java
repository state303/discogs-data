package io.dsub.discogsdata.batch.step.release;

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
public class ReleaseFlowConfigurer {

    private final DumpService dumpService;
    private final StepBuilderFactory stepBuilderFactory;
    private final Step releaseArtistStep;
    private final Step releaseCreditedArtistStep;
    private final Step releaseStep;
    private final Step releaseWorkStep;
    private final Step releaseVideoStep;

    @Bean
    @JobScope
    public Flow releaseFlow(@Value("#{jobParameters['release']}") String etag) {
        if (etag == null) {
            log.debug("etag is empty. skipping...");
            return new FlowBuilder<Flow>(this.toString()).build();
        }

        return new FlowBuilder<Flow>("releaseFlow " + etag)
                .start(releaseSourceStep(null))
                .next(releaseStep)
                .next(releaseCreditedArtistStep)
                .next(releaseArtistStep)
                .next(releaseWorkStep)
                .next(releaseVideoStep)
                .next(releaseSourceCleanupStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step releaseSourceStep(@Value("#{jobParameters['release']}") String etag) {
        DiscogsDump dump;
        if (etag == null) {
            dump = dumpService.getMostRecentDumpByType(DumpType.ARTIST);
            etag = dump.getEtag();
        } else {
            dump = dumpService.getDumpByEtag(etag);
        }
        return stepBuilderFactory.get("releaseSourceStep " + etag)
                .tasklet(new FileCopyTasklet(dump))
                .throttleLimit(1)
                .build();
    }

    @Bean
    @JobScope
    public Step releaseSourceCleanupStep(@Value("#{jobParameters['release']}") String etag) {
        DiscogsDump dump = dumpService.getDumpByEtag(etag);
        return stepBuilderFactory.get("releaseSourceCleanupStep " + etag)
                .tasklet(new FileCleanupTasklet(dump))
                .throttleLimit(1)
                .build();
    }
}
