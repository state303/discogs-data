package io.dsub.discogsdata.common.entity.master;

import io.dsub.discogsdata.common.entity.Video;
import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
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

    private String title;

    private String dataQuality;

    /*
     * Actual key to be injected. This makes possible to insert WITHOUT fetching
     * the mapped object first (faster)
     */
    @Column(name = "release_item_id")
    private Long releaseItemId;

    @OneToMany
    @Builder.Default
    private List<Video> videos = new ArrayList<>();

    /*
     * Convenient READ_ONLY access for actually mapped class.
     * NOTE: mark any FetchType to avoid warning about immutability.
     */
    @JoinColumn(name = "release_item_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = ReleaseItem.class, fetch = FetchType.LAZY)
    private ReleaseItem releaseItem;
}
