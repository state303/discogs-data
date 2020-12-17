package io.dsub.discogsdata.batch.xml.object;

import io.dsub.discogsdata.common.entity.master.Master;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.Set;

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
    private Set<ArtistInfo> artists;

    @XmlElementWrapper(name = "genres")
    @XmlElement(name = "genre")
    private Set<String> genres;

    @XmlElementWrapper(name = "styles")
    @XmlElement(name = "style")
    private Set<String> styles;

    @XmlElement(name = "year")
    private Short year;

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "data_quality")
    private String dataQuality;

    @XmlElementWrapper(name = "videos")
    @XmlElement(name = "video")
    private Set<VideoUrl> videos;

    @Override
    public Master toEntity() {
        return null;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ArtistInfo {
        @XmlElement(name = "id")
        private Long id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class VideoUrl {
        @XmlAttribute(name = "src")
        private String url;
    }
}
