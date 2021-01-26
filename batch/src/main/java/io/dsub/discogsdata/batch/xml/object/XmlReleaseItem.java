package io.dsub.discogsdata.batch.xml.object;

import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import lombok.*;

import javax.xml.bind.annotation.*;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
@NoArgsConstructor
@AllArgsConstructor
public class XmlReleaseItem {

    @XmlAttribute(name = "id")
    private Long releaseId;
    @XmlAttribute(name = "status")
    private String status;
    @XmlElement(name = "title")
    private String title;
    @XmlElement(name = "country")
    private String country;
    @XmlElement(name = "notes")
    private String notes;
    @XmlElement(name = "data_quality")
    private String dataQuality;
    @XmlElement(name = "master_id")
    private Master master;
    @XmlElement(name = "released")
    private String releaseDate;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Master {
        @XmlValue
        private Long masterId;
        @XmlAttribute(name = "is_main_release")
        private boolean isMaster;
    }

    public ReleaseItem toReleaseItem() {
        XmlReleaseItem trimmedItem = this.withTrimmedData();
        return ReleaseItem.builder()
                .id(trimmedItem.releaseId)
                .isMaster(trimmedItem.getMaster() != null && trimmedItem.getMaster().isMaster)
                .dataQuality(trimmedItem.getDataQuality())
                .country(trimmedItem.getCountry())
                .notes(trimmedItem.getNotes())
                .status(trimmedItem.getStatus())
                .title(trimmedItem.getTitle())
                .build();
    }

    public XmlReleaseItem withTrimmedData() {
        return XmlReleaseItem.builder()
                .releaseId(releaseId)
                .country(trimString(country))
                .notes(trimString(notes))
                .dataQuality(trimString(dataQuality))
                .status(trimString(status))
                .title(trimString(title))
                .releaseDate(releaseDate)
                .master(master)
                .build();
    }

    private static String trimString(String in) {
        if (in != null && in.isBlank()) {
            return null;
        }
        return in;
    }
}
