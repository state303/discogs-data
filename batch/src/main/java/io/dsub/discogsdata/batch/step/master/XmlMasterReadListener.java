package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.step.XmlObjectReadListener;
import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterGenre;
import io.dsub.discogsdata.common.entity.master.MasterStyle;
import io.dsub.discogsdata.common.entity.master.MasterVideo;
import org.springframework.stereotype.Component;

@Component
public class XmlMasterReadListener extends XmlObjectReadListener<XmlMaster> {
    public XmlMasterReadListener(DumpCache dumpCache) {
        super(dumpCache);
    }

    @Override
    public void afterRead(XmlMaster item) {
        item.getArtists().stream()
                .map(artistInfo -> new SimpleRelation(item.getId(), artistInfo.getId()))
                .forEach(relation -> dumpCache.addSimpleRelation(XmlMaster.ArtistInfo.class, relation));
        dumpCache.addSimpleRelation("masterMainRelease", new SimpleRelation(item.getId(), item.getMainRelease()));
//        for (XmlMaster.Video video : item.getVideos()) {
//            MasterVideo masterVideo = video.toVideoEntity();
//            masterVideo.setMaster(Master.builder().id(item.getId()).build());
//            dumpCache.addItem(MasterVideo.class, masterVideo);
//        }

        item.getGenres().forEach(genre -> {
            dumpCache.putGenre(genre, null);
            dumpCache.addSimpleRelation(MasterGenre.class, item.getId(), genre);
        });

        item.getStyles().forEach(style -> {
            dumpCache.putStyle(style, null);
            dumpCache.addSimpleRelation(MasterStyle.class, item.getId(), style);
        });
    }
}