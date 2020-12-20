package io.dsub.discogsdata.common.entity;

import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String name;
    @Column(columnDefinition = "TEXT")
    private String title;
    @Column(length = 1000)
    private String duration;

    /*
     * Actual key to be injected. This makes possible to insert WITHOUT fetching
     * the mapped object first (faster)
     */
    @Column(name = "release_item_id")
    private Long releaseItemId;

    /*
     * Convenient READ_ONLY access for actually mapped class.
     * NOTE: mark any FetchType to avoid warning about immutability.
     */
    @JoinColumn(name = "release_item_id", insertable = false, updatable = false)
    @ManyToOne(targetEntity = ReleaseItem.class, fetch = FetchType.LAZY)
    private ReleaseItem releaseItem;
}
