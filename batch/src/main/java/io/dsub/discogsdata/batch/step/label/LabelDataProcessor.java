package io.dsub.discogsdata.batch.step.label;

import io.dsub.discogsdata.batch.step.XmlItemProcessor;
import io.dsub.discogsdata.batch.xml.object.XmlLabel;
import org.springframework.stereotype.Component;

@Component
public class LabelDataProcessor implements XmlItemProcessor<XmlLabel, XmlLabel> {
    @Override
    public XmlLabel process(XmlLabel item) throws Exception {
        if (assertNotNullAndBlank(item.getProfile())) {
            item.setProfile(null);
        }

        if (assertNotNullAndBlank(item.getDataQuality())) {
            item.setDataQuality(null);
        }

        if (assertNotNullAndBlank(item.getContactInfo())) {
            item.setContactInfo(null);
        }

        if (assertNotNullAndBlank(item.getName())) {
            item.setName(null);
        }
        return item;
    }
}
