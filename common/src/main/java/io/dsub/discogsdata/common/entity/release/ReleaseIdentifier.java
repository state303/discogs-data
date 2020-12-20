package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.Format;
import io.dsub.discogsdata.common.entity.Identifier;
import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseIdentifier extends BaseTimeEntity {
    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseIdentifierId implements Serializable {
        @Column(name = "release_item_id")
        private Long releaseItemId;
        @Column(name = "identifier_id")
        private Long identifier_id;
    }

    @EmbeddedId
    private ReleaseIdentifierId releaseIdentifierId;

    @JoinColumn(name = "release_item_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = ReleaseItem.class, fetch = FetchType.EAGER)
    private ReleaseItem releaseItem;

    @JoinColumn(name = "identifier_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Identifier.class, fetch = FetchType.EAGER)
    private Identifier identifier;
}
