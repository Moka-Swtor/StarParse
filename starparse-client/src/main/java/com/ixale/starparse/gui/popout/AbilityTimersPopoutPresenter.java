package com.ixale.starparse.gui.popout;

import com.ixale.starparse.domain.Actor;
import com.ixale.starparse.domain.Combat;
import com.ixale.starparse.domain.stats.CombatStats;
import com.ixale.starparse.parser.Parser;
import com.ixale.starparse.parser.TimerState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Map;
import java.util.stream.Collectors;

public class AbilityTimersPopoutPresenter extends GridPopoutPresenter{
    @Override
    protected void refreshCombatStats(Combat combat, CombatStats stats) throws Exception {
        // nothing to do
    }

    @Override
    public void resetCombatStats() {
        // nothing to do
    }

    // TODO: replace with initilisation of list of timers identified as ability timers
    @Deprecated
    public void setActorStates(final Map<Actor, Parser.ActorState> actorStates) {

        Map<String, TimerState> collect = actorStates.entrySet().stream()
                .filter(this::isSelfOrPlayer)
                .collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
        super.setTimersStates(collect);
    }

    // TODO: remove
    private boolean isSelfOrPlayer(Map.Entry<Actor, Parser.ActorState> entry) {
        Actor.Type type = entry.getKey().getType();
        return Actor.Type.SELF == type || Actor.Type.PLAYER == type;
    }

    // TODO: replace with meaningfull paint
    @Override
    void repaintTimer(GraphicsContext gc, double width, double height, TimerState timerState) {
        gc.clearRect(0, 0, width, height);
        if (timerState.getDuration() == null) {
            gc.setFill(Color.LIMEGREEN);
        } else {
            gc.setFill(Color.DARKGREEN);
        }
        gc.fillRoundRect(0, 0, width, height, 0, 0);
    }
}
