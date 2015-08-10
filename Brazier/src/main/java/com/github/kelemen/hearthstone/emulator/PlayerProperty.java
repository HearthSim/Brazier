package com.github.kelemen.hearthstone.emulator;

public interface PlayerProperty extends WorldProperty {
    public Player getOwner();

    @Override
    public default World getWorld() {
        return getOwner().getWorld();
    }
}
