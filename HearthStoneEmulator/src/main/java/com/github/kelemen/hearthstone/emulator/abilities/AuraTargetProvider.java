package com.github.kelemen.hearthstone.emulator.abilities;

import com.github.kelemen.hearthstone.emulator.World;
import java.util.List;

public interface AuraTargetProvider<Source, Target> {
    public List<Target> getPossibleTargets(World world, Source source);
}
