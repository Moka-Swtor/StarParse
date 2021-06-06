package com.ixale.starparse.gui.popout;

import com.ixale.starparse.parser.TimerState;
import com.ixale.starparse.time.TimeUtils;
import com.ixale.starparse.timer.BaseTimer;
import com.ixale.starparse.timer.CustomTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AbilityTimersPopoutPresenter extends GridPopoutPresenter{

    private static final Logger LOGGER = LoggerFactory.getLogger(AbilityTimersPopoutPresenter.class);
    private static final long startMillis = System.currentTimeMillis();

    private final Map<Integer, CustomTimer> timerStateMap = new HashMap<>();
    private final Set<CustomTimer> customTimers = new HashSet<>();


    @Override
    protected int getMinRows() {
        return 2;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //noinspection SuspiciousNameCombination
        setSlotWidth(DEFAULT_SLOT_HEIGHT);
        setSlotRows(2);
        super.initialize(url, resourceBundle);
    }

    public void addOrUpdateOrCompleteTimer(final BaseTimer timer) {
        if(!timer.isAbilityTimer())
            return;
        if(!(timer instanceof CustomTimer))
            return;
        CustomTimer customTimer = (CustomTimer) timer;
        customTimer.update(TimeUtils.getCurrentTime());
        boolean existingTimer = customTimers.stream().anyMatch(comparableToOtherTimer(customTimer));
        if (existingTimer) {
            return;
        }

        System.out.println("["+(System.currentTimeMillis()-startMillis)+  "] update ability popout : "+customTimer);
        this.customTimers.add(customTimer);
        super.setTimersStates(this.customTimers.stream().collect(Collectors.toMap(CustomTimer::getAbilityTimerTrigram, CustomTimer::toTimerState)));
    }

    private Predicate<CustomTimer> comparableToOtherTimer(CustomTimer customTimer) {
        return ct -> Objects.equals(ct.getTimeFrom(), customTimer.getTimeFrom())
                && Objects.equals(ct.getName(), customTimer.getName())
                && Objects.equals(ct.getFirstInterval(), customTimer.getFirstInterval())
                && ct.computeEffect().attributeEquals(customTimer.computeEffect());
    }

    public void removeTimer(BaseTimer timer) {
        CustomTimer customTimer = (CustomTimer) timer;
        Set<CustomTimer> customTimers = this.customTimers.stream().filter(Predicate.not(comparableToOtherTimer(customTimer))).collect(Collectors.toSet());
        this.customTimers.clear();
        this.customTimers.addAll(customTimers);
        //TODO
        System.out.println("remove Ability timer : "+timer);
    }

    public void resetTimers() {
        // TODO
        System.out.println("reset ability settings");
    }

    @Override
    public void tickFrames(Collection<TimerFrame> timerFrames) {
        if(timerFrames==null || timerFrames.isEmpty())
            return;
        // TODO
        System.out.println("tick ability: "+timerFrames.stream().map(TimerFrame::toString).collect(Collectors.joining(", ")));
    }


    // TODO: replace with meaningfull paint
    @Override
    boolean repaintTimer(GraphicsContext gc, double width, double height, TimerState timerState) {
        gc.clearRect(0, 0, width, height);
        if (timerState.getDuration() == null) {
            gc.setFill(Color.LIMEGREEN);
        } else {
            gc.setFill(Color.DARKGREEN);
        }
        gc.fillRoundRect(0, 0, width, height, 0, 0);
        return true;
    }
}
