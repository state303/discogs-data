package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.Identifier;
import io.dsub.discogsdata.common.entity.Video;
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
public class ReleaseVideo extends BaseTimeEntity {
    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseVideoId implements Serializable {
        @Column(name = "release_item_id")
        private Long releaseItemId;
        @Column(name = "video_id")
        private Long video_id;
    }

    @EmbeddedId
    private ReleaseVideoId releaseVideoId;

    @JoinColumn(name = "release_item_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = ReleaseItem.class, fetch = FetchType.EAGER)
    private ReleaseItem releaseItem;

    @JoinColumn(name = "video_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Video.class, fetch = FetchType.EAGER)
    private Video video;
}
