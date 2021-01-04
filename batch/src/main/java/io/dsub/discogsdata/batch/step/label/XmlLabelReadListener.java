package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.step.XmlObjectReadListener;
import io.dsub.discogsdata.batch.xml.object.XmlLabel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class XmlLabelReadListener extends XmlObjectReadListener<XmlLabel> {

    public XmlLabelReadListener(DumpCache dumpCache) {
        super(dumpCache);
    }

    @Override
    public void afterRead(XmlLabel item) {
        if (item.getProfile() != null && item.getProfile().contains(DO_NOT_USE_FLAG)) {
            return;
        }
        if (item.getSubLabels() != null && item.getSubLabels().size() > 0) {
            item.getSubLabels().forEach(
                    ref -> dumpCache.addSimpleRelation(
                            ref.getClass(), new SimpleRelation(item.getId(), ref.getId())));
        }
    }
}
