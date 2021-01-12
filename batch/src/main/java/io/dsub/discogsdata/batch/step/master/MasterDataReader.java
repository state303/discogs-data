package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class MasterDataReader extends ProgressBarStaxEventItemReader<XmlMaster> {
    public MasterDataReader(@Value("#{jobParameters['master']}") String etag, DumpService dumpService) throws Exception {
        super(XmlMaster.class, dumpService.getDumpByEtag(etag));
    }
}