package com.github.kelemen.brazier.ui;

import com.github.kelemen.brazier.Hero;
import com.github.kelemen.brazier.PlayerId;
import com.github.kelemen.brazier.TargetableCharacter;
import com.github.kelemen.brazier.TargeterDef;
import com.github.kelemen.brazier.actions.TargetNeed;
import com.github.kelemen.brazier.minions.Minion;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;
import javax.swing.JComponent;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

public final class PlayerTargetNeed {
    private final TargeterDef targeterDef;
    private final TargetNeed targetNeed;

    public PlayerTargetNeed(
            TargeterDef targeterDef,
            TargetNeed targetNeed) {
        ExceptionHelper.checkNotNullArgument(targeterDef, "targeterDef");
        ExceptionHelper.checkNotNullArgument(targetNeed, "targetNeed");

        this.targeterDef = targeterDef;
        this.targetNeed = targetNeed;
    }

    public PlayerId getPlayerId() {
        return targeterDef.getPlayerId();
    }

    public TargeterDef getTargeterDef() {
        return targeterDef;
    }

    public TargetNeed getTargetNeed() {
        return targetNeed;
    }

    public boolean isAllowedTarget(TargetableCharacter target) {
        if (!target.isTargetable(targeterDef)) {
            return false;
        }
        if (target instanceof Minion) {
            return isAllowedMinion((Minion)target);
        }
        if (target instanceof Hero) {
            return isAllowedHero((Hero)target);
        }
        return false;
    }

    public boolean isAllowedMinion(Minion minion) {
        return targetNeed.getAllowMinionCondition().test(getPlayerId(), minion);
    }

    public boolean isAllowedHero(Hero hero) {
        return targetNeed.getAllowHeroCondition().test(getPlayerId(), hero);
    }

    public static ListenerRef trackForTarget(
            TargetManager targetManager,
            JComponent component,
            TargetableCharacter target,
            Consumer<Boolean> highlightSetter) {
        ExceptionHelper.checkNotNullArgument(targetManager, "targetManager");
        ExceptionHelper.checkNotNullArgument(component, "component");
        ExceptionHelper.checkNotNullArgument(target, "target");
        ExceptionHelper.checkNotNullArgument(highlightSetter, "highlightSetter");

        MouseListener listener = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                UiTargetCondition condition = targetManager.getCondition();
                if (condition != null) {
                    Object conditionObj = condition.getCondition();
                    if (conditionObj instanceof PlayerTargetNeed) {
                        boolean allowed = ((PlayerTargetNeed)conditionObj).isAllowedTarget(target);
                        if (allowed) {
                            highlightSetter.accept(false);
                            condition.getCallback().accept(target.getTargetId());
                        }
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                highlightSetter.accept(false);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                Object conditionObj = targetManager.getConditionObj();
                if (conditionObj instanceof PlayerTargetNeed) {
                    boolean allowed = ((PlayerTargetNeed)conditionObj).isAllowedTarget(target);
                    highlightSetter.accept(allowed);
                }
            }
        };
        component.addMouseListener(listener);
        return new ListenerRef() {
            private volatile boolean registered = true;

            @Override
            public boolean isRegistered() {
                return registered;
            }

            @Override
            public void unregister() {
                component.removeMouseListener(listener);
                registered = false;
            }
        };
    }
}
