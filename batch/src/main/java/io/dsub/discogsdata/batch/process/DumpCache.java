package io.dsub.discogsdata.batch.process;

import io.dsub.discogsdata.batch.xml.object.XmlRelease;
import io.dsub.discogsdata.common.entity.Genre;
import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@SuppressWarnings("unchecked")
public class DumpCache {

    private static final Map<String, ConcurrentLinkedQueue<SimpleRelation>> SIMPLE_RELATIONS_MAP = new ConcurrentHashMap<>();
    private static final Map<String, ConcurrentLinkedQueue<BaseEntity>> OBJECT_RELATIONS_MAP = new ConcurrentHashMap<>();

    private static final Map<String, Long> GENRES_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Long> STYLES_MAP = new ConcurrentHashMap<>();

    public void addSimpleRelation(Class<?> clazz, SimpleRelation simpleRelation) {
        makeIfAbsent(clazz.getSimpleName(), SIMPLE_RELATIONS_MAP);
        SIMPLE_RELATIONS_MAP.get(clazz.getSimpleName()).add(simpleRelation);
    }

    public void addSimpleRelation(Class<?> clazz, Object parent, Object child) {
        makeIfAbsent(clazz.getSimpleName(), SIMPLE_RELATIONS_MAP);
        SIMPLE_RELATIONS_MAP.get(clazz.getSimpleName()).add(new SimpleRelation(parent, child));
    }

    public <T extends BaseEntity> void addItem(Class<T> clazz, T item) {
        makeIfAbsent(clazz.getSimpleName(), OBJECT_RELATIONS_MAP);
        OBJECT_RELATIONS_MAP.get(clazz.getSimpleName()).add(item);
    }

    public void addSimpleRelations(Class<?> clazz, Collection<SimpleRelation> simpleRelations) {
        makeIfAbsent(clazz.getSimpleName(), SIMPLE_RELATIONS_MAP);
        SIMPLE_RELATIONS_MAP.get(clazz.getSimpleName()).addAll(simpleRelations);
    }

    public void addSimpleRelation(String key, SimpleRelation simpleRelation) {
        makeIfAbsent(key, SIMPLE_RELATIONS_MAP);
    }

    public <T extends BaseEntity> void addItems(Class<T> clazz, Collection<T> items) {
        makeIfAbsent(clazz.getSimpleName(), OBJECT_RELATIONS_MAP);
        OBJECT_RELATIONS_MAP.get(clazz.getSimpleName()).addAll(items);
    }

    public ConcurrentLinkedQueue<SimpleRelation> pullSimpleRelationsQueue(Class<?> clazz) {
        return SIMPLE_RELATIONS_MAP.get(clazz.getSimpleName());
    }
    public ConcurrentLinkedQueue<SimpleRelation> pullSimpleRelationsQueue(String key) {
        return SIMPLE_RELATIONS_MAP.get(key);
    }

    public <T> ConcurrentLinkedQueue<T> pullObjectRelationsQueue(Class<T> clazz) {
        return pullObjectRelationsQueue(clazz.getSimpleName());
    }
    public <T> ConcurrentLinkedQueue<T> pullObjectRelationsQueue(String key) {
        return (ConcurrentLinkedQueue<T>) OBJECT_RELATIONS_MAP.get(key);
    }

    private <T> void makeIfAbsent(String key, Map<String, ConcurrentLinkedQueue<T>> map) {
        if (!map.containsKey(key)) {
            map.put(key, new ConcurrentLinkedQueue<>());
        }
    }

    public Long getStyleId(String styleName) {
        return STYLES_MAP.get(styleName);
    }

    public Long getGenreId(String genreName) {
        return GENRES_MAP.get(genreName);
    }

    public void putGenre(String genreName, Long id) {
        if (id == null) {
            id = -1L;
        }
        GENRES_MAP.put(genreName, id);
    }

    public void putStyle(String styleName, Long id) {
        if (id == null) {
            id = -1L;
        }
        STYLES_MAP.put(styleName, id);
    }

    public Set<String> getGenresNames() {
        return GENRES_MAP.keySet();
    }

    public Set<String> getStylesNames() {
        return STYLES_MAP.keySet();
    }
}
