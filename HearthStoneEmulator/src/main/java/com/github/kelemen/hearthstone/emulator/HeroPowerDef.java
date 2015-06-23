package com.github.kelemen.hearthstone.emulator;

import com.github.kelemen.hearthstone.emulator.cards.CardPlayActionDef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class HeroPowerDef implements HearthStoneEntity {
    public static final HeroPowerDef DO_NOTHING = new HeroPowerDef.Builder(new HeroPowerId("")).create();

    public static final class Builder {
        private final HeroPowerId id;
        private String description;
        private int manaCost;
        private int maxUseCount;
        private final List<CardPlayActionDef> actions;

        public Builder(HeroPowerId id) {
            ExceptionHelper.checkNotNullArgument(id, "id");
            this.id = id;
            this.manaCost = 2;
            this.maxUseCount = 1;
            this.actions = new ArrayList<>();
            this.description = "";
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setManaCost(int manaCost) {
            ExceptionHelper.checkArgumentInRange(manaCost, 0, Integer.MAX_VALUE, "manaCost");
            this.manaCost = manaCost;
        }

        public void setMaxUseCount(int maxUseCount) {
            ExceptionHelper.checkArgumentInRange(maxUseCount, 0, Integer.MAX_VALUE, "maxUseCount");
            this.maxUseCount = maxUseCount;
        }

        public void addAction(CardPlayActionDef action) {
            ExceptionHelper.checkNotNullArgument(action, "action");
            this.actions.add(action);
        }

        public HeroPowerDef create() {
            return new HeroPowerDef(this);
        }
    }

    private final HeroPowerId id;
    private final String description;
    private final int manaCost;
    private final int maxUseCost;
    private final List<CardPlayActionDef> actions;

    private HeroPowerDef(Builder builder) {
        this.id = builder.id;
        this.manaCost = builder.manaCost;
        this.maxUseCost = builder.maxUseCount;
        this.actions = CollectionsEx.readOnlyCopy(builder.actions);
        this.description = builder.description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public Set<Keyword> getKeywords() {
        return Collections.emptySet();
    }

    public String getDisplayName() {
        // TODO: Allow customizing the display name.
        return id.getName();
    }

    @Override
    public HeroPowerId getId() {
        return id;
    }

    public int getManaCost() {
        return manaCost;
    }

    public int getMaxUseCost() {
        return maxUseCost;
    }

    public List<CardPlayActionDef> getActions() {
        return actions;
    }
}
