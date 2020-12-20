package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import io.dsub.discogsdata.common.entity.label.Label;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseWork extends BaseTimeEntity {

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseWorksId implements Serializable {
        @Column(name = "release_item_id")
        private Long releaseItemId;
        @Column(name = "label_id")
        private Long labelId;
    }

    @EmbeddedId
    private ReleaseWorksId releaseWorksId;

    @JoinColumn(name = "release_item_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = ReleaseItem.class, fetch = FetchType.EAGER)
    private ReleaseItem releaseItem;

    @JoinColumn(name = "label_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Label.class, fetch = FetchType.EAGER)
    private Label label;

    private String name;
    private String job;
}
