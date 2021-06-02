package com.ixale.starparse.parser;

import static com.ixale.starparse.domain.EntityGroup.*;
import static com.ixale.starparse.domain.EntityGuid.*;

import java.util.*;
import java.util.stream.Collectors;

import com.ixale.starparse.domain.Actor;
import com.ixale.starparse.domain.CharacterDiscipline;
import com.ixale.starparse.domain.Entity;
import com.ixale.starparse.domain.EntityGuid;
import com.ixale.starparse.domain.Event;
import com.ixale.starparse.domain.Raid;
import com.ixale.starparse.domain.RaidBoss;
import com.ixale.starparse.domain.RaidBossName;
import com.ixale.starparse.domain.Actor.Type;
import com.ixale.starparse.domain.ops.*;

public class Helpers {

	public static boolean isSourceThisPlayer(final Event e) {
		return e.getSource() != null && e.getSource().getType().equals(Actor.Type.SELF);
	}

	public static boolean isSourceOtherPlayer(final Event e) {
		return e.getSource() != null && e.getSource().getType().equals(Actor.Type.PLAYER);
	}

	public static boolean isTargetEqual(final Event e, final Long guid) {
		return e.getTarget() != null && e.getTarget().getGuid() != null
			&& e.getTarget().getGuid().equals(guid);
	}

	public static boolean isTargetEqual(final Event e, final Long guid, final String name) {
		if (e.getTarget() == null) {
			return false;
		}
		if (guid != null) {
			return guid.equals(e.getTarget().getGuid());
		}
		if (name != null) {
			return name.equals(e.getTarget().getName());
		}
		return false;
	}

	public static boolean isTargetThisPlayer(final Event e) {
		return e.getTarget() != null && e.getTarget().getType().equals(Actor.Type.SELF);
	}

	public static boolean isTargetOtherPlayer(final Event e) {
		return e.getTarget() != null && e.getTarget().getType().equals(Actor.Type.PLAYER);
	}

	public static boolean isSourceEqual(final Event e, final Long guid) {
		return e.getSource() != null && e.getSource().getGuid() != null
			&& e.getSource().getGuid().equals(guid);
	}

	public static boolean isSourceEqual(final Event e, final Long guid, final String name) {
		if (e.getSource() == null) {
			return false;
		}
		if (guid != null) {
			return guid.equals(e.getSource().getGuid());
		}
		if (name != null) {
			return name.equals(e.getSource().getName());
		}
		return false;
	}

	public static boolean isTargetOrSourceWithin(final Event e, final Long... guids) {
		return isSourceWithin(e, guids) || isTargetWithin(e, guids);
	}

