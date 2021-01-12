package io.dsub.discogsdata.batch.xml.object;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMasterSubData {
    @XmlElement(name = "main_release")
    private Long mainRelease;

    @XmlElementWrapper(name = "genres")
    @XmlElement(name = "genre")
    private Set<String> genres = new HashSet<>();

    @XmlElementWrapper(name = "styles")
    @XmlElement(name = "style")
    private Set<String> styles = new HashSet<>();
}

