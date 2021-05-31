package com.ixale.starparse.gui.popout;

import com.ixale.starparse.domain.ValueType;
import com.ixale.starparse.ws.RaidCombatMessage;

import java.net.URL;
import java.util.ResourceBundle;

public class RaidDtpsPopoutPresenter extends BaseRaidPopoutPresenter {

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		super.initialize(url, resourceBundle);

		sets.put(ValueType.TAKEN, new Set());
	}

	@Override
	protected ValueType getSetKey(final RaidCombatMessage message) {
		return ValueType.TAKEN;
	}

	@Override
	protected Integer getMinValueTotal() {
		return MIN_DISPLAY_VALUE;
	}

	@Override
	protected Integer getValueTotal(final RaidCombatMessage message) {
		return message.getCombatStats().getDamageTaken();
	}

	@Override
	protected Integer getValuePerSecond(final RaidCombatMessage message) {
		return message.getCombatStats().getDtps();
	}
}
