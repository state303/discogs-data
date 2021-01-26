package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Data
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseGenre extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "release_id")
    @ManyToOne
    private ReleaseItem releaseItem;

    @JoinColumn(name = "genre_id")
    @ManyToOne
    private Genre genre;
}
