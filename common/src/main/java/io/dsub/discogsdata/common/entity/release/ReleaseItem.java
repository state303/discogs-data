package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import io.dsub.discogsdata.common.entity.master.Master;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseItem extends BaseTimeEntity {
    @Id
    private Long id;

    private boolean isMaster;

    private String status;

    @Column(length = 2000)
    private String title;

    private String country;

    @Column(columnDefinition = "LONGTEXT")
    private String notes;

    private String dataQuality;

    /*
     * Actual mapping value to be injected. This makes possible to insert WITHOUT fetching
     * the mapped object first (faster)
     */
    @Column(name = "master_id")
    private Long masterId;

    /*
     * Convenient READ_ONLY access for actually mapped class.
     * NOTE: mark any FetchType to avoid warning about immutability.
     */
    @JoinColumn(name = "master_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = Master.class, fetch = FetchType.LAZY)
    private Master master;

    private Instant releaseDate;
}
