package com.ixale.starparse.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ixale.starparse.domain.CharacterDiscipline;
import com.ixale.starparse.domain.Combat;
import com.ixale.starparse.domain.CombatActorState;
import com.ixale.starparse.domain.CombatInfo;
import com.ixale.starparse.domain.LocationInfo;
import com.ixale.starparse.domain.Raid;
import com.ixale.starparse.parser.Helpers;
import org.springframework.stereotype.Service;

import com.ixale.starparse.domain.Actor;
import com.ixale.starparse.domain.Actor.Type;
import com.ixale.starparse.domain.AttackType;
import com.ixale.starparse.domain.CombatSelection;
import com.ixale.starparse.domain.EffectKey;
import com.ixale.starparse.domain.Entity;
import com.ixale.starparse.domain.EntityGuid;
import com.ixale.starparse.domain.Event;
import com.ixale.starparse.domain.stats.CombatEventStats;

@Service("context")
public class Context {

	private String version;
	private String serverId;
	private LocationInfo locationInfo;

	private final Map<Object, Actor> actors = new HashMap<>();
	private final Map<Long, Entity> entities = new HashMap<>();
	private final Map<Long, AttackType> attacks = new HashMap<>();
	private final Map<Integer, Map<String, List<CombatEventStats>>> combatEvents = new HashMap<>();
	private final Map<Integer, CombatInfo> combatInfo = new HashMap<>();

	private Long tickFrom, tickTo;

	final private LinkedHashMap<EffectKey, ArrayList<Long[]>> effects = new LinkedHashMap<>();

	private String characterName;
	private String selectedPlayer;

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(final String serverId) {
		this.serverId = serverId;
	}

	public LocationInfo getLocationInfo() {
		return locationInfo;
	}

	public void setLocationInfo(final LocationInfo locationInfo) {
		this.locationInfo = locationInfo;
	}

	public Actor getActor(final String name, final Type type, final Long guid, final Long instanceId) {
		return getActor(name, type, guid, instanceId, null, null, null);
	}

	public Actor getActor(final String name, final Type type, final Long guid, final Long instanceId,
			final String x, final String y, final String angle) {
		if (instanceId == null) {
			return getActor(name, type, guid);
		}
		if (guid == null) {
			return getActor(name, type);
		}
		if (!actors.containsKey(instanceId)) {
			final Raid raid = locationInfo == null || locationInfo.getInstanceGuid() == null ? null : Helpers.getRaidByInstanceGuid(locationInfo.getInstanceGuid());
			if (raid != null && x != null && y != null && angle != null) {
				final Raid.Npc npc = raid.getNpcs().get(guid);
				try {
					if (npc != null && npc.getNameResolver() != null) {
						actors.put(instanceId, new Actor(
								npc.getNameResolver().getNpcName(npc.getName() == null ? name : npc.getName(), Float.valueOf(x), Float.valueOf(y), Float.valueOf(angle)),
								type, guid, instanceId));
						return actors.get(instanceId);
					}

				} catch (Exception ignored) {
				}
			}
			actors.put(instanceId, new Actor(name, type, guid, instanceId));
		}
		return actors.get(instanceId);
	}

	public Actor getActor(final String name, final Type type, final Long guid) {
		if (guid == null) {
			return getActor(name, type);
		}
		if (!actors.containsKey(guid)) {
			actors.put(guid, new Actor(name, type, guid));
		}
		return actors.get(guid);
	}

	public Actor getActor(final String name, final Type type) {
		if (!actors.containsKey(name)) {
			actors.put(name, new Actor(name, type));
		}
		return actors.get(name);
	}

	public boolean isActorHostile(final String playerName) {
		return playerName != null && actors.containsKey(playerName) && Boolean.TRUE.equals(actors.get(playerName).isHostile());
	}

	public Entity getEntity(final String name, final Long guid) {
		if (!entities.containsKey(guid)) {
			entities.put(guid, new Entity(name, guid));
		}
		return entities.get(guid);
	}

	public Map<Object, Actor> getActors() {
		return actors;
	}

	public CombatSelection getCombatSelection() {
		if (tickFrom == null && tickTo == null) {
			return null;
		}
		return new CombatSelection(null, null, tickFrom, tickTo);
	}

	public void setTickFrom(final Long tickFrom) {
		this.tickFrom = tickFrom;
	}

