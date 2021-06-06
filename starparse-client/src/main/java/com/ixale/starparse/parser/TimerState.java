package com.ixale.starparse.parser;

import com.ixale.starparse.domain.Entity;

import java.util.Objects;

public interface TimerState {
    /**
     * Seems to be currently specific to mercenary/commando hot
     * @return
     */
    default int getStacks() {
        return 0;
    }

    Integer getDuration();

    Long getSince();
    Long getLast();
    Entity getEffect();
}
