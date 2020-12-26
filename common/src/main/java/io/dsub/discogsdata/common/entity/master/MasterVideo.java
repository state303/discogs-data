package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.Video;
import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class MasterVideo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "master_id")
    @ManyToOne
    private Master master;

    @JoinColumn(name = "video_id")
    @ManyToOne
    private Video video;
}
