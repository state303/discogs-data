package io.dsub.discogsdata.batch.dump.entity;

import io.dsub.discogsdata.batch.dump.SimpleDumpFetcher;
import io.dsub.discogsdata.batch.dump.dto.DumpDto;
import io.dsub.discogsdata.batch.dump.enums.DumpType;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

@Data
@Slf4j
@Entity
@Builder
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class DiscogsDump implements Comparable<DiscogsDump> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private DumpType dumpType;
    private String uri;
    private String etag;
    private Long size;
    private LocalDateTime lastModified;

    public String getResourceUrl() {
        return SimpleDumpFetcher.LAST_KNOWN_BUCKET_URL + "/" + this.uri;
    }

    public URL toUrl() {
        try {
            return new URL(SimpleDumpFetcher.LAST_KNOWN_BUCKET_URL + "/" + this.uri);
        } catch (MalformedURLException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public DumpDto toDto() {
        return DumpDto.builder()
                .dumpType(dumpType.name())
                .etag(etag)
                .size(size)
                .lastModified(lastModified)
                .build();
    }

    public String getRootElementName() {
        return this.getDumpType().name().toLowerCase().replace("xml", "");
    }

    @Override
    public int compareTo(DiscogsDump that) {
        return this.lastModified
                .compareTo(that.getLastModified());
    }
}
