package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.label.Label;
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
public class ReleaseCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "label_id")
    private Label label;
    @ManyToOne
    @JoinColumn(name = "release_id")
    private ReleaseItem releaseItem;

    private String name;
    private String job;
}
