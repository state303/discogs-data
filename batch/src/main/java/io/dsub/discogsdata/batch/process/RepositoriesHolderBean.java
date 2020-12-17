package io.dsub.discogsdata.batch.process;

import io.dsub.discogsdata.common.repository.artist.ArtistAliasRepository;
import io.dsub.discogsdata.common.repository.artist.ArtistGroupRepository;
import io.dsub.discogsdata.common.repository.artist.ArtistMemberRepository;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Getter
@Component
@RequiredArgsConstructor
public class RepositoriesHolderBean {
    private final ArtistRepository artistRepository;
    private final ArtistMemberRepository artistMemberRepository;
    private final ArtistGroupRepository artistGroupRepository;
    private final ArtistAliasRepository artistAliasRepository;
}
