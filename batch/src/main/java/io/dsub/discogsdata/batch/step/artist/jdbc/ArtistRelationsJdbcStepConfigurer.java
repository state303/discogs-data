package io.dsub.discogsdata.batch.step.artist.jdbc;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlArtistRelation;
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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static io.dsub.discogsdata.batch.step.XmlItemProcessor.IGNORE_FLAG;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArtistRelationsJdbcStepConfigurer {

    private final StepBuilderFactory sbf;
    private final DumpService dumpService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ArtistRelationsJdbcItemWriter artistRelationsJdbcItemWriter;
    private final ArtistRepository artistRepository;

    @Bean
    @JobScope
    public Step artistRelationsJdbcStep(@Value("#{jobParameters['artist']}") String etag, @Value("#{jobParameters['chunkSize']}") Integer chunkSize) throws Exception {
        return sbf.get("artistRelationsJdbcStep " + etag)
                .<XmlArtistRelation, Future<XmlArtistRelation>>chunk(chunkSize)
                .reader(artistRelationProgressBarStaxEventItemReader(null))
                .processor(asyncArtistRelationsJdbcProcessor())
                .writer(asyncArtistRelationsJdbcWriter())
                .build();
    }

    @Bean
    @StepScope
    public AsyncItemWriter<XmlArtistRelation> asyncArtistRelationsJdbcWriter() throws Exception {
        AsyncItemWriter<XmlArtistRelation> writer = new AsyncItemWriter<>();
        writer.setDelegate(artistRelationsJdbcItemWriter);
        writer.afterPropertiesSet();
        return writer;
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<XmlArtistRelation, XmlArtistRelation> asyncArtistRelationsJdbcProcessor() throws Exception {
        ItemProcessor<XmlArtistRelation, XmlArtistRelation> processor = item -> {
            item.setAliases(filterMissingArtistReferences(item.getAliases()));
            item.setGroups(filterMissingArtistReferences(item.getGroups()));
            item.setMembers(filterMissingArtistReferences(item.getMembers()));
            return item;
        };
        AsyncItemProcessor<XmlArtistRelation, XmlArtistRelation> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(processor);
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.afterPropertiesSet();
        return asyncItemProcessor;
    }

    @Bean
    @StepScope
    public ProgressBarStaxEventItemReader<XmlArtistRelation> artistRelationProgressBarStaxEventItemReader(@Value("#{jobParameters['artist']}") String etag) throws Exception {
        ProgressBarStaxEventItemReader<XmlArtistRelation> reader =
                new ProgressBarStaxEventItemReader<>(XmlArtistRelation.class, dumpService.getDumpByEtag(etag));
        reader.afterPropertiesSet();
        reader.setSaveState(false);
        return reader;
    }

    private <T extends XmlArtistRelation.ArtistRef> List<T> filterMissingArtistReferences(List<T> refs) {
        return refs.stream().filter(item -> artistRepository.existsById(item.getId())).collect(Collectors.toList());
    }
}
