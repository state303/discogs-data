package io.dsub.discogsdata.batch.xml.object;

import io.dsub.discogsdata.batch.process.SimpleRelation;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "artist")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlArtistRelation {

    @XmlElement(name = "id")
    private Long id;

    @XmlElement(name = "profile")
    private String profile;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static abstract class ArtistRef {
        @XmlValue
        private String name;
        @XmlAttribute(name = "id")
        private Long id;
    }

    @XmlElementWrapper(name = "aliases")
    @XmlElement(name = "name")
    private List<Alias> aliases = new LinkedList<>();

    @XmlElementWrapper(name = "groups")
    @XmlElement(name = "name")
    private List<Group> groups = new LinkedList<>();

    @XmlElementWrapper(name = "members")
    @XmlElement(name = "name")
    private List<Member> members = new LinkedList<>();

    public static class Alias extends ArtistRef{}
    public static class Group extends ArtistRef{}
    public static class Member extends ArtistRef{}

    public Map<String, List<SimpleRelation>> toSimpleRelations() {
        Map<String, List<SimpleRelation>> simpleRelations = new HashMap<>();
        simpleRelations.put("aliases", aliases.stream()
                .map(item -> new SimpleRelation(id, item.getId()))
                .collect(Collectors.toList()));
        simpleRelations.put("groups", groups.stream()
                .map(item -> new SimpleRelation(id, item.getId()))
                .collect(Collectors.toList()));
        simpleRelations.put("members", members.stream()
                .map(item -> new SimpleRelation(id, item.getId()))
                .collect(Collectors.toList()));
        return simpleRelations;
    }
}
