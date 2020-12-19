package io.dsub.discogsdata.batch.process;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@SuppressWarnings("unchecked")
public class RelationsHolder {

    private static final Map<Class<?>, ConcurrentLinkedQueue<SimpleRelation>> SIMPLE_RELATIONS_MAP = new ConcurrentHashMap<>();
    private static final Map<Class<?>, ConcurrentLinkedQueue<BaseEntity>> OBJECT_RELATIONS_MAP = new ConcurrentHashMap<>();

    public void addSimpleRelation(Class<?> clazz, SimpleRelation simpleRelation) {
        makeIfAbsent(clazz, SIMPLE_RELATIONS_MAP);
        SIMPLE_RELATIONS_MAP.get(clazz).add(simpleRelation);
    }

    public <T extends BaseEntity> void addItem(Class<T> clazz, T item) {
        makeIfAbsent(clazz, OBJECT_RELATIONS_MAP);
        OBJECT_RELATIONS_MAP.get(clazz).add(item);
    }

    public void addSimpleRelations(Class<?> clazz, Collection<SimpleRelation> simpleRelations) {
        makeIfAbsent(clazz, SIMPLE_RELATIONS_MAP);
        SIMPLE_RELATIONS_MAP.get(clazz).addAll(simpleRelations);
    }

    public <T extends BaseEntity> void addItems(Class<T> clazz, Collection<T> items) {
        makeIfAbsent(clazz, OBJECT_RELATIONS_MAP);
        OBJECT_RELATIONS_MAP.get(clazz).addAll(items);
    }

    public ConcurrentLinkedQueue<SimpleRelation> pullSimpleRelationsQueue(Class<?> clazz) {
        return SIMPLE_RELATIONS_MAP.get(clazz);
    }

    public <T> ConcurrentLinkedQueue<T> pullObjectRelationsQueue(Class<T> clazz) {
        return (ConcurrentLinkedQueue<T>) OBJECT_RELATIONS_MAP.get(clazz);
    }

    private <T> void makeIfAbsent(Class<?> clazz, Map<Class<?>, ConcurrentLinkedQueue<T>> map) {
        if (!map.containsKey(clazz)) {
            map.put(clazz, new ConcurrentLinkedQueue<>());
        }
    }
}
