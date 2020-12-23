package io.dsub.discogsdata.batch.step;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.batch.process.RepositoriesHolderBean;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.process.XmlObjectReadListener;
import io.dsub.discogsdata.batch.reader.CustomStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.repository.CrudRepository;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

@Configuration
@RequiredArgsConstructor
public class ArtistStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final DumpService dumpService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final RelationsHolder relationsHolder;
    private final RepositoriesHolderBean repositoriesHolderBean;
    private final XmlObjectReadListener xmlObjectReadListener;

    @Bean
    @JobScope
    public Step asyncArtistStep(@Value("#{jobParameters['artist']}") String etag) throws Exception {
        return stepBuilderFactory
                .get("artistStep " + etag)
                .<XmlArtist, Future<Artist>>chunk(5000)
                .reader(artistReader(null))
                .processor(asyncArtistProcessor())
                .writer(asyncArtistWriter())
                .listener(xmlObjectReadListener)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @JobScope
    public Flow artistFlow(@Value("#{jobParameters['artist']}") String etag) throws Exception {
        return new FlowBuilder<Flow>("artistFlow " + etag)
                .start(asyncArtistStep(null))
                .next(asyncArtistGroupStep(null))
                .build();
    }

    @Bean
    public AsyncItemProcessor<XmlArtist, Artist> asyncArtistProcessor() {
        AsyncItemProcessor<XmlArtist, Artist> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(artistProcessor());
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemWriter<Artist> asyncArtistWriter() throws Exception {
        AsyncItemWriter<Artist> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(artistWriter());
        return asyncItemWriter;
    }

    @Bean
    public ItemProcessor<XmlArtist, Artist> artistProcessor() {
        return XmlArtist::toEntity;
    }

    @Bean
    @StepScope
    public CustomStaxEventItemReader<XmlArtist> artistReader(@Value("#{jobParameters['artist']}") String etag) throws Exception {
        DiscogsDump artistDump = dumpService.getDumpByEtag(etag);
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(XmlArtist.class);
        jaxb2Marshaller.afterPropertiesSet();
        StaxEventItemReader<XmlArtist> reader = new StaxEventItemReaderBuilder<XmlArtist>()
                .resource(new InputStreamResource(new GZIPInputStream(new URL(artistDump.getResourceUrl()).openStream())))
                .name(artistDump.getRootElementName() + " reader: " + artistDump.getEtag())
                .addFragmentRootElements("artist")
                .unmarshaller(jaxb2Marshaller)
                .build();
        return new CustomStaxEventItemReader<>(reader);
    }

    @Bean
    public RepositoryItemWriter<Artist> artistWriter() throws Exception {
        RepositoryItemWriter<Artist> writer = new RepositoryItemWriter<>();
        writer.setRepository(repositoriesHolderBean.getArtistRepository());
        writer.afterPropertiesSet();
        return writer;
    }

    @Bean
    @JobScope
    public Step asyncArtistGroupStep(@Value("#{jobParameters['artist']}") String etag) throws Exception {
        return stepBuilderFactory
                .get("artistGroupStep " + etag)
                .<SimpleRelation, Future<ArtistGroup>>chunk(5000)
                .reader(artistGroupItemReader())
                .processor(artistGroupProcessor())
                .writer(artistGroupWriter())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<SimpleRelation> artistGroupItemReader() {
        ConcurrentLinkedQueue<SimpleRelation> queue = relationsHolder.pullSimpleRelationsQueue(XmlArtist.Group.class);
        return queue::poll;
    }

    @Bean
    public AsyncItemWriter<ArtistGroup> artistGroupWriter() throws Exception {
        AsyncItemWriter<ArtistGroup> writer = new AsyncItemWriter<>();
        writer.setDelegate(artistGroupRepositoryItemWriter());
        writer.afterPropertiesSet();
        return writer;
    }

    @Bean
    public RepositoryItemWriter<ArtistGroup> artistGroupRepositoryItemWriter() throws Exception {
        RepositoryItemWriter<ArtistGroup> writer = new RepositoryItemWriter<>();
        writer.setRepository(repositoriesHolderBean.getArtistGroupRepository());
        writer.afterPropertiesSet();
        return writer;
    }


    @Bean
    public AsyncItemProcessor<SimpleRelation, ArtistGroup> artistGroupProcessor() throws Exception {
        AsyncItemProcessor<SimpleRelation, ArtistGroup> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(simpleRelation -> {
            if (isValid(simpleRelation, repositoriesHolderBean.getArtistRepository())) {
                return simpleRelation.toArtistGroup();
            }
            return null;
        });
        asyncItemProcessor.afterPropertiesSet();
        return asyncItemProcessor;
    }

    private boolean isValid(SimpleRelation simpleRelation, CrudRepository<?, Long> repository) {
        return simpleRelation.getParentId() != null &&
                simpleRelation.getChildId() != null &&
                repository.existsById(simpleRelation.getParentId()) &&
                repository.existsById(simpleRelation.getChildId());
    }
}
