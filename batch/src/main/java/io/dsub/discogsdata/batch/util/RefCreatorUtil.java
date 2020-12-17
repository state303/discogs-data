package io.dsub.discogsdata.batch.util;

import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RefCreatorUtil {
    public Collection<BaseEntity> getArtistRelations(Long id, List<? extends XmlArtist.ArtistRef>... refs) {
        List<BaseEntity> list = new LinkedList<>();
        for (List<? extends XmlArtist.ArtistRef> ref : refs) {
            list.addAll(ref.stream().map(o -> o.toRelEntity(id)).collect(Collectors.toList()));
        }
        return list;
    }
}
