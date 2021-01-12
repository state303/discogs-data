package io.dsub.discogsdata.batch.xml.object;

import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterVideo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMaster extends XmlObject {
    @XmlAttribute(name = "id")
    private Long id;

    @XmlElement(name = "main_release")
    private Long mainRelease;

    @XmlElementWrapper(name = "artists")
    @XmlElement(name = "artist")
    private Set<ArtistInfo> artists = new HashSet<>();

    @XmlElementWrapper(name = "genres")
    @XmlElement(name = "genre")
    private Set<String> genres = new HashSet<>();

    @XmlElementWrapper(name = "styles")
    @XmlElement(name = "style")
    private Set<String> styles = new HashSet<>();

    @XmlElement(name = "year")
    private Short year;

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "data_quality")
    private String dataQuality;

    @XmlElementWrapper(name = "videos")
    @XmlElement(name = "video")
    private Set<Video> videos = new HashSet<>();

    @Override
    public Master toEntity() {
        String[] fields = new String[]{title, dataQuality};
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] != null && fields[i].isBlank()) {
                fields[i] = null;
            }
        }
        return Master.builder()
                .id(id)
                .title(fields[0])
                .dataQuality(fields[1])
                .year(year)
                .build();
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ArtistInfo {
        @XmlElement(name = "id")
        private Long id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Video {
        @XmlElement(name = "title")
        private String title;
        @XmlElement(name = "description")
        private String description;
        @XmlAttribute(name = "src")
        private String url;
        public MasterVideo toMasterVideo() {
            return MasterVideo.builder()
                    .title(title)
                    .url(url)
                    .description(description)
                    .build();
        }
        public MasterVideo toMasterVideo(Long masterId) {
            return MasterVideo.builder()
                    .title(title)
                    .url(url)
                    .description(description)
                    .master(Master.builder().id(masterId).build())
                    .build();
        }
    }
}
