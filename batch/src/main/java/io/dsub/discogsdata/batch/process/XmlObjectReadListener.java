package io.dsub.discogsdata.batch.process;

import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.batch.xml.object.XmlLabel;
import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import io.dsub.discogsdata.batch.xml.object.XmlObject;
import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.entity.Video;
import io.dsub.discogsdata.common.entity.master.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class XmlObjectReadListener implements ItemReadListener<XmlObject> {

    private static final String DO_NOT_USE_FLAG = "[b]DO NOT USE.[/b]";

    private final RelationsHolder relationsHolder;

    @Override
    public void beforeRead() {
    }

    @Override
    public void onReadError(Exception ex) {
    }

    @Override
    public void afterRead(XmlObject item) {
        if (item instanceof XmlArtist) {
            updateArtistRefs(item);
        } else if (item instanceof XmlLabel) {
            updateLabelRefs(item);
        } else if (item instanceof XmlMaster) {

        }
    }

    private void updateArtistRefs(XmlObject item) {
        XmlArtist artist = (XmlArtist) item;
        if (artist.getProfile().contains(DO_NOT_USE_FLAG)) {
            return;
        }
        artist.getRelations().forEach(
                ref -> relationsHolder.addSimpleRelation(
                        ref.getClass(), new SimpleRelation(artist.getId(), ref.getId())));
    }

    private void updateLabelRefs(XmlObject item) {
        XmlLabel label = (XmlLabel) item;
        if (label.getProfile().contains(DO_NOT_USE_FLAG)) {
            return;
        }
        label.getSubLabels().forEach(
                ref -> relationsHolder.addSimpleRelation(
                        ref.getClass(), new SimpleRelation(label.getId(), ref.getId())));
    }

    private void updateMasterRefs(XmlObject item) {
        XmlMaster xmlMaster = (XmlMaster) item;

        xmlMaster.getArtists().stream()
                .map(artistInfo -> new SimpleRelation(xmlMaster.getId(), artistInfo.getId()))
                .forEach(sr -> relationsHolder.addSimpleRelation(XmlMaster.ArtistInfo.class, sr));

        xmlMaster.getStyles().stream()
                .map(styleStr -> {
                    MasterStyle masterStyle = new MasterStyle();
                    Style style = new Style();
                    style.setName(styleStr);
                    Master master = new Master();
                    master.setId(xmlMaster.getId());
                    masterStyle.setMaster(master);
                    masterStyle.setStyle(style);
                    return masterStyle;
                })
                .forEach(ms -> relationsHolder.addItem(MasterStyle.class, ms));

        xmlMaster.getGenres().stream()
                .map(genreStr -> {
                    MasterGenre masterGenre = new MasterGenre();
                    Genre genre = new Genre();
                    genre.setName(genreStr);
                    Master master = new Master();
                    master.setId(xmlMaster.getId());
                    masterGenre.setMaster(master);
                    masterGenre.setGenre(genre);
                    return masterGenre;
                })
                .forEach(mg -> relationsHolder.addItem(MasterGenre.class, mg));

        xmlMaster.getVideos().stream()
                .map(xmlVideo -> {
                    MasterVideo masterVideo = new MasterVideo();
                    Master master = new Master();
                    master.setId(xmlMaster.getId());

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
