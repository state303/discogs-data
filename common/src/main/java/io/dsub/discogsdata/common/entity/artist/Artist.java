package io.dsub.discogsdata.common.entity.artist;

import io.dsub.discogsdata.common.entity.base.BaseTimeEntity;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
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
    private String realName;

    @Builder.Default
    @ElementCollection
    @Column(columnDefinition = "TEXT")
    private List<String> nameVariation = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @Column(columnDefinition = "TEXT")
    private List<String> urls = new ArrayList<>();

    @Column(columnDefinition = "LONGTEXT")
    private String profile;

    private String dataQuality;
}
