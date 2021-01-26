package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.step.FileCleanupTasklet;
import io.dsub.discogsdata.batch.step.FileCopyTasklet;
import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import io.dsub.discogsdata.batch.xml.object.XmlMasterSubData;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Future;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MasterJdbcFlowConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final DumpService dumpService;
    private final MasterDataReader masterDataReader;
    private final MasterJdbcItemWriter masterJdbcItemWriter;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final MasterJdbcPreStepWriter preStepWriter;

    @Bean
    @JobScope
    public Flow masterJdbcFlow(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {

        if (etag == null) {
            log.debug("etag for master dump is empty. skipping...");
            return new FlowBuilder<Flow>(this.toString()).build();
        }

        return new FlowBuilder<Flow>("executing master batch flow with etag " + etag)
                .start(masterSourceStep(null))
                .next(masterJdbcPreStep(etag, chunkSize))
                .next(masterJdbcStep(etag, chunkSize))
                .next(masterSourceCleanupStep(null))
                .build();
    }

    @Bean
    @JobScope
    public Step masterJdbcPreStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory
                .get("masterJdbcPreStep " + etag)
                .<XmlMasterSubData, XmlMasterSubData>chunk(chunkSize)
                .reader(new ProgressBarStaxEventItemReader<>(XmlMasterSubData.class, dumpService.getDumpByEtag(etag)))
                .writer(preStepWriter)
                .build();
    }

    @Bean
    @JobScope
    public Step masterJdbcStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {

        AsyncItemProcessor<XmlMaster, XmlMaster> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(item -> item);
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.afterPropertiesSet();

        AsyncItemWriter<XmlMaster> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(masterJdbcItemWriter);

        return stepBuilderFactory
                .get("masterJdbcStep " + etag)
                .<XmlMaster, Future<XmlMaster>>chunk(chunkSize)
                .reader(masterDataReader)
                .processor(asyncItemProcessor)
                .writer(asyncItemWriter)
                .build();
    }

    @Bean
    @JobScope
    public Step masterSourceStep(@Value("#{jobParameters['master']}") String etag) {
        DiscogsDump dump;
        if (etag == null) {
            dump = dumpService.getMostRecentDumpByType(DumpType.ARTIST);
            etag = dump.getEtag();
        } else {
            dump = dumpService.getDumpByEtag(etag);
        }
        return stepBuilderFactory.get("masterSourceStep " + etag)
                .tasklet(new FileCopyTasklet(dump))
                .throttleLimit(1)
                .build();
    }

    @Bean
    @JobScope
    public Step masterSourceCleanupStep(@Value("#{jobParameters['master']}") String etag) {
        return stepBuilderFactory.get("masterSourceCleanupStep")
                .tasklet(new FileCleanupTasklet(dumpService.getDumpByEtag(etag)))
                .throttleLimit(1)
                .build();
    }
}
