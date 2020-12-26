package io.dsub.discogsdata.batch.process;

import lombok.Data;

@Data
public class SimpleRelation {
    private final Long parentId;
    private final Long childId;
}
