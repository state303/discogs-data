package io.dsub.discogsdata.batch.step.master;

import io.dsub.discogsdata.batch.process.RelationsHolder;
import io.dsub.discogsdata.batch.process.SimpleRelation;
import io.dsub.discogsdata.batch.step.XmlObjectReadListener;
import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.entity.Video;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterGenre;
import io.dsub.discogsdata.common.entity.master.MasterStyle;
import io.dsub.discogsdata.common.entity.master.MasterVideo;
import org.springframework.stereotype.Component;

@Component
public class XmlMasterReadListener extends XmlObjectReadListener<XmlMaster> {
    public XmlMasterReadListener(RelationsHolder relationsHolder) {
        super(relationsHolder);
    }
    @Override
    public void afterRead(XmlMaster item) {
        item.getArtists().stream()
                .map(artistInfo -> new SimpleRelation(item.getId(), artistInfo.getId()))
                .forEach(sr -> relationsHolder.addSimpleRelation(XmlMaster.ArtistInfo.class, sr));

        item.getStyles().stream()
                .map(styleStr -> {
                    MasterStyle masterStyle = new MasterStyle();
                    Style style = new Style();
                    style.setName(styleStr);
                    Master master = new Master();
                    master.setId(item.getId());
                    masterStyle.setMaster(master);
                    masterStyle.setStyle(style);
                    return masterStyle;
                })
                .forEach(ms -> relationsHolder.addItem(MasterStyle.class, ms));

        item.getGenres().stream()
                .map(genreStr -> {
                    MasterGenre masterGenre = new MasterGenre();
                    Genre genre = new Genre();
                    genre.setName(genreStr);
                    Master master = new Master();
                    master.setId(item.getId());
                    masterGenre.setMaster(master);
                    masterGenre.setGenre(genre);
                    return masterGenre;
                })
                .forEach(mg -> relationsHolder.addItem(MasterGenre.class, mg));

        item.getVideos().stream()
                .map(xmlVideo -> {
                    MasterVideo masterVideo = new MasterVideo();
                    Master master = new Master();
                    master.setId(item.getId());

                    Video video = Video.builder()
                            .url(xmlVideo.getUrl())
                            .title(xmlVideo.getTitle())
                            .description(xmlVideo.getDescription())
                            .build();

                    masterVideo.setMaster(master);
                    masterVideo.setVideo(video);
                    return masterVideo;
                })
                .forEach(mv -> relationsHolder.addItem(MasterVideo.class, mv));
    }
}
