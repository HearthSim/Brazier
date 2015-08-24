package com.github.kelemen.brazier.actions;

import com.github.kelemen.brazier.Player;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public interface PlayActionRequirement {
    public static final PlayActionRequirement ALLOWED = (player) -> true;

    public boolean meetsRequirement(Player player);

    public static PlayActionRequirement merge(Collection<? extends PlayActionRequirement> requirements) {
        List<PlayActionRequirement> requirementsCopy = new ArrayList<>(requirements);
        ExceptionHelper.checkNotNullElements(requirementsCopy, "requirements");

        return (player) -> {
            for (PlayActionRequirement requirement: requirementsCopy) {
                if (!requirement.meetsRequirement(player)) {
                    return false;
                }
            }
            return true;
        };
    }
}