	public static boolean isSourceWithin(final Event e, final Long... guids) {
		if (e.getSource() != null && e.getSource().getGuid() != null) {
			for (final long guid: guids) {
				if (e.getSource().getGuid().equals(guid)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isTargetWithin(final Event e, final Long... guids) {
		if (e.getTarget() != null && e.getTarget().getGuid() != null) {
			for (final long guid: guids) {
				if (e.getTarget().getGuid().equals(guid)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isActionApply(final Event e) {
		return e.getAction().getGuid().equals(ApplyEffect.getGuid());
	}

	public static boolean isActionRemove(final Event e) {
		return e.getAction().getGuid().equals(RemoveEffect.getGuid());
	}

	public static boolean isEffectAbilityActivate(final Event e) {
		return e.getEffect().getGuid().equals(AbilityActivate.getGuid());
	}

	public static boolean isEffectEnterCombat(final Event e) {
		return e.getEffect().getGuid().equals(EnterCombat.getGuid());
	}

	public static boolean isEffectExitCombat(final Event e) {
		return e.getEffect().getGuid().equals(ExitCombat.getGuid());
	}

	public static boolean isEffectDeath(final Event e) {
		if (e.getEffect().getGuid().equals(Death.getGuid())) {
			return true;
		}
		if (e.getEffect().getGuid().equals(Revived.getGuid()) && e.getSource() != null && Type.NPC.equals(e.getSource().getType())) {
			// TODO: remove once 5.4 bug is fixed
			return true;
		}
		return false;
	}

	public static boolean isEffectRevive(final Event e) {
		if (!e.getEffect().getGuid().equals(Revived.getGuid())) {
			return false;
		}
		if (e.getSource() != null && Type.NPC.equals(e.getSource().getType())) {
			// TODO: remove once 5.4 bug is fixed
			return false;
		}
		return true;
	}

	public static boolean isEffectLoginImmunity(final Event e) {
		return e.getEffect().getGuid().equals(SafeLoginImmunity.getGuid());
	}

	public static boolean isEffectHeal(final Event e) {
		return e.getEffect().getGuid().equals(Heal.getGuid());
	}

	public static boolean isEffectDamage(final Event e) {
		return e.getEffect().getGuid().equals(Damage.getGuid());
	}

	public static boolean isEffectGuard(final Event e) {
		return containsGuid(GUARD, e.getEffect().getGuid());
	}

	public static boolean isEffectCombatDrop(final Event e) {
		return containsGuid(DROP, e.getEffect().getGuid());
	}

	public static boolean isEffectAbsorption(final Event e) {
		return containsGuid(ABSORPTION, e.getEffect().getGuid());
	}

	public static boolean isEffectAbsorptionBroken(Long guid) {
		return containsGuid(ABSORPTION_BROKEN, guid);
	}

	public static boolean isEffectDualWield(final Event e) {
		return isEffectEqual(e, EffectCentering.getGuid())
			|| isEffectEqual(e, EffectFury.getGuid());
	}

	public static boolean isEffectPvP(final Event e) {
		return e.getEffect().getGuid().equals(Trauma.getGuid()); // no lingering - not interesting
	}

	public static boolean isEffectGeneric(final Event e) {
		return containsGuid(GENERIC, e.getEffect().getGuid());
	}

	public static boolean isAbilityGeneric(final Event e) {
		return containsGuid(GENERIC, e.getAbility().getGuid());
	}

	public static boolean isAbilityNonreducedThreat(final Event e) {
		return containsGuid(NONREDUCED_THREAT, e.getAbility().getGuid()) 
				|| (e.getAbility().getName() != null && (
					e.getAbility().getName().contains("Medpac")
					|| e.getAbility().getName().contains("Healing Resonance")
				));
	}

	public static boolean isAbilityNoThreat(final Event e) {
		if (e.getAbility() == null || e.getAbility().getGuid() == null) {
			return false;
		}
		return e.getAbility().getGuid().equals(FocusedDefense.getGuid())
				|| e.getAbility().getGuid().equals(EnragedDefense.getGuid())
				|| e.getAbility().getGuid().equals(813226287693824L) // Sever Force
				|| e.getAbility().getGuid().equals(808424514256896L) // Creeping Terror
				|| e.getAbility().getGuid().equals(2157001295527936L) // Force in Balance
				|| e.getAbility().getGuid().equals(808433104191488L) // Death Field
				|| e.getAbility().getGuid().equals(964675424485376L) // Force Breach
				|| e.getAbility().getGuid().equals(808235535695872L) // Discharge
				|| e.getAbility().getGuid().equals(3414395921104896L) // Serenity Strike
				|| e.getAbility().getGuid().equals(3410053709168640L); // Leeching Strike
		// 3400617666019328 Demolish
		// 3400587601248256 Force Leach
	}

	public static boolean isAbilityFakeHeal(final Event e) {
		return isAbilityEqual(e, ForceEmpowerment.getGuid())
				|| isAbilityEqual(e, UnlimitedPower.getGuid())
				|| isAbilityEqual(e, 773652459028480L); // PVP rebirth
	}

	public static boolean isAbilityEqual(final Event e, final Long guid) {
		return e.getAbility() != null && e.getAbility().getGuid() != null
			&& e.getAbility().getGuid().equals(guid);
	}

	public static boolean isAbilityEqual(final Event e, final Long guid, final String name) {
		if (e.getAbility() == null) {
			return false;
		}
		if (guid != null) {
			return guid.equals(e.getAbility().getGuid());
		}
		if (name != null) {
			return name.equals(e.getAbility().getName());
		}
		return false;
	}

	public static boolean isEffectEqual(final Event e, final Long guid) {
		return e.getEffect() != null && e.getEffect().getGuid() != null
			&& e.getEffect().getGuid().equals(guid);
	}

	public static boolean isEffectEqual(final Event e, final Long guid, final String name) {
		if (e.getEffect() == null) {
			return false;
		}
		if (guid != null) {
			return guid.equals(e.getEffect().getGuid());
		}
		if (name != null) {
			return name.equals(e.getEffect().getName());
		}
		return false;
	}

	public static EntityGuid getEntityGuid(final Entity entity) {
		if (entity == null || entity.getGuid() == null) {
			return null;
		}
		for (int i = 0; i < EntityGuid.values().length; i++) {
			if (EntityGuid.values()[i].getGuid() == entity.getGuid()) {
				return EntityGuid.values()[i];
			}
		}
		return null;
	}

	private static final Raid[] raids = new Raid[]{
			new EternityVault(),
			new KaraggasPalace(),
			new ExplosiveConflict(),
			new TerrorFromBeyond(),
			new ScumAndVillainy(),
			new DreadFortress(),
			new DreadPalace(),
			new Ravagers(),
			new TempleOfSacrifice(),
			new WorldBoss(),
			new TrainingDummy(),
			new EternalChampionship(),
			new Iokath(),
			new Dxun()
	};

	private static final Map<Long, RaidBoss> bossesByGuids = new HashMap<>();
	private static final Map<String, RaidBoss> bossesByVerbose = new HashMap<>();
	private static final Map<RaidBossName, Map<Raid.Size, Map<Raid.Mode, RaidBoss>>> bossesInstances = new HashMap<>();
	static {
		// build journal
		for (final Raid r: raids) {
			for (final RaidBoss b: r.getBosses()) {
				if (b.getConfidentNpcGuids() != null) {
					for (long guid: b.getConfidentNpcGuids()) {
						if (bossesByGuids.containsKey(guid)) {
							throw new RuntimeException("Duplicate boss (confident): " + guid);
						}
						bossesByGuids.put(guid, b);
					}
				}
				if (b.getTentativeNpcGuids() != null) {
					for (long guid: b.getTentativeNpcGuids()) {
						if (bossesByGuids.containsKey(guid)) {
							throw new RuntimeException("Duplicate boss (tentative): " + guid);
						}
						bossesByGuids.put(guid, b);
					}
				}
				if (bossesByVerbose.containsKey(b.toString())) {
					throw new RuntimeException("Duplicate boss (verbose): " + b);
				}
				bossesByVerbose.put(b.toString(), b);

				if (!bossesInstances.containsKey(b.getRaidBossName())) {
					bossesInstances.put(b.getRaidBossName(), new HashMap<>());
				}
				if (!bossesInstances.get(b.getRaidBossName()).containsKey(b.getSize())) {
					bossesInstances.get(b.getRaidBossName()).put(b.getSize(), new HashMap<>());
				}
				bossesInstances.get(b.getRaidBossName()).get(b.getSize()).put(b.getMode(), b);
			}
		}
	}

	public static RaidBoss getRaidBoss(long entityGuid, final Raid.Size size, final Raid.Mode mode) {
		final RaidBoss boss = bossesByGuids.get(entityGuid);
		if (boss != null && size != null && mode != null
			&& bossesInstances.containsKey(boss.getRaidBossName())
			&& bossesInstances.get(boss.getRaidBossName()).containsKey(size)
			&& bossesInstances.get(boss.getRaidBossName()).get(size).containsKey(mode)) {
			// set by 5.4 combat enter event
			return bossesInstances.get(boss.getRaidBossName()).get(size).get(mode);
		}
		return boss;
	}

	private static final List<HashMap<Long, CharacterDiscipline>> disciplines = Arrays.asList(
		new HashMap<Long, CharacterDiscipline>(), // single wield
		new HashMap<Long, CharacterDiscipline>() // dual wield
	);
	static {
		// build journal
		for (final CharacterDiscipline discipline: CharacterDiscipline.values()) {
			int i = discipline.isDualWield() ? 1 : 0;
			for (long guid: discipline.getAbilities()) {
				if (disciplines.get(i).containsKey(guid)) {
					throw new RuntimeException("Duplicate discipline: " + guid);
				}
				disciplines.get(i).put(guid, discipline);
			}
		}
	}

	public static CharacterDiscipline getDiscipline(long abilityGuid, boolean isDualWield) {
		return disciplines.get(isDualWield ? 1 : 0).get(abilityGuid);
	}

	public static RaidBoss getRaidBossByVerbose(String bossNameVerbose) {
		return bossesByVerbose.get(bossNameVerbose);
	}

	public static SortedMap<String, List<String>> getRaidBosses() {
		SortedMap<String, List<String>> map = new TreeMap<>(String::compareTo);
		for (Raid raid : raids) {
			map.put(raid.getClass().getSimpleName(), raid.getBosses().stream().map(RaidBoss::getName).distinct().sorted().collect(Collectors.toList()));
		}
		return map;
	}

	public static String findRaidName(String bossName) {
		for (Raid raid : raids) {
			if (raid.getBosses().stream().map(RaidBoss::getName).anyMatch(bossName::equals)) {
				return raid.getClass().getSimpleName();
			}
		}
		return null;
	}
}
