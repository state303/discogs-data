package io.dsub.discogsdata.batch.xml.object;

import io.dsub.discogsdata.common.entity.artist.Artist;
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
public class XmlArtist {

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

    public Artist toArtist() {
        return Artist.builder()
                .id(id)
                .name(name)
                .dataQuality(dataQuality)
                .realName(realName)
                .profile(profile)
                .urls(urls)
                .nameVariations(nameVariations)
                .build();
    }
}

