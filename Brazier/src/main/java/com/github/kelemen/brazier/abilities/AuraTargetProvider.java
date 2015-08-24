package com.github.kelemen.brazier.abilities;

import com.github.kelemen.brazier.World;
import java.util.List;

public interface AuraTargetProvider<Source, Target> {
    public List<Target> getPossibleTargets(World world, Source source);
}
