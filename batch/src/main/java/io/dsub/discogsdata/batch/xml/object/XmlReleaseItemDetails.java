package io.dsub.discogsdata.batch.xml.object;

import io.dsub.discogsdata.batch.util.MalformedDateParser;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlReleaseItemDetails {

    @XmlAttribute(name = "id")
    private Long releaseId;

    @XmlElementWrapper(name = "genres")
    @XmlElement(name = "genre")
    private Set<String> genres;

    @XmlElementWrapper(name = "styles")
    @XmlElement(name = "style")
    private Set<String> styles;

    @XmlElement(name = "released")
    private String releaseDate;

    @XmlElementWrapper(name = "artists")
    @XmlElement(name = "artist")
    private Set<AlbumArtist> releaseArtists;

    @XmlElementWrapper(name = "extraartists")
    @XmlElement(name = "artist")
    private Set<CreditedArtist> creditedArtists;

    @XmlElementWrapper(name = "labels")
    @XmlElement(name = "label")
    private Set<Label> labels = new HashSet<>();

    @XmlElementWrapper(name = "formats")
    @XmlElement(name = "format")
    private Set<Format> formats = new HashSet<>();

    @XmlElementWrapper(name = "tracklist")
    @XmlElement(name = "track")
    private Set<Track> tracks = new HashSet<>();

    @XmlElementWrapper(name = "identifiers")
    @XmlElement(name = "identifier")
    private Set<Identifier> identifiers = new HashSet<>();

    @XmlElementWrapper(name = "companies")
    @XmlElement(name = "company")
    private Set<ReleaseWork> releaseWorks = new HashSet<>();

    @XmlElementWrapper(name = "videos")
    @XmlElement(name = "video")
    private Set<Video> videos = new HashSet<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AlbumArtist {
        @XmlElement(name = "id")
        private Long id;
        @XmlElement(name = "name")
        private String name;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CreditedArtist {
        @XmlElement(name = "id")
        private Long id;
        @XmlElement(name = "name")
        private String name;
        @XmlElement(name = "role")
        private String role;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Label {
        @XmlAttribute(name = "catno")
        private String categoryNumber;
        @XmlAttribute(name = "id")
        private Long id;
        @XmlAttribute(name = "name")
        private String labelName;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Format {
        @XmlAttribute(name = "name")
        private String name;
        @XmlAttribute(name = "qty")
        private Integer qty;
        @XmlAttribute(name = "text")
        private String text;
        @XmlElementWrapper(name = "descriptions")
        @XmlElement(name = "description")
        private Set<String> description;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Track {
        @XmlElement(name = "position")
        private String position;
        @XmlElement(name = "title")
        private String title;
        @XmlElement(name = "duration")
        private String duration;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Identifier {
        @XmlAttribute(name = "description")
        private String description;
        @XmlAttribute(name = "type")
        private String type;
        @XmlAttribute(name = "value")
        private String value;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ReleaseWork {
        @XmlElement(name = "id")
        private Long id;
        @XmlElement(name = "name")
        private String name;
        @XmlElement(name = "entity_type_name")
        private String job;
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
    }
}
