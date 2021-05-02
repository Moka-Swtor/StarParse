package com.ixale.starparse.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ixale.starparse.gui.Config;
import javafx.scene.paint.Color;

import com.ixale.starparse.domain.ConfigTimer.Condition.Type;
import com.ixale.starparse.gui.Marshaller.SerializeCallback;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class ConfigTimers implements Serializable, SerializeCallback {

	private static final long serialVersionUID = 1L;
	public static String COMMUNITY_PREFIX = "community: ";
	public static String DEFAULT_PREFIX = "default: ";

	public ConfigTimers() {
	}

	private final List<ConfigTimer> timers = new ArrayList<>();

	public Map<String, ConfigTimers> splitByPrefix(String prefix) {
		ConfigTimers defaultTimers = new ConfigTimers();
		ConfigTimers prefixTimers = new ConfigTimers();
		defaultTimers.allTimers = new ArrayList<>();
		prefixTimers.allTimers = new ArrayList<>();

		allTimers.forEach(configTimer -> {
			if (configTimer.getFolder() != null && configTimer.getFolder().startsWith(prefix)) {
				prefixTimers.allTimers.add(configTimer);
			} else {
				defaultTimers.allTimers.add(configTimer);
			}
		});

		return Map.of(DEFAULT_PREFIX, defaultTimers, prefix, prefixTimers);
	}

	public void addPrefixToFolders(String prefix) {
		timers.forEach(configTimer -> {
			if (configTimer.getFolder() != null && !configTimer.getFolder().startsWith(prefix)) {
				configTimer.setFolder(prefix + configTimer.getFolder());
			}
		});
	}

	@XStreamOmitField
	private transient List<ConfigTimer> allTimers = null;

	public List<ConfigTimer> getTimers() {
		if (timers.isEmpty()) {
			ConfigTimer.Condition condHo = new ConfigTimer.Condition();
			condHo.setType(Type.ABILITY_ACTIVATED);
			condHo.setAbilityGuid(801303458480128L);

			final ConfigTimer ho = new ConfigTimer();
			ho.setName("Hold the line");
			ho.setTrigger(condHo);
			ho.setInterval(29.0);
			ho.setColor(Color.AQUA);
			ho.setAudio("Yoda - Laught.wav");

			timers.add(ho);
		}
		if (allTimers == null) {
			allTimers = new ArrayList<>(timers);
		}

		return allTimers;
	}

	@Override
	public void beforeSerialize() {
		timers.clear();
		for (ConfigTimer timer: allTimers) {
			if (timer.isSystem()) {
				// save only if anything changed
				if (timer.isEnabled() && !timer.isSystemModified()) {
					continue;
				}
			}
			timers.add(timer);
		}
	}

	@Override
	public String toString() {
		return "Timers (" + timers.size() + ")";
	}

	public boolean wasEmpty() {
		return timers.isEmpty();
	}
}
