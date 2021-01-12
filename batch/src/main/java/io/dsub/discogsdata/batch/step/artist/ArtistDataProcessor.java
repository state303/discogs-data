package io.dsub.discogsdata.batch.step.artist;

import io.dsub.discogsdata.batch.step.XmlItemProcessor;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import org.springframework.stereotype.Component;

@Component
public class ArtistDataProcessor implements XmlItemProcessor<XmlArtist, XmlArtist> {
    @Override
    public XmlArtist process(XmlArtist xmlArtist) throws Exception {
        if (assertNotNullAndBlank(xmlArtist.getProfile())) {
            xmlArtist.setProfile(null);
        }
        if (assertNotNullAndBlank(xmlArtist.getRealName())) {
            xmlArtist.setRealName(null);
        }
        if (assertNotNullAndBlank(xmlArtist.getName())) {
            xmlArtist.setName(null);
        }
        if (assertNotNullAndBlank(xmlArtist.getDataQuality())) {
            xmlArtist.setDataQuality(null);
        }
        return xmlArtist;
    }
}
