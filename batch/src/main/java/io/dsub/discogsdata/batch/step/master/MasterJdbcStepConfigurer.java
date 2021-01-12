package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class MasterJdbcStepConfigurer {
    private final StepBuilderFactory stepBuilderFactory;
    private final MasterDataReader masterDataReader;
    private final MasterJdbcItemWriter masterJdbcItemWriter;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Bean
    @JobScope
    public Step masterJdbcStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory
                .get("masterJdbcStep " + etag)
                .<XmlMaster, Future<XmlMaster>>chunk(chunkSize)
                .reader(masterDataReader)
                .processor(asyncMasterJdbcItemProcessor())
                .writer(asyncMasterJdbcItemWriter())
                .build();
    }

    @Bean
    public AsyncItemProcessor<XmlMaster, XmlMaster> asyncMasterJdbcItemProcessor() throws Exception {
        AsyncItemProcessor<XmlMaster, XmlMaster> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(item -> item);
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        asyncItemProcessor.afterPropertiesSet();
        return asyncItemProcessor;
    }

    @Bean
    public AsyncItemWriter<XmlMaster> asyncMasterJdbcItemWriter() {
        AsyncItemWriter<XmlMaster> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(masterJdbcItemWriter);
        return asyncItemWriter;
    }


}
