package io.dsub.discogsdata.batch.xml.object;

import io.dsub.discogsdata.common.entity.label.Label;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlLabel extends XmlObject {
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
    private Set<String> urls;
    @XmlElementWrapper(name = "sublabels")
    @XmlElement(name = "label")
    private Set<SubLabel> SubLabels;

    @Override
    public Label toEntity() {
        return Label.builder()
                .id(id)
                .build();
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SubLabel {
        @XmlValue
        private String name;
        @XmlAttribute(name = "id")
        private Long id;
    }
}
