package com.ixale.starparse.parser;

import com.ixale.starparse.domain.Entity;

public interface TimerState {
    int getStacks();
    Long getSince();
    Long getLast();
    Entity getEffect();
    Integer getDuration();
}
