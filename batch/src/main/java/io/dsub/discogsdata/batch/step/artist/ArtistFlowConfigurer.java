package io.dsub.discogsdata.batch.step.artist;

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
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ArtistFlowConfigurer {

    private final Step artistStep;
    private final Step artistGroupStep;
    private final Step artistMemberStep;
    private final Step artistAliasStep;
    private final DumpService dumpService;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    @JobScope
    public Flow artistFlow(@Value("#{jobParameters['artist']}") String etag) throws IOException {
        if (etag == null) {
            log.debug("etag is empty. skipping...");
            return new FlowBuilder<Flow>(this.toString()).build();
        }
        return new FlowBuilder<Flow>("artistFlow " + etag)
                .start(artistSourceStep(null))
                .next(artistStep)
                .next(artistGroupStep)
                .next(artistMemberStep)
                .next(artistAliasStep)
                .next(artistSourceCleanupStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step artistSourceStep(@Value("#{jobParameters['artist']}") String etag) {
        DiscogsDump dump;
        if (etag == null) {
            dump = dumpService.getMostRecentDumpByType(DumpType.ARTIST);
            etag = dump.getEtag();
        } else {
            dump = dumpService.getDumpByEtag(etag);
        }
        return stepBuilderFactory.get("artistSourceStep " + etag)
                .tasklet(new FileCopyTasklet(dump))
                .throttleLimit(1)
                .build();
    }

    @Bean
    @JobScope
    public Step artistSourceCleanupStep(@Value("#{jobParameters['artist']}") String etag) throws IOException {
        return stepBuilderFactory.get("artistSourceCleanupStep")
                .tasklet(new FileCopyTasklet(dumpService.getDumpByEtag(etag)))
                .throttleLimit(1)
                .build();
    }
}
