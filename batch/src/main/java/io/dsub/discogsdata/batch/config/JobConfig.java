package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.batch.process.RepositoriesHolderBean;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.process.XmlReadListener;
import io.dsub.discogsdata.batch.reader.CustomStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.GZIPInputStream;

@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class JobConfig {

    private final DumpService dumpService;
    private final ArtistRepository artistRepository;
    private final RelationsHolder relationsHolder;
    private final RepositoriesHolderBean repositoriesHolderBean;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobBuilderFactory jobBuilderFactory;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final XmlReadListener xmlReadListener;

    ////////////////////////////////////////////////////////////////////////////////////
    // JOBS AND STEPS
    ////////////////////////////////////////////////////////////////////////////////////

    @Bean
    public Job asyncJob() throws Exception {
        return jobBuilderFactory
                .get("Asynchronous Processing JOB + 1")
                .incrementer(new RunIdIncrementer())
                .flow(asyncArtistStep())
                .next(asyncArtistGroupStep())
                .end()
                .listener(new JobExecutionListener() {
                    public void beforeJob(JobExecution jobExecution) {}
                    public void afterJob(JobExecution jobExecution) {
                        System.exit(0);
                    }
                })
                .build();
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // ARTIST STEP, READER, PROCESSOR, WRITER
    ////////////////////////////////////////////////////////////////////////////////////

    @Bean
    public Step asyncArtistStep() throws Exception {
        return stepBuilderFactory
                .get("ArtistProcess : Read -> Process -> Write")
                .<XmlArtist, Future<Artist>>chunk(5000)
                .reader(artistReader())
                .processor(asyncProcessor())
                .writer(asyncWriter())
                .listener(xmlReadListener)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public Step asyncArtistGroupStep() throws Exception {
        return stepBuilderFactory
                .get("ArtistReferenceProcess : Read -> Process -> Write")
                .<SimpleRelation, Future<ArtistGroup>>chunk(5000)
                .reader(artistGroupItemReader())
                .processor(artistGroupProcessor())
                .writer(artistGroupWriter())
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        System.out.println(relationsHolder.pullSimpleRelationsQueue(XmlArtist.Group.class).size());
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        return null;
                    }
                })
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
            if (isValid(simpleRelation, artistRepository)) {
                return simpleRelation.toArtistGroup();
            }
            return null;
        });
        asyncItemProcessor.afterPropertiesSet();
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemProcessor<XmlArtist, Artist> asyncProcessor() {
        AsyncItemProcessor<XmlArtist, Artist> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(artistProcessor());
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemWriter<Artist> asyncWriter() throws Exception {
        AsyncItemWriter<Artist> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(artistWriter());
        return asyncItemWriter;
    }

    @Bean
    public ItemProcessor<XmlArtist, Artist> artistProcessor() {
        return XmlArtist::toEntity;
    }

    @Bean
    public ItemReader<XmlArtist> artistReader() throws Exception {
        DiscogsDump artistDump = dumpService.getMostRecentDumpByType(DumpType.ARTIST);
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(XmlArtist.class);
        jaxb2Marshaller.afterPropertiesSet();
        StaxEventItemReader<XmlArtist> reader = new StaxEventItemReaderBuilder<XmlArtist>()
                .resource(new InputStreamResource(new GZIPInputStream(new URL(artistDump.getResourceUrl()).openStream())))
                .name(artistDump.getRootElementName() + " reader: " + artistDump.getEtag())
                .addFragmentRootElements("artist")
                .unmarshaller(jaxb2Marshaller)
                .build();

        return new CustomStaxEventItemReader<>(reader, relationsHolder);
    }

    @Bean
    public RepositoryItemWriter<Artist> artistWriter() throws Exception {
        RepositoryItemWriter<Artist> writer = new RepositoryItemWriter<>();
        writer.setRepository(artistRepository);
        writer.afterPropertiesSet();
        return writer;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // ARTIST STEP, READER, PROCESSOR, WRITER
    ////////////////////////////////////////////////////////////////////////////////////

    private boolean isValid(SimpleRelation simpleRelation, CrudRepository<?, Long> repository) {
        return simpleRelation.getParentId() != null &&
                simpleRelation.getChildId() != null &&
                repository.existsById(simpleRelation.getParentId()) &&
                repository.existsById(simpleRelation.getChildId());
    }
}
