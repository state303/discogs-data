package io.dsub.discogsdata.batch.process;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RelationsHolder {

    private static final Map<Class<?>, ConcurrentLinkedQueue<?>> QUEUE_MAP = new ConcurrentHashMap<>();
    private AtomicLong size = new AtomicLong();

    @SuppressWarnings("unchecked")
    public <T> ConcurrentLinkedQueue<T> pullCachedList(Class<T> clazz) {

        log.debug("{} == null ?? {} ", clazz, QUEUE_MAP.get(clazz) == null);

        return (ConcurrentLinkedQueue<T>) QUEUE_MAP.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> void putEntity(Class<T> clazz, List<T> items) {
        ConcurrentLinkedQueue<T> concurrentLinkedQueue =
                (ConcurrentLinkedQueue<T>) QUEUE_MAP.getOrDefault(clazz, new ConcurrentLinkedQueue<T>());
        concurrentLinkedQueue.addAll(items);
    }

    @SuppressWarnings("unchecked")
    public <T> void putEntity(Class<T> clazz, T item) {
        ConcurrentLinkedQueue<T> concurrentLinkedQueue = (ConcurrentLinkedQueue<T>) QUEUE_MAP.get(clazz);
        if (concurrentLinkedQueue == null) {
            concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
            QUEUE_MAP.put(clazz, concurrentLinkedQueue);
        }
        concurrentLinkedQueue.add(item);
    }

    public enum Type {
        ARTIST_ALIAS, ARTIST_GROUP, ARTIST_MEMBER
    }
}
