package io.dsub.discogsdata.batch.xml.object;

import io.dsub.discogsdata.common.entity.label.Label;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.List;
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
    private List<String> urls;
    @XmlElementWrapper(name = "sublabels")
    @XmlElement(name = "label")
    private List<SubLabel> SubLabels;

    @Override
    public Label toEntity() {

        String[] fields = new String[]{name, contactInfo, dataQuality, profile};

        for (int i = 0; i < fields.length; i++) {
            if (fields[i] != null && fields[i].isBlank()) {
                fields[i] = null;
            }
        }

        return Label.builder()
                .id(id)
                .name(fields[0])
                .contactInfo(fields[1])
                .profile(fields[2])
                .profile(fields[3])
                .urls(urls)
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
