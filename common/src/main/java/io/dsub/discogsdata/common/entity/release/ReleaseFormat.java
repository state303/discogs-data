package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.Format;
import io.dsub.discogsdata.common.entity.artist.Artist;
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
public class ReleaseFormat extends BaseTimeEntity {
    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseFormatId implements Serializable {
        @Column(name = "release_item_id")
        private Long releaseItemId;
        @Column(name = "format_id")
        private Long formatId;
    }

    @EmbeddedId
    private ReleaseFormatId releaseFormatId;

    @JoinColumn(name = "release_item_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = ReleaseItem.class, fetch = FetchType.EAGER)
    private ReleaseItem releaseItem;

    @JoinColumn(name = "format_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Format.class, fetch = FetchType.EAGER)
    private Format format;
}
