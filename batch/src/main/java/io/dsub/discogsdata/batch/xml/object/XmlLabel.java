package io.dsub.discogsdata.batch.xml.object;

import io.dsub.discogsdata.common.entity.label.Label;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlLabel {
    @XmlElement(name = "id")
    private Long id;
    @XmlElement(name = "name")
    private String name;
    @XmlElement(name = "contactinfo")
    private String contactInfo;
    @XmlElement(name = "profile")
    private String profile;
    @XmlElement(name = "data_quality")
    private String dataQuality;
    @XmlElementWrapper(name = "urls")
    @XmlElement(name = "url")
    private List<String> urls = new LinkedList<>();
}
