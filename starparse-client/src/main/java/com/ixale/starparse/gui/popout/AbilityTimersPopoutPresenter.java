package com.ixale.starparse.gui.popout;

import com.ixale.starparse.domain.ConfigPopoutDefault;
import com.ixale.starparse.domain.ConfigTimer;
import com.ixale.starparse.domain.ConfigTimers;
import com.ixale.starparse.parser.TimerState;
import com.ixale.starparse.time.TimeUtils;
import com.ixale.starparse.timer.BaseTimer;
import com.ixale.starparse.timer.CustomTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AbilityTimersPopoutPresenter extends GridPopoutPresenter{

    private static final Logger LOGGER = LoggerFactory.getLogger(AbilityTimersPopoutPresenter.class);
    private static final long startMillis = System.currentTimeMillis();

    private final Map<CustomTimer,TimerState> customTimers = new HashMap<>();

    private Color inactiveColor;
    private Color activeColor;
    private double inactiveOpacity;

    @Override
    protected int getMinRows() {
        return 2;
    }

    @Override
    boolean repaintTimer(GraphicsContext gc, double width, double height, TimerState timerState) {
        debug("non interesting repaint");
        customTimers.entrySet().stream()
                .filter(e-> timerState==e.getValue())
                .map(Map.Entry::getKey)
                .filter(customTimer -> customTimer.getTimeRemaining()!=null)
                .findAny()
                .ifPresent(customTimer -> {
            this.repaintTimer(customTimer, !customTimer.isNew());
        });
        return true;
    }

    @Override
    public void tickFrames(Collection<TimerFrame> timerFrames) {
        //  debug("tick ability: "+timerFrames.stream().map(TimerFrame::toString).collect(Collectors.joining(", ")));
    }

    private void debug(String s) {
        LOGGER.debug(s);
        System.out.println(s);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //noinspection SuspiciousNameCombination
        setSlotWidth(DEFAULT_SLOT_HEIGHT);
        setSlotRows(2);
        super.initialize(url, resourceBundle);
    }

    @Override
    protected void onFrameCoordsUpdated(TimerFrame frame) {
        characterConfig().saveFrameDisposition(frame.name, frame.getCol()+";"+frame.getRow());
    }

    @Override
    protected void removeFromFrames(TimerFrame frame) {
        super.removeFromFrames(frame);
        characterConfig().saveFrameDisposition(frame.name, null);
    }

    @Override
    protected void initFrameColAndRow(TimerFrame frame) {
        String disposition = characterConfig().getDisposition(frame.name);
        if (disposition != null && disposition.indexOf(';') > 0) {
            String[] split = disposition.split(";");
            int col = Integer.parseInt(split[0]);
            int row = Integer.parseInt(split[1]);
            frame.setColAndRow(col, row);
        }
    }

    public void addOrUpdateOrCompleteTimer(final BaseTimer timer) {
        if(!timer.isAbilityTimer())
            return;
        if(!(timer instanceof CustomTimer))
            return;
        CustomTimer customTimer = (CustomTimer) timer;
        customTimer.update(TimeUtils.getCurrentTime());
        Optional<CustomTimer> maybeExistingTimer = customTimers.keySet().stream().filter(cu -> comparableToOtherTimer(customTimer).test(cu)).findAny();
        if (maybeExistingTimer.isPresent()) {
            CustomTimer existingTimer = maybeExistingTimer.get();
            boolean wasNew = existingTimer.isNew();
            existingTimer.update(customTimer.getTimeFrom());
            if (wasNew) {
                debug("[" + (System.currentTimeMillis() - startMillis) + "] starting existing timer : " + customTimer);
                this.repaintTimer(existingTimer, true);
            } else {
                // TODO: depending on remaining time customize timer ending?
               // debug("["+(System.currentTimeMillis()-startMillis)+  "] update existing timer : "+customTimer.getName() + " remaining: "+customTimer.getTimeRemaining());
            }
            return;
        }

        debug("["+(System.currentTimeMillis()-startMillis)+  "] adding new timer : "+customTimer);
        this.customTimers.put(customTimer, customTimer.toTimerState()); // CustomTimer::getAbilityTimerTrigram, CustomTimer::toTimerState
        super.setTimersStates(this.customTimers.entrySet().stream().collect(Collectors.toMap(o -> o.getKey().getAbilityTimerTrigram(), Map.Entry::getValue)));
        this.repaintTimer(customTimer, true);
    }

    private Predicate<CustomTimer> comparableToOtherTimer(CustomTimer customTimer) {
        return ct -> Objects.equals(ct.getName(), customTimer.getName())
                && Objects.equals(ct.getFirstInterval(), customTimer.getFirstInterval())
                && ct.computeEffect().attributeEquals(customTimer.computeEffect());
    }

    public void removeTimer(BaseTimer timer) {
        this.customTimers.keySet().stream()
                .filter(ct -> ct.getAbilityTimerTrigram().equals(((CustomTimer) timer).getAbilityTimerTrigram()))
                .findAny().ifPresent(customTimer -> {
            debug("remove Ability timer : " + timer);
            this.repaintTimer(customTimer, false);
            customTimer.resetState();
        });
    }

    public void resetTimers(ConfigTimers configTimers, ConfigPopoutDefault popoutDefault, String discipline) {
        super.resetTimers();
        Map<CustomTimer, TimerState> customAbilityTimers = configTimers.getTimers().stream()
                .filter(configTimer -> configTimer.getTimerType() != null)
                .filter(configTimer -> configTimer.getTimerType().isClassTimer())
                .filter(configTimer -> StringUtils.isEmpty(discipline)
                        || configTimer.getCharacterDiscipline() != null && configTimer.getCharacterDiscipline().getFullName().equals(discipline))
                .map(CustomTimer::new)
                .collect(Collectors.toMap(ct -> ct, CustomTimer::toTimerState));
        customAbilityTimers.keySet().forEach(customTimer -> customTimer.start(TimeUtils.getCurrentTime()));

        this.inactiveColor = popoutDefault.getInactiveColor();
        this.inactiveOpacity = popoutDefault.getInactiveOpacity() != null ? popoutDefault.getInactiveOpacity() / 100. : 0.70d;
        this.activeColor = popoutDefault.getActiveColor();
        this.customTimers.clear();
        this.customTimers.putAll(customAbilityTimers);
        super.setTimersStates(this.customTimers.entrySet().stream().collect(Collectors.toMap(o->o.getKey().getAbilityTimerTrigram(), Map.Entry::getValue)));
        debug("reset ability settings");
    }



    private void repaintTimer(CustomTimer customTimer, boolean timerStart) {
        TimerState timerState = customTimers.get(customTimer);
        TimerFrame timerFrame = super.getFrame(timerState);
        if (timerFrame == null) {
            return;
        }
        double width = timerFrame.pane.getWidth()-2;
        double height = timerFrame.pane.getHeight()-2;
        Canvas canvas = retrieveCanvas(timerFrame, (int) Math.max(width, height));
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        if (!timerStart) {
            gc.setFill(activeColor);
            gc.fillRoundRect(0, 0, width, height, 0, 0);
            gc.clearRect(5, 5, width-9, height-9);
            debug("[" + (System.currentTimeMillis() - startMillis) + "] unpaint timer : " + customTimer.getName());
        } else {
            gc.setFill(inactiveColor.deriveColor(0, 1, .9, inactiveOpacity));
            gc.fillRoundRect(0, 0, width, height, 0, 0);
            debug("[" + (System.currentTimeMillis() - startMillis) + "] paint timer : " + customTimer.getName());
        }
    }
}
