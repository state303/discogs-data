package io.dsub.discogsdata.common.entity.label;

import io.dsub.discogsdata.common.entity.release.ReleaseItem;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class LabelRelease {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "release_id")
    private ReleaseItem releaseItem;

    @ManyToOne
    @JoinColumn(name = "label_id")
    private Label label;

    private String categoryNumber;
}
