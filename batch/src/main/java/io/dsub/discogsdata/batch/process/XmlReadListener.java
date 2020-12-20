package io.dsub.discogsdata.batch.process;

import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.batch.xml.object.XmlObject;
import io.dsub.discogsdata.common.entity.artist.ArtistAlias;
import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import io.dsub.discogsdata.common.entity.artist.ArtistMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemReadListener;

@Slf4j
@RequiredArgsConstructor
public class XmlReadListener implements ItemReadListener<XmlObject> {

    private final RelationsHolder relationsHolder;

    @Override
    public void beforeRead() {
    }

    @Override
    public void onReadError(Exception ex) {
    }

    @Override
    public void afterRead(XmlObject item) {
        if (item instanceof XmlArtist) {
            updateArtistRefs(item);
        }
    }

    private void updateArtistRefs(XmlObject item) {
        XmlArtist artist = (XmlArtist) item;
        if (artist.getProfile().contains("[b]DO NOT USE.[/b]")) {
            return;
        }
        artist.getRelations().forEach(
                ref -> relationsHolder.addSimpleRelation(
                        ref.getClass(), new SimpleRelation(artist.getId(), ref.getId())));
    }


}
