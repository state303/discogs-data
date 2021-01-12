package io.dsub.discogsdata.batch.step.artist.jpa;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.step.artist.ArtistDataProcessor;
import io.dsub.discogsdata.batch.step.artist.ArtistDumpReader;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.persistence.EntityManagerFactory;
import java.util.concurrent.Future;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArtistJpaStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final DumpService dumpService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ArtistDataProcessor artistDataProcessor;
    private final EntityManagerFactory emf;
    private final ArtistDumpReader artistDumpReader;

    private String etag;

    @Bean
    @JobScope
    public Step artistJpaStep(@Value("#{jobParameters['artist']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {

        this.etag = etag != null ? etag : dumpService.getMostRecentDumpByType(DumpType.ARTIST).getEtag();

        return stepBuilderFactory
                .get("artistJpaStep " + etag)
                .<XmlArtist, Future<Artist>>chunk(chunkSize)
                .reader(artistDumpReader)
                .processor(asyncArtistProcessor())
                .writer(asyncArtistWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public AsyncItemProcessor<XmlArtist, Artist> asyncArtistProcessor() {
        AsyncItemProcessor<XmlArtist, Artist> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(item -> {
            XmlArtist artist = artistDataProcessor.process(item);
            return artist == null ? null : artist.toArtist();
        });
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemWriter<Artist> asyncArtistWriter() {
        JpaItemWriter<Artist> jpaItemWriter = new JpaItemWriterBuilder<Artist>()
                .usePersist(true)
                .entityManagerFactory(emf)
                .build();
        AsyncItemWriter<Artist> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(jpaItemWriter);
        return asyncWriter;
    }
}
