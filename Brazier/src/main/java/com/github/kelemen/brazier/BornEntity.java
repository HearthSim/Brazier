package com.github.kelemen.brazier;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public interface BornEntity {
    public static final Comparator<BornEntity> CMP = (entity1, entity2) -> {
        return Long.compare(entity1.getBirthDate(), entity2.getBirthDate());
    };

    public long getBirthDate();

    public static void sortEntities(List<? extends BornEntity> entities) {
        entities.sort(CMP);
    }

    public static void sortEntities(BornEntity[] entities) {
        Arrays.sort(entities, CMP);
    }
}
