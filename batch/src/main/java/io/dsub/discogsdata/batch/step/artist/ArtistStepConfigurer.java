package io.dsub.discogsdata.batch.step.artist;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.reader.CustomStaxEventItemReader;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Future;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArtistStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final DumpService dumpService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final XmlArtistReadListener readListener;
    private final ArtistRepository artistRepository;

    private String etag;

    @Bean
    @JobScope
    public Step artistStep(@Value("#{jobParameters['artist']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {

        this.etag = etag != null ? etag : dumpService.getMostRecentDumpByType(DumpType.ARTIST).getEtag();

        return stepBuilderFactory
                .get("artistStep " + etag)
                .<XmlArtist, Future<Artist>>chunk(chunkSize)
                .reader(artistReader())
                .processor(asyncArtistProcessor())
                .writer(asyncArtistWriter())
                .listener(readListener)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public AsyncItemProcessor<XmlArtist, Artist> asyncArtistProcessor() {
        AsyncItemProcessor<XmlArtist, Artist> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(XmlArtist::toEntity);
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemWriter<Artist> asyncArtistWriter() throws Exception {
        RepositoryItemWriter<Artist> syncWriter = new RepositoryItemWriter<>();
        syncWriter.setRepository(artistRepository);
        syncWriter.afterPropertiesSet();
        AsyncItemWriter<Artist> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(syncWriter);
        return asyncWriter;
    }

    @Bean
    @StepScope
    public CustomStaxEventItemReader<XmlArtist> artistReader() throws Exception {
        DiscogsDump artistDump = dumpService.getDumpByEtag(etag);
        return new CustomStaxEventItemReader<>(XmlArtist.class, artistDump);
    }
}
