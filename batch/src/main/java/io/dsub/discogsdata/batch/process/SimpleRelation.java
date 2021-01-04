package io.dsub.discogsdata.batch.process;

import lombok.Data;

@Data
public class SimpleRelation {
    private final Object parent;
    private final Object child;
}
