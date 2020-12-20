package io.dsub.discogsdata.common.entity.master;

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
public class MasterVideo extends BaseTimeEntity {
    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MasterVideoId implements Serializable {
        @Column(name = "master_id")
        private Long masterId;
        @Column(name = "video_id")
        private Long videoId;
    }

    @EmbeddedId
    private MasterVideoId masterVideoId;

    @JoinColumn(name = "master_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Master.class, fetch = FetchType.EAGER)
    private Master master;

    @JoinColumn(name = "video_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Video.class, fetch = FetchType.EAGER)
    private Video video;
}
