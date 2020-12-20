package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.Style;
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
public class MasterStyle extends BaseTimeEntity {
    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MasterStyleId implements Serializable {
        @Column(name = "master_id")
        private Long masterId;
        @Column(name = "style_id")
        private Long genreId;
    }

    @EmbeddedId
    private MasterStyleId masterStyleId;

    @JoinColumn(name = "master_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Master.class, fetch = FetchType.EAGER)
    private Master master;

    @JoinColumn(name = "style_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Style.class, fetch = FetchType.EAGER)
    private Style style;
}
