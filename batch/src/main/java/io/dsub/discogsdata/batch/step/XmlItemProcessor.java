package io.dsub.discogsdata.batch.step;

import org.springframework.batch.item.ItemProcessor;

public interface XmlItemProcessor<S, T> extends ItemProcessor<S, T> {
    String IGNORE_FLAG = "[b]DO NOT USE.[/b]";

    default boolean assertNotNullAndBlank(String s) {
        return s != null && s.isBlank();
    }
}
