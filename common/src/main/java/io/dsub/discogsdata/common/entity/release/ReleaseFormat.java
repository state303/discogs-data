package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.Format;
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
public class ReleaseFormat {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "release_id")
    private ReleaseItem releaseItem;

    @ManyToOne
    @JoinColumn(name = "format_id")
    private Format format;
}
