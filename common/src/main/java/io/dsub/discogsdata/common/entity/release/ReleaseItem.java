package io.dsub.discogsdata.common.entity.release;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import io.dsub.discogsdata.common.entity.master.Master;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "release_item_identifier")
    private List<Identifier> identifiers = new ArrayList<>();

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "release_item_format")
    private List<Format> formats = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "master_id")
    private Master master;

    private LocalDate releaseDate;
}
