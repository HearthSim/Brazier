package com.github.kelemen.brazier;

public interface PlayerProperty extends WorldProperty {
    public Player getOwner();

    @Override
    public default World getWorld() {
        return getOwner().getWorld();
    }
}
