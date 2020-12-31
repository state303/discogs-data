package io.dsub.discogsdata.batch.step.artist;

import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.step.XmlObjectReadListener;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class XmlArtistReadListener extends XmlObjectReadListener<XmlArtist> {

    public XmlArtistReadListener(RelationsHolder relationsHolder) {
        super(relationsHolder);
    }

    @Override
    public void afterRead(XmlArtist item) {
        if (item.getProfile().contains(DO_NOT_USE_FLAG)) {
            return;
        }
        item.getRelations().forEach(
                ref -> relationsHolder.addSimpleRelation(
                        ref.getClass(), new SimpleRelation(item.getId(), ref.getId())));
    }
}
