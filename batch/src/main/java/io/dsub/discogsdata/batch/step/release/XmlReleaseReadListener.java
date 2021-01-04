package io.dsub.discogsdata.batch.step.release;

import io.dsub.discogsdata.batch.process.DumpCache;
import io.dsub.discogsdata.batch.step.XmlObjectReadListener;
import io.dsub.discogsdata.batch.xml.object.XmlRelease;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.label.Label;
import io.dsub.discogsdata.common.entity.release.*;
import org.springframework.stereotype.Component;

@Component
public class XmlReleaseReadListener extends XmlObjectReadListener<XmlRelease> {

    public XmlReleaseReadListener(DumpCache dumpCache) {
        super(dumpCache);
    }

    @Override
    public void afterRead(XmlRelease item) {
        for (XmlRelease.CreditedArtist creditedArtist : item.getCreditedArtists()) {
            ReleaseCreditedArtist releaseCreditedArtist = ReleaseCreditedArtist.builder()
                    .artist(Artist.builder().id(creditedArtist.getId()).build())
                    .releaseItem(ReleaseItem.builder().id(item.getReleaseId()).build())
                    .role(creditedArtist.getRole())
                    .build();
            dumpCache.addItem(ReleaseCreditedArtist.class, releaseCreditedArtist);
        }
        item.getAlbumArtists().forEach(albumArtist -> {
            ReleaseArtist releaseArtist = ReleaseArtist.builder()
                    .artist(Artist.builder().id(albumArtist.getId()).build())
                    .releaseItem(ReleaseItem.builder().id(item.getReleaseId()).build())
                    .build();
            dumpCache.addItem(ReleaseArtist.class, releaseArtist);
        });
        item.getVideos().forEach(video -> {
            ReleaseVideo releaseVideo = ReleaseVideo.builder()
                    .releaseItem(ReleaseItem.builder().id(item.getReleaseId()).build())
                    .description(video.getDescription())
                    .title(video.getTitle())
                    .url(video.getUrl())
                    .build();
            dumpCache.addItem(ReleaseVideo.class, releaseVideo);
        });
        item.getReleaseWorks().forEach(releaseWork -> {
            ReleaseWork work = ReleaseWork.builder()
                    .releaseItem(ReleaseItem.builder().id(item.getReleaseId()).build())
                    .label(Label.builder().id(releaseWork.getId()).build())
                    .job(releaseWork.getJob())
                    .build();
            dumpCache.addItem(ReleaseWork.class, work);
        });
    }
}
