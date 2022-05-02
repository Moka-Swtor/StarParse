package com.ixale.starparse.service.dao;

import com.ixale.starparse.domain.Actor;
import com.ixale.starparse.domain.Combat;
import com.ixale.starparse.domain.CombatSelection;
import com.ixale.starparse.domain.Effect;
import com.ixale.starparse.domain.Event;
import com.ixale.starparse.domain.stats.AbsorptionStats;
import com.ixale.starparse.domain.stats.ChallengeStats;
import com.ixale.starparse.domain.stats.CombatMitigationStats;
import com.ixale.starparse.domain.stats.CombatStats;
import com.ixale.starparse.domain.stats.CombatTickStats;
import com.ixale.starparse.domain.stats.DamageDealtStats;
import com.ixale.starparse.domain.stats.DamageTakenStats;
import com.ixale.starparse.domain.stats.HealingDoneStats;
import com.ixale.starparse.domain.stats.HealingTakenStats;

import java.util.List;
import java.util.Set;

public interface CombatDao {

	void storeCombats(List<Combat> combats, Combat currentCombat) throws Exception;

	Combat findCombat(int combatId) throws Exception;

	List<Combat> getCombats() throws Exception;

	CombatStats getCombatStats(Combat combat, CombatSelection combatSel, String playerName) throws Exception;

	List<CombatTickStats> getCombatTicks(Combat combat, CombatSelection combatSel, String playerName) throws Exception;

	List<Actor> getCombatActors(Combat combat, Actor.Role role, CombatSelection combatSel) throws Exception;

	List<Event> getCombatEvents(Combat combat, Set<Event.Type> filterFlags,
			Actor filterSource, Actor filterTarget, String filterSearch,
			CombatSelection combatSel, String playerName) throws Exception;

	List<DamageDealtStats> getDamageDealtStatsSimple(Combat combat, CombatSelection combatSel, String playerName) throws Exception;

	List<DamageDealtStats> getDamageDealtStats(Combat combat, boolean byTargetType, boolean byTargetInstance, boolean byAbility,
			CombatSelection combatSel, String playerName) throws Exception;

	List<HealingDoneStats> getHealingDoneStats(Combat combat, boolean byTarget, boolean byAbility, CombatSelection combatSel, String playerName) throws Exception;

	CombatMitigationStats getCombatMitigationStats(Combat combat, CombatSelection combatSel, String playerName) throws Exception;

	List<DamageTakenStats> getDamageTakenStats(Combat combat, boolean bySourceType, boolean bySourceInstance, boolean byAbility,
			CombatSelection combatSel, String playerName) throws Exception;

	List<HealingTakenStats> getHealingTakenStats(Combat combat, boolean bySource, boolean byAbility, CombatSelection combatSel, String playerName) throws Exception;

	List<AbsorptionStats> getAbsorptionStats(Combat combat, CombatSelection combatSel, String playerName) throws Exception;

	List<ChallengeStats> getCombatChallengeStats(Combat combat, CombatSelection combatSel, String playerName) throws Exception;

	List<Effect> getCombatEffects(Combat combat, CombatSelection combatSel) throws Exception;

	void reset() throws Exception;
}
