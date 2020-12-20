package io.dsub.discogsdata.batch.xml.object;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistAlias;
import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import io.dsub.discogsdata.common.entity.artist.ArtistMember;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "artist")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlArtist extends XmlObject {

    @XmlElement(name = "id")
    private Long id;
    @XmlElement(name = "name")
    private String name;
    @XmlElement(name = "realname")
    private String realName;
    @XmlElement(name = "profile")
    private String profile;
    @XmlElement(name = "data_quality")
    private String dataQuality;

    @XmlElementWrapper(name = "urls")
    @XmlElement(name = "url")
    private List<String> urls = Collections.synchronizedList(new ArrayList<>());

    @XmlElementWrapper(name = "namevariations")
    @XmlElement(name = "name")
    private List<String> nameVariations = Collections.synchronizedList(new ArrayList<>());

    @XmlElementWrapper(name = "aliases")
    @XmlElement(name = "name")
    private List<XmlArtist.Alias> aliases = Collections.synchronizedList(new ArrayList<>());

    @XmlElementWrapper(name = "groups")
    @XmlElement(name = "name")
    private List<XmlArtist.Group> groups = Collections.synchronizedList(new ArrayList<>());

    @XmlElementWrapper(name = "members")
    @XmlElement(name = "name")
    private List<XmlArtist.Member> members = Collections.synchronizedList(new ArrayList<>());

    public Collection<XmlArtist.ArtistRef> getRelations() {
        List<XmlArtist.ArtistRef> refs = new ArrayList<>(groups);
        refs.addAll(members);
        refs.addAll(aliases);
        return refs;
    }

    public Artist toEntity() {
        return Artist.builder()
                .id(id)
                .name(name)
                .realName(realName)
                .dataQuality(dataQuality)
                .profile(profile)
                .nameVariation(nameVariations)
                .urls(urls)
                .build();
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static abstract class ArtistRef {
        @XmlValue
        private String name;
        @XmlAttribute(name = "id")
        private Long id;
    }

    public static class Alias extends XmlArtist.ArtistRef {
    }

    public static class Group extends XmlArtist.ArtistRef {
    }

    public static class Member extends XmlArtist.ArtistRef {
    }
}

