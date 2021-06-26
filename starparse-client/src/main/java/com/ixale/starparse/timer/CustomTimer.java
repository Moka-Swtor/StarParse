package com.ixale.starparse.timer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ixale.starparse.domain.ConfigTimer;
import com.ixale.starparse.domain.ConfigTimer.Condition;
import com.ixale.starparse.domain.Entity;
import com.ixale.starparse.gui.SoundManager;
import com.ixale.starparse.parser.TimerState;
import com.ixale.starparse.timer.TimerManager.RaidPullTimer;

import com.ixale.starparse.ws.Utils;
import javafx.scene.paint.Color;

public class CustomTimer extends BaseTimer {

	private final ConfigTimer timer;
	private final List<ConfigTimer> nextTimers = new ArrayList<>(), cancelTimers = new ArrayList<>();
	private final BaseTimer systemTimer;

	private Integer countdownThreshold = null, soundThreshold = null;
	private Long lastSample = null, lastSound = null;

	public CustomTimer(final ConfigTimer timer) {
		super(timer.getName(), null, (int) Math.round(timer.getInterval() * 1000),
			timer.getRepeat() != null && timer.getRepeat() > 0 && timer.getInterval() > 0 ? (int) Math.round(timer.getInterval() * 1000) : null,
			timer.getRepeat(),
			timer.getCancel() != null && Condition.Type.COMBAT_END.equals(timer.getCancel().getType()) ? Scope.COMBAT : Scope.ANY);

		this.timer = timer;
		this.systemTimer = null;
	}

	public CustomTimer(final ConfigTimer timer, final BaseTimer systemTimer) {
		this(timer, systemTimer, null);
	}

	public CustomTimer(final ConfigTimer timer, final BaseTimer systemTimer, final Integer interval) {
		super(systemTimer.getName(), timer.getName(),
			interval == null ? systemTimer.getFirstInterval() : interval,
			systemTimer.getRepeatInterval(),
			null, // MAX
			systemTimer.getScope());

		this.timer = timer;
		this.systemTimer = systemTimer;
	}

	@Override
	public boolean isAbilityTimer() {

		return timer!=null && timer.getTimerType()!=null && timer.getTimerType().isClassTimer();
		//TODO: use previous line when working again on grid layout feature
//		return super.isAbilityTimer();
	}

	@Override
	public void start(Long timeFrom) {
		super.start(timeFrom);

		if (timer.getCountdownCount() != null) {
			countdownThreshold = timer.getCountdownCount() * 1000 + (TimerManager.POLLING / 2);
		}
		if (timer.getAudio() != null && timer.getSoundOffset() != null && timer.getSoundOffset() > 1) {
			soundThreshold = timer.getSoundOffset() * 1000 + (TimerManager.POLLING / 2);
		}
	}

	@Override
	protected void expiredRepeat(long timeTo) {
		if (timer.getAudio() != null && (lastSound == null || lastSound < timeTo)) {
			if (!TimerManager.isMuted() || Scope.ANY.equals(getScope())) {
				SoundManager.play(timer.getAudio(), timer.getVolume() == null ? null : timer.getVolume() * 1.0);
			}
			lastSound = timeTo;
		}
	}

	@Override
	protected void runningTick(long timeTo, long timeRemaining) {
		if (countdownThreshold != null && countdownThreshold >= timeRemaining) {
			int sample = (int) Math.round((timeRemaining + 300) / 1000.0);
			if (sample > 0 && (lastSample == null || (timeTo - (sample * 1000)) > lastSample)) {
				if (!TimerManager.isMuted() || Scope.ANY.equals(getScope())) {
					SoundManager.play(sample, timer.getCountdownVoice(),
						timer.getCountdownVolume() == null ? null : timer.getCountdownVolume() * 1.0);
				}

				lastSample = timeTo - (sample * 1000); // endures random restarts from other threads etc
			}
		}
		if (soundThreshold != null && soundThreshold >= timeRemaining && (lastSound == null || lastSound < timeTo)) {
			if (!TimerManager.isMuted() || Scope.ANY.equals(getScope())) {
				SoundManager.play(timer.getAudio(), timer.getVolume() == null ? null : timer.getVolume() * 1.0);
			}
			lastSound = timeTo;
		}
	}

	@Override
	protected void expired(long timeTo) {
		expiredRepeat(timeTo);

		for (final ConfigTimer nextTimer: nextTimers) {
			TimerManager.startTimer(nextTimer, getTimeTo());
		}
		for (final ConfigTimer cancelTimer: cancelTimers) {
			TimerManager.stopTimer(cancelTimer.getName());
		}
	}

	@Override
	public boolean isVisual() {
		return timer.getColor() != null;
	}

	public Color getColor() {
		return timer.getColor();
	}

	public String getAbilityTimerTrigram() {
		return timer.getAbilityTimerTrigram();
	}

	public Entity computeEffect() {
		return timer == null || timer.getTrigger() == null
				? new Entity(getName(), 0L)
				: timer.getTrigger().computeEntity();
	}

	public List<ConfigTimer> getNextTimers() {
		return nextTimers;
	}

	public List<ConfigTimer> getCancelTimers() {
		return cancelTimers;
	}

	public BaseTimer getSystemTimer() {
		return systemTimer;
	}

	@Override
	public boolean doOverrideExpiringThreshold() {
		return systemTimer != null && (systemTimer instanceof RaidPullTimer);
	}

	@Override
	public String toString() {
		List<Utils.Pair> pairList = List.of(
				Utils.Pair.of("timer", this.timer),
				Utils.Pair.of("nextTimers", this.nextTimers),
				Utils.Pair.of("cancelTimers", this.cancelTimers),
				Utils.Pair.of("systemTimer", this.systemTimer),
				Utils.Pair.of("countdownThreshold", this.countdownThreshold),
				Utils.Pair.of("soundThreshold", this.soundThreshold),
				Utils.Pair.of("lastSample", this.lastSample),
				Utils.Pair.of("lastSound", this.lastSound)
		);
		return "CustomTimer{" +
				"timer=" + this.timer +
				pairList.stream().filter(Utils.Pair::hasValue).map(Utils.Pair::toString).collect(Collectors.joining(", ")) +
				'}'+super.toString();
	}

	public boolean wouldHaveSameTimerState(CustomTimer otherTimer) {
		return Objects.equals(getTimeFrom(), otherTimer.getTimeFrom())
				&& Objects.equals(getFirstInterval(),otherTimer.getFirstInterval())
				&& Objects.equals(getName(),otherTimer.getName())
				&& (timer == null || Objects.equals(timer.getTrigger(),otherTimer.timer.getTrigger()));
	}

	public TimerState toTimerState() {
		return new TimerState() {
			@Override
			public int getStacks() {
				return 0;
			}

			@Override
			public Long getSince() {
				return getTimeFrom();
			}

			@Override
			public Long getLast() {
				return getTimeFrom();
			}

			@Override
			public Entity getEffect() {
				return computeEffect();
			}

			@Override
			public Integer getDuration() {
				return getFirstInterval();
			}
		};
	}
}
