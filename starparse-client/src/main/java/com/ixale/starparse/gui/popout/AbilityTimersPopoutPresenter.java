package com.ixale.starparse.gui.popout;

import com.ixale.starparse.domain.ConfigTimer;
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
        Optional<CustomTimer> maybeExistingTimer = customTimers.stream().filter(cu -> comparableToOtherTimer(customTimer).test(cu)).findAny();
        if (maybeExistingTimer.isPresent()) {
            CustomTimer existingTimer = maybeExistingTimer.get();
            boolean wasNew = existingTimer.isNew();
            existingTimer.update(customTimer.getTimeFrom());
            if (wasNew) {
                System.out.println("[" + (System.currentTimeMillis() - startMillis) + "] starting existing timer : " + customTimer);
            } else {
              //  System.out.println("["+(System.currentTimeMillis()-startMillis)+  "] update existing timer : "+customTimer.getName() + " remaining: "+customTimer.getTimeRemaining());
            }
            return;
        }

        System.out.println("["+(System.currentTimeMillis()-startMillis)+  "] adding new timer : "+customTimer);
        this.customTimers.add(customTimer);
        super.setTimersStates(this.customTimers.stream().collect(Collectors.toMap(CustomTimer::getAbilityTimerTrigram, CustomTimer::toTimerState)));
    }

    private Predicate<CustomTimer> comparableToOtherTimer(CustomTimer customTimer) {
        return ct -> Objects.equals(ct.getName(), customTimer.getName())
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

    public void resetTimers(List<ConfigTimer> timers) {
        super.resetTimers();
        List<CustomTimer> customAbilityTimers = timers.stream()
                .filter(configTimer -> configTimer.getTimerType() != null)
                .filter(configTimer -> configTimer.getTimerType().isClassTimer())
                .map(CustomTimer::new)
                .collect(Collectors.toList());
        customAbilityTimers.forEach(customTimer -> customTimer.start(TimeUtils.getCurrentTime()));

        this.customTimers.clear();
        this.customTimers.addAll(customAbilityTimers);
        super.setTimersStates(this.customTimers.stream().collect(Collectors.toMap(CustomTimer::getAbilityTimerTrigram, CustomTimer::toTimerState)));
        System.out.println("reset ability settings");
    }

    @Override
    public void tickFrames(Collection<TimerFrame> timerFrames) {
        if(timerFrames==null || timerFrames.isEmpty())
            return;
        // TODO - seems like nothing to do
        System.out.println("tick ability: "+timerFrames.stream().map(TimerFrame::toString).collect(Collectors.joining(", ")));
    }


    // TODO: replace with meaningfull paint
    @Override
    boolean repaintTimer(GraphicsContext gc, double width, double height, TimerState timerState) {
        System.out.println("[" + (System.currentTimeMillis() - startMillis) + "] paint timerState : " + timerState.getSince());
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
