package io.dsub.discogsdata.batch.process;

import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import lombok.Data;

@Data
public class SimpleRelation {
    private final Long parentId;
    private final Long childId;

    public ArtistGroup toArtistGroup() {

        ArtistGroup.ArtistGroupId id =
                new ArtistGroup.ArtistGroupId(parentId, childId);

        return ArtistGroup.builder()
                .artistGroupId(id)
                .build();
    }
}
