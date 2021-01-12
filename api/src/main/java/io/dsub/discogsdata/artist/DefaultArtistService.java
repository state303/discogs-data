package io.dsub.discogsdata.artist;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.artist.ArtistAlias;
import io.dsub.discogsdata.common.entity.artist.ArtistGroup;
import io.dsub.discogsdata.common.repository.artist.ArtistAliasRepository;
import io.dsub.discogsdata.common.repository.artist.ArtistGroupRepository;
import io.dsub.discogsdata.common.repository.artist.ArtistMemberRepository;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import io.dsub.discogsdata.exception.ArtistNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultArtistService implements ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistMemberRepository artistMemberRepository;
    private final ArtistGroupRepository artistGroupRepository;
    private final ArtistAliasRepository artistAliasRepository;

    @Override
    public ArtistDto getArtistById(long id) throws ArtistNotFoundException {
        Optional<Artist> optionalArtist = artistRepository.findById(id);
        if (optionalArtist.isEmpty()) {
            throw new ArtistNotFoundException("artist with id " + id + " not found");
        }
        return this.makeArtistDto(optionalArtist.get());
    }

    @Override
    public List<ArtistDto> getArtists() {
        return artistRepository.findAll(PageRequest.of(1, 50)).stream()
                .map(this::makeArtistDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ArtistDto> getArtistsByName(String name) {
        return artistRepository.findAllByNameContains(name).stream()
                .map(this::makeArtistDto)
                .collect(Collectors.toList());
    }

    private ArtistDto makeArtistDto(Artist artist) {
        List<Long> members = artistMemberRepository.findAllByArtistId(artist.getId()).stream()
                .map(item -> item.getMember().getId())
                .collect(Collectors.toList());
        List<Long> groups = artistGroupRepository.findAllByArtistId(artist.getId()).stream()
                .map(item -> item.getGroup().getId())
                .collect(Collectors.toList());
        List<Long> aliases = artistAliasRepository.findAllByArtistId(artist.getId()).stream()
                .map(item -> item.getAlias().getId())
                .collect(Collectors.toList());
        return ArtistDto.fromArtist(artist, members, groups, aliases);
    }
}
