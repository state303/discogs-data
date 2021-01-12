package io.dsub.discogsdata.common.entity.artist;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
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
public class Artist extends BaseTimeEntity {
    @Id
    private Long id;
    private String name;
    @Column(length = 2000)
    private String realName;

    @Builder.Default
    @ElementCollection
    @Column(length = 2000, name = "name_variation")
    @JoinTable(name = "artist_name_variation")
    private List<String> nameVariations = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @Column(columnDefinition = "TEXT", name = "url")
    @JoinTable(name = "artist_url")
    private List<String> urls = new ArrayList<>();

    @Column(columnDefinition = "LONGTEXT")
    private String profile;

    private String dataQuality;
}
