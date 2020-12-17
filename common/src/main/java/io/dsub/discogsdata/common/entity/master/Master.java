package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Master extends BaseTimeEntity {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private ReleaseItem mainReleaseItem;

    private short year;

    private String title;

    private String dataQuality;
}
