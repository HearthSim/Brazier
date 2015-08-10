package com.github.kelemen.hearthstone.emulator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class HearthStoneEntityDatabase<EntityType extends HearthStoneEntity> {
    public static final class Builder<EntityType extends HearthStoneEntity> {
        private final List<EntityType> entities;
        private final Set<EntityId> addedIds;

        public Builder() {
            this.entities = new LinkedList<>();
            this.addedIds = new HashSet<>();
        }

        public void addEntity(EntityType entity) {
            ExceptionHelper.checkNotNullArgument(entity, "entity");

            if (!addedIds.add(entity.getId())) {
                throw new IllegalArgumentException("Attempting to add multiple entities with the same ID: "
                        + entity.getId());
            }

            entities.add(entity);
        }

        public HearthStoneEntityDatabase<EntityType> create() {
            return new HearthStoneEntityDatabase<>(this);
        }
    }

    private final List<EntityType> entities;
    private final Map<EntityId, EntityType> entitiesById;

    private final ConcurrentMap<Keywords, List<EntityType>> entitiesByKeyword;

    private HearthStoneEntityDatabase(Builder<EntityType> builder) {
        this.entities = CollectionsEx.readOnlyCopy(builder.entities);
        this.entitiesById = toById(this.entities);
        this.entitiesByKeyword = new ConcurrentHashMap<>();
    }

    public static <EntityType extends HearthStoneEntity> HearthStoneEntityDatabase<EntityType> emptyDatabase() {
        return new Builder<EntityType>().create();
    }

    private static <EntityType extends HearthStoneEntity> Map<EntityId, EntityType> toById(List<EntityType> entities) {
        Map<EntityId, EntityType> result = CollectionsEx.newHashMap(entities.size());
        for (EntityType entity: entities) {
            if (result.put(entity.getId(), entity) != null) {
                throw new IllegalArgumentException("Found entities with the same ID: " + entity.getId());
            }
        }
        return result;
    }

    private List<EntityType> findByKeywords(Keywords keywords) {
        List<EntityType> result = new LinkedList<>();
        for (EntityType entity: entities) {
            if (keywords.isApplicable(entity)) {
                result.add(entity);
            }
        }
        return result;
    }

    public EntityType tryGetById(EntityId id) {
        return entitiesById.get(id);
    }

    public EntityType getById(EntityId id) {
        EntityType result = tryGetById(id);
        if (result == null) {
            throw new IllegalArgumentException("No such entity: " + id);
        }
        return result;
    }

    public List<EntityType> getByKeywords(Keyword... keywords) {
        Keywords allKeys = new Keywords(keywords);

        if (keywords.length == 0) {
            return entities;
        }

        return entitiesByKeyword.computeIfAbsent(allKeys, (key) -> CollectionsEx.readOnlyCopy(findByKeywords(key)));
    }

    public List<EntityType> getAll() {
        return entities;
    }

    private static final class Keywords {
        private final Keyword[] keywords;

        public Keywords(Keyword[] keywords) {
            this.keywords = keywords.clone();
            ExceptionHelper.checkNotNullElements(this.keywords, "keywords");
        }

        public boolean isApplicable(HearthStoneEntity entity) {
            Set<Keyword> entityKeywords = entity.getKeywords();
            for (Keyword keyword: keywords) {
                if (!entityKeywords.contains(keyword)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return 415 + Arrays.hashCode(this.keywords);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final Keywords other = (Keywords)obj;
            return Arrays.equals(this.keywords, other.keywords);
        }
    }
}
