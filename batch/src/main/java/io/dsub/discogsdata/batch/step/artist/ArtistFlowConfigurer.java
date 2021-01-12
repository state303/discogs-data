package io.dsub.discogsdata.batch.step.artist;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.step.FileCleanupTasklet;
import io.dsub.discogsdata.batch.step.FileCopyTasklet;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.unit.DataSize;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.concurrent.Future;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ArtistFlowConfigurer {

    private final Step artistJdbcStep;
    private final Step artistRelationsJdbcStep;
    private final Step artistJpaStep;
    private final DumpService dumpService;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    @JobScope
    public Flow artistFlow(@Value("#{jobParameters['artist']}") String etag) {
        if (etag == null) {
            log.debug("etag is empty. skipping...");
            return new FlowBuilder<Flow>(this.toString()).build();
        }

        return new FlowBuilder<Flow>("artistFlow " + etag)
                .start(artistSourceStep(null))
                .next(artistJdbcStep)
                .next(artistRelationsJdbcStep)
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
    public Step artistSourceCleanupStep(@Value("#{jobParameters['artist']}") String etag) {
        return stepBuilderFactory.get("artistSourceCleanupStep")
                .tasklet(new FileCleanupTasklet(dumpService.getDumpByEtag(etag)))
                .throttleLimit(1)
                .build();
    }
}
