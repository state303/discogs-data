package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.entity.DiscogsDump;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import io.dsub.discogsdata.batch.xml.object.XmlMasterSubData;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class MasterJdbcPreStepConfigurer {

    private final StepBuilderFactory stepBuilderFactory;
    private final DumpService dumpService;
    private final MasterJdbcPreStepWriter preStepWriter;

    @Bean
    @JobScope
    public Step masterJdbcPreStep(@Value("#{jobParameters['master']}") String etag, @Value("#{jobParameters['chunkSize']}") int chunkSize) throws Exception {
        return stepBuilderFactory
                .get("masterJdbcPreStep " + etag)
                .<XmlMasterSubData, XmlMasterSubData>chunk(chunkSize)
                .reader(masterJdbcPreStepReader(null))
                .writer(preStepWriter)
                .build();
    }

    @Bean
    @StepScope
    public ProgressBarStaxEventItemReader<XmlMasterSubData> masterJdbcPreStepReader(@Value("#{jobParameters['master']}") String etag) throws Exception {
        return new ProgressBarStaxEventItemReader<>(XmlMasterSubData.class, dumpService.getDumpByEtag(etag));
    }
}
