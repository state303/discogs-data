package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.batch.reader.CustomStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterVideo;
import io.dsub.discogsdata.common.repository.master.MasterVideoRepository;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class MasterVideoStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final MasterVideoRepository masterVideoRepository;
    private final DumpService dumpService;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Bean
    @JobScope
    public Step masterVideoStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory.get("masterVideoStep " + etag)
                .<XmlMaster, Future<List<MasterVideo>>>chunk(chunkSize)
                .reader(masterVideoItemReader(null))
                .processor(asyncMasterVideoProcessor())
                .writer(asyncMasterVideoWriter())
                .taskExecutor(taskExecutor)
                .throttleLimit(10)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<XmlMaster> masterVideoItemReader(@Value("#{jobParameters['master']}") String etag) throws Exception {
        return new CustomStaxEventItemReader<>(XmlMaster.class, dumpService.getDumpByEtag(etag));
    }

    @Bean
    @StepScope
    public AsyncItemProcessor<XmlMaster, List<MasterVideo>> asyncMasterVideoProcessor() {
        AsyncItemProcessor<XmlMaster, List<MasterVideo>> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.setDelegate(xmlMaster -> xmlMaster.getVideos().stream()
                .map(XmlMaster.Video::toVideoEntity)
                .peek(video -> video.setMaster(Master.builder().id(xmlMaster.getId()).build()))
                .collect(Collectors.toList()));
        return asyncItemProcessor;
    }

    @Bean
    @StepScope
    public AsyncItemWriter<List<MasterVideo>> asyncMasterVideoWriter() throws Exception {
        RepositoryItemWriter<List<MasterVideo>> syncMasterVideoRepositoryWriter = new RepositoryItemWriter<>() {
            @Override
            protected void doWrite(List<? extends List<MasterVideo>> items) {
                items.forEach(masterVideoRepository::saveAll);
            }
        };
        AsyncItemWriter<List<MasterVideo>> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(syncMasterVideoRepositoryWriter);
        asyncItemWriter.afterPropertiesSet();
        return asyncItemWriter;
    }
}
