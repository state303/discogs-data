package io.dsub.discogsdata.batch.step.artist;

import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.reader.ProgressBarStaxEventItemReader;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class ArtistDumpReader extends ProgressBarStaxEventItemReader<XmlArtist> {
    public ArtistDumpReader(@Value("#{jobParameters['artist']}") String etag, DumpService dumpService) throws Exception {
        super(XmlArtist.class, dumpService.getDumpByEtag(etag));
    }
}
