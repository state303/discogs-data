package io.dsub.discogsdata.common.entity.label;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Builder
@Table(name = "label_sublabel")
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class LabelSubLabel extends BaseTimeEntity{
    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelSubLabelId implements Serializable {
        @Column(name = "parent_label_id")
        private Long parentLabelId;
        @Column(name = "sub_label_id")
        private Long subLabelId;
    }

    @EmbeddedId
    private LabelSubLabelId labelSubLabelId;

    /*
     * Convenient READ_ONLY access for actually mapped class.
     * NOTE: mark any FetchType to avoid warning about immutability.
     */
    @JoinColumn(name = "parent_label_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Label.class, fetch = FetchType.EAGER)
    private Label parent;

    @JoinColumn(name = "sub_label_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Label.class, fetch = FetchType.EAGER)
    private Label subLabel;
}
