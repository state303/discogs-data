package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlLabel;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class LabelDumpReader extends ProgressBarStaxEventItemReader<XmlLabel> {
    public LabelDumpReader(@Value("#{jobParameters['label']}") String etag, DumpService dumpService) throws Exception {
        super(XmlLabel.class, dumpService.getDumpByEtag(etag));
    }
}
