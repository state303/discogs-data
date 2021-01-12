package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import lombok.*;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Master extends BaseTimeEntity {
    @Id
    private Long id;

    private short year;

    @Column(length = 2000)
    private String title;

    private String dataQuality;

    @OneToOne
    private ReleaseItem mainReleaseItem;

    @OneToMany(mappedBy = "master", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<MasterVideo> videos;
}
