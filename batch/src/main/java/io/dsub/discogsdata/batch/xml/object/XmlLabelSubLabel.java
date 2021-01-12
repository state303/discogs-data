package io.dsub.discogsdata.batch.xml.object;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlLabelSubLabel {

    @XmlElement(name = "id")
    private Long id;

    @XmlElement(name = "profile")
    private String profile;

    @XmlElementWrapper(name = "sublabels")
    @XmlElement(name = "label")
    private List<SubLabel> SubLabels = new ArrayList<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SubLabel {
        @XmlValue
        private String name;
        @XmlAttribute(name = "id")
        private Long id;
    }
}
