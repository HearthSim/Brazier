package com.github.kelemen.brazier;

import com.github.kelemen.brazier.actions.UndoAction;
import com.github.kelemen.brazier.actions.UndoBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

public final class SecretContainer implements PlayerProperty {
    public static final int MAX_SECRETS = 5;

    private final Player owner;
    private final List<Secret> secrets;
    private final List<Secret> secretsView;

    public SecretContainer(Player owner) {
        ExceptionHelper.checkNotNullArgument(owner, "owner");

        this.owner = owner;
        this.secrets = new ArrayList<>(MAX_SECRETS);
        this.secretsView = Collections.unmodifiableList(secrets);
    }

    public List<Secret> getSecrets() {
        return secretsView;
    }

    public boolean isFull() {
        return secrets.size() >= MAX_SECRETS;
    }

    @Override
    public Player getOwner() {
        return owner;
    }

    public Secret findById(EntityId secretId) {
        ExceptionHelper.checkNotNullArgument(secretId, "secretId");

        for (Secret secret: secrets) {
            if (secretId.equals(secret.getSecretId())) {
                return secret;
            }
        }
        return null;
    }

    public UndoAction addSecret(Secret secret) {
        ExceptionHelper.checkNotNullArgument(secret, "secret");

        if (isFull()) {
            return UndoAction.DO_NOTHING;
        }

        secrets.add(secret);
        UndoAction activateUndo = secret.activate();

        return () -> {
            activateUndo.undo();
            secrets.remove(secrets.size() - 1);
        };
    }

    public UndoAction stealActivatedSecret(SecretContainer other, Secret secret) {
        UndoAction removeUndo = other.removeSecretLeaveActive(secret);
        if (isFull()) {
            UndoAction deactivateUndo = secret.deactivate();
            return () -> {
                deactivateUndo.undo();
                removeUndo.undo();
            };
        }

        UndoAction replaceOwnerUndo = secret.setOwner(owner);
        secrets.add(secret);

        return () -> {
            secrets.remove(secrets.size() - 1);
            replaceOwnerUndo.undo();
            removeUndo.undo();
        };
    }

    private UndoAction removeSecretLeaveActive(Secret secret) {
        ExceptionHelper.checkNotNullArgument(secret, "secret");

        int secretCount = secrets.size();
        for (int i = 0; i < secretCount; i++) {
            if (secrets.get(i) == secret) {
                secrets.remove(i);
                int origIndex = i;
                return () -> {
                    secrets.add(origIndex, secret);
                };
            }
        }
        return UndoAction.DO_NOTHING;
    }

    public UndoAction removeSecret(Secret secret) {
        ExceptionHelper.checkNotNullArgument(secret, "secret");
        // TODO: Show secret to the opponent

        UndoAction removeUndo = removeSecretLeaveActive(secret);
        UndoAction deactivateUndo = secret.deactivate();
        return () -> {
            deactivateUndo.undo();
            removeUndo.undo();
        };
    }

    public UndoAction removeAllSecrets() {
        if (secrets.isEmpty()) {
            return UndoAction.DO_NOTHING;
        }

        UndoBuilder result = new UndoBuilder(secrets.size() + 1);
        List<Secret> currentSecrets = new ArrayList<>(secrets);
        secrets.clear();
        result.addUndo(() -> secrets.addAll(currentSecrets));

        for (Secret secret: currentSecrets) {
            result.addUndo(secret.deactivate());
        }

        // TODO: Show secrets to the opponent

        return result;
    }

    public boolean hasSecret() {
        return !secrets.isEmpty();
    }
}