	public void setTickTo(final Long tickTo) {
		this.tickTo = tickTo;
	}

	public Long getTickFrom() {
		return tickFrom;
	}

	public Long getTickTo() {
		return tickTo;
	}

	public void addSelectedEffect(EffectKey effectKey, final ArrayList<Long[]> windows) {
		effects.put(effectKey, windows);
	}

	public LinkedHashMap<EffectKey, ArrayList<Long[]>> getSelectedEffects() {
		return effects;
	}

	public void addAttack(final AttackType type, long guid) {
		attacks.put(guid, type);
	}

	public void addAttacks(final Map<Long, AttackType> attacks) {
		for (final long guid : EntityGuid.MR) {
			attacks.put(guid, AttackType.MR);
		}
		for (final long guid : EntityGuid.FT) {
			attacks.put(guid, AttackType.FT);
		}
		this.attacks.putAll(attacks);
	}

	public Map<Long, AttackType> getAttacks() {
		return attacks;
	}

	public void addCombatEvent(final int combatId, final Actor player, final Event.Type type, final long timestamp) {
		if (!combatEvents.containsKey(combatId)) {
			combatEvents.put(combatId, new HashMap<>());
		}
		final String playerName = player.getName();
		if (!combatEvents.get(combatId).containsKey(playerName)) {
			combatEvents.get(combatId).put(playerName, new ArrayList<>());
		}
		combatEvents.get(combatId).get(playerName).add(new CombatEventStats(type, timestamp));
	}

	public List<CombatEventStats> getCombatEvents(int combatId, final String playerName) {
		return combatEvents.get(combatId) == null ? null : combatEvents.get(combatId).get(playerName);
	}

	public Integer findCombatIdByCombatEvent(final Event.Type type, final long timestamp, final String playerName) {
		for (final Integer combatId : combatEvents.keySet()) {
			if (combatEvents.get(combatId).get(playerName) == null) {
				continue;
			}
			for (final CombatEventStats ce : combatEvents.get(combatId).get(playerName)) {
				if (Math.abs(ce.getTimestamp() - timestamp) < 1000 && ce.getType().equals(type)) {
					return combatId;
				}
			}
		}
		return null;
	}

	public String getSelectedPlayer() {
		return selectedPlayer;
	}

	public void setSelectedPlayer(final String selectedPlayer) {
		this.selectedPlayer = selectedPlayer;
	}

	public String getCharacterName() {
		return characterName;
	}

	public void setCharacterName(final String characterName) {
		this.characterName = characterName;
	}

	public Map<Integer, CombatInfo> getCombatInfo() {
		return combatInfo;
	}

	public void addCombatPlayer(final Combat combat, final Actor player, final CharacterDiscipline discipline) {
		final CombatInfo combatInfo = this.combatInfo.get(combat.getCombatId());
		if (combatInfo != null) {
			combatInfo.addCombatPlayer(player, discipline);
		}
	}

	public void setCombatActorState(final Combat combat, final Actor actor, final Raid.Npc npc, final String currentHp, final String maxHp, final long tick) {
		if (maxHp == null || currentHp == null || maxHp.equals(currentHp)) {
			return;
		}
		final CombatInfo combatInfo = this.combatInfo.get(combat.getCombatId());
		if (combatInfo != null) {
			CombatActorState cas = combatInfo.getCombatActorStates().get(actor);
			if (cas == null) {
				cas = new CombatActorState(actor, npc, tick);
				combatInfo.getCombatActorStates().put(actor, cas);
			} else {
				cas.setTick(tick);
			}
			cas.setCurrentHp(Long.valueOf(currentHp));
			cas.setMaxHp(Long.valueOf(maxHp));
		}
	}

	public Collection<CombatActorState> getCombatActorStates(final Combat combat) {
		final CombatInfo combatInfo = this.combatInfo.get(combat.getCombatId());
		if (combatInfo != null) {
			return combatInfo.getCombatActorStates().values();
		}
		return null;
	}

	public void reset() {
		actors.clear();
		entities.clear();
		tickFrom = tickTo = null;
		effects.clear();
		combatEvents.clear();
		combatInfo.clear();
		locationInfo = null;
		version = null;
		serverId = null;
		selectedPlayer = null;
		characterName = null;
	}
}
