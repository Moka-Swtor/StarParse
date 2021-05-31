package com.ixale.starparse.parser;

import static com.ixale.starparse.parser.Helpers.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixale.starparse.domain.Absorption;
import com.ixale.starparse.domain.Actor;
import com.ixale.starparse.domain.AttackType;
import com.ixale.starparse.domain.CharacterDiscipline;
import com.ixale.starparse.domain.CharacterRole;
import com.ixale.starparse.domain.Combat;
import com.ixale.starparse.domain.CombatLog;
import com.ixale.starparse.domain.Effect;
import com.ixale.starparse.domain.EffectKey;
import com.ixale.starparse.domain.Entity;
import com.ixale.starparse.domain.EntityGuid;
import com.ixale.starparse.domain.Event;
import com.ixale.starparse.domain.Phase;
import com.ixale.starparse.domain.Raid;
import com.ixale.starparse.domain.Raid.Mode;
import com.ixale.starparse.domain.Raid.Size;
import com.ixale.starparse.domain.RaidBoss;
import com.ixale.starparse.domain.RaidBoss.BossUpgrade;
import com.ixale.starparse.service.impl.Context;
import com.ixale.starparse.time.TimeUtils;
import com.ixale.starparse.timer.BaseTimer;
import com.ixale.starparse.timer.TimerManager;

public class Parser {

	private static final Logger logger = LoggerFactory.getLogger(Parser.class);

	// combat_2014-01-26_21_17_31_474300
	private final static Pattern filePattern = Pattern
			.compile("^combat_(?<Year>\\d{4})\\-(?<Month>\\d{2})\\-(?<Day>\\d{2})_(?<HH>\\d{2})_\\d{2}_\\d{2}_\\d{6}\\.txt$");
	private Matcher fileMatcher;

	// combat log line pattern
	private final static Pattern basePattern = Pattern.compile(
			"^\\[(?<TimeStamp>"
			+ "(?<HH>\\d{2}):(?<MM>\\d{2}):(?<SS>\\d{2})\\.(?<MS>\\d{3})"
			+ ")\\]"
			+ " \\[(?<Source>"
			+ "|@(?<SourcePlayerName>[^\\]:]*)(:(?<SourceCompanionName>[^{]*) \\{(?<SourceCompanionGuid>\\d*)\\})?"
			+ "|(?<SourceNpcName>[^{]*) \\{(?<SourceNpcGuid>\\d*)\\}(|:(?<SourceNpcInstance>\\d*))"
			+ ")\\]"
			+ " \\[(?<Target>"
			+ "|@(?<TargetPlayerName>[^\\]:]*)(|:(?<TargetCompanionName>[^{]*) \\{(?<TargetCompanionGuid>\\d*)\\})"
			+ "|(?<TargetNpcName>[^{]*) \\{(?<TargetNpcGuid>\\d*)\\}(|:(?<TargetNpcInstance>\\d*))"
			+ ")\\]"
			+ " \\[(?<Ability>"
			+ "|(?<AbilityName>[^{]*) \\{(?<AbilityGuid>\\d*)\\}"
			+ ")\\]"
			+ " \\[("
			+ "(?<ActionName>[^{]*) \\{(?<ActionGuid>\\d*)\\}: (?<EffectName>[^{]*) \\{(?<EffectGuid>\\d*)\\}"
			+ ")\\]"
			+ " \\("
			+ "(?<Value>\\-?\\d+)?(?<IsCrit>\\*)? ?"
			+ "( (?<DamageType>[^ \\-]+) \\{(?<DamageTypeGuid>\\d+)\\})?"
			+ "(\\((?<ReflectType>[^ ]+) \\{(?<ReflectTypeGuid>\\d+)\\}\\))?"
			+ "(?<IsMitigation> -((?<MitigationType>[^ ]+) \\{(?<MitigationTypeGuid>\\d+)\\})?)?"
			+ "( \\((?<AbsorbValue>\\d+) (?<AbsorbType>[^ ]+) \\{(?<AbsorbTypeGuid>\\d+)\\}\\))?"
			+ "\\)"
			+ "($| <(?<Threat>[^>]*?)>)");

	// combat log line pattern for enter/exit combat since 5.4
	private final static Pattern combat54Pattern = Pattern.compile(
			"^\\[(?<TimeStamp>"
			+ "(?<HH>\\d{2}):(?<MM>\\d{2}):(?<SS>\\d{2})\\.(?<MS>\\d{3})"
			+ ")\\]"
			+ " \\[(?<Source>"
			+ "|@(?<SourcePlayerName>[^\\]:]*)(:(?<SourceCompanionName>[^{]*) \\{(?<SourceCompanionGuid>\\d*)\\})?"
			+ "|(?<SourceNpcName>[^{]*) \\{(?<SourceNpcGuid>\\d*)\\}(|:(?<SourceNpcInstance>\\d*))"
			+ ")\\]"
			+ " \\[(?<Target>"
			+ "|@(?<TargetPlayerName>[^\\]:]*)(|:(?<TargetCompanionName>[^{]*) \\{(?<TargetCompanionGuid>\\d*)\\})"
			+ "|(?<TargetNpcName>[^{]*) \\{(?<TargetNpcGuid>\\d*)\\}(|:(?<TargetNpcInstance>\\d*))"
			+ ")\\]"
			+ " \\[(?<Ability>"
			+ "|(?<AbilityName>[^{]*) \\{(?<AbilityGuid>\\d*)\\}"
			+ ")\\]"
			+ " \\[("
			+ "(?<ActionName>[^{]*) \\{(?<ActionGuid>\\d*)\\}: (?<EffectName>[^{]*) \\{(?<EffectGuid>(836045448945489|836045448945490))\\}"
			+ ")\\]"
			+ " \\((?<Value>.*)\\)"
			+ "($| <(?<Threat>[^>]*?)>)");

	// combat log line pattern for GSF only (fallback)
	private final static Pattern gsfPattern = Pattern.compile(
			"^\\[(?<TimeStamp>"
			+ "(?<HH>\\d{2}):(?<MM>\\d{2}):(?<SS>\\d{2})\\.(?<MS>\\d{3})"
			+ ")\\]"
			+ " \\[(?<SourceGsfName>|\\d{10,})\\]"
			+ " \\[(?<TargetGsfName>|\\d{10,})\\] .*");

	// healing threat ratios
	private static final double THREAT_HEAL = .5,
			THREAT_HEAL_REDUCTION = .1, // 10% reduction on healers
			THREAT_TANK = 2, // 200% for tanks (even for their self heals / medpacs)
			THREAT_GUARD = .75;

	private static final long COMBAT_DELAY_WINDOW = 4 * 1000, // window to 1) include any lagged damage or healing 2) detect combat drop abilities 3)
			// reconnect "shattered" combats
			COMBAT_REVIVE_WINDOW = 60 * 1000, // window to use revive after death
			COMBAT_RETURN_WINDOW = 30 * 1000, // window to re-enter the combat after revival or combat drop

			EFFECT_OVERLAP_TOLERANCE = 500, // start A ... (start B ... end A ~ within 0.5s) ... end B

			ABSORPTION_OUTSIDE_DELAY_WINDOW = 4 * 1000, // window to include lagged absorption after the effect has ended
			ABSORPTION_INSIDE_DELAY_WINDOW = 500, // effect A ... effect B ... (end A ... absorption A ~ within 0.5s) ... end B ...

			PHASE_DAMAGE_WINDOW = 7 * 1000, // if no damage even occurs within 5s, create close the "damage phase"
			PHASE_DAMAGE_MININUM = 5 * 1000,
			HEALING_THREAT_TOLERANCE = 5;

	private Calendar c;
	private int lastHour;

	// parsed
	private final ArrayList<Event> events = new ArrayList<Event>();
	private final ArrayList<Combat> combats = new ArrayList<Combat>();
	private final ArrayList<Effect> effects = new ArrayList<Effect>();
	private final ArrayList<Absorption> absorptions = new ArrayList<Absorption>();
	private final ArrayList<Phase> phases = new ArrayList<Phase>();

	private Context context;

	// combat log
	private int combatLogId;
	private CombatLog combatLog;

	// events
	private int eventId;

	// effects
	private int effectId;
	private final ArrayList<Effect> currentEffects = new ArrayList<Effect>();

	private EffectKey effectKey;
	private List<Effect> effectInstances;
	private final HashMap<EffectKey, List<Effect>> runningEffects = new HashMap<EffectKey, List<Effect>>();
	private final HashMap<EffectKey, Integer> stackedEffects = new HashMap<EffectKey, Integer>();
	private final ArrayList<Long> activatedAbilities = new ArrayList<Long>();

	// absorptions
	private final List<Effect> absorptionEffectsRunning = new ArrayList<>();
	private final List<Effect> absorptionEffectsClosing = new ArrayList<>();
	private final List<Effect> absorptionEffectsConsumed = new ArrayList<>();
	private final List<Integer> absorptionEventsInside = new ArrayList<>();
	private final List<Integer> absorptionEventsOutside = new ArrayList<>();

	// phases
	private int phaseId;
	private Phase currentBossPhase;
	private String newBossPhaseName;
	private Event firstDamageEvent, lastDamageEvent, lastCombatDropEvent;

	// combat
	private int combatId;
	private Combat combat;
	private Long combatConnectLimit;
	private BossUpgrade combatBossUpgrade;
	private Raid.Mode instanceMode;
	private Raid.Size instanceSize;
	private boolean isUsingMimCrystal = false;

	// effective threat for the current fight
	private long combatTotalThreat;
	// to support mara/jugg and sent/guard distinction (used for player only)
	private boolean isDualWield = false;

	public class ActorState implements TimerState {

		// effective guard state (0, 1, 2)
		public int guarded = 0;
		// detected role
		public CharacterRole role = null; // assume healer by default (worst case scenario = more EHPS for off-healing DPS)
		// healing stacks
		public int hotStacks = 0;
		public Long hotSince = null;
		public Entity hotEffect = null;
		public Integer hotDuration = null;
		public Long hotLast = null;

		@Override
		public int getStacks() {
			return hotStacks;
		}

		@Override
		public Long getSince() {
			return hotSince;
		}

		@Override
		public Long getLast() {
			return hotLast;
		}

		@Override
		public Entity getEffect() {
			return hotEffect;
		}

		@Override
		public Integer getDuration() {
			return hotDuration;
		}
	}

	private final HashMap<Actor, ActorState> actorStates = new HashMap<Actor, ActorState>();
	private Entity pendingHealAbility = null;
	private int hotCount = 0, hotTotal = 0, hotAverage = 0;

	public Parser(final Context context) {
		this.context = context;
	}

	public void reset() {
		combatLogId = 0;
		combatLog = null;
		c = Calendar.getInstance(TimeUtils.getCurrentTimezone());
		lastHour = 0;

		eventId = 0;
		events.clear();

		combatId = 0;
		combat = null;
		combatConnectLimit = null;
		combatBossUpgrade = null;
		instanceMode = null;
		instanceSize = null;
		combats.clear();

		effectId = 0;
		effectKey = null;
		effectInstances = null;
		runningEffects.clear();
		stackedEffects.clear();
		currentEffects.clear();
		effects.clear();
		activatedAbilities.clear();

		absorptionEffectsRunning.clear();
		absorptionEffectsClosing.clear();
		absorptionEffectsConsumed.clear();
		absorptionEventsInside.clear();
		absorptionEventsOutside.clear();
		absorptions.clear();

		phaseId = 0;
		currentBossPhase = null;
		firstDamageEvent = lastDamageEvent = null;
		phases.clear();

		actorStates.clear();

		combatTotalThreat = 0;
		isDualWield = false;
		pendingHealAbility = null;
		hotCount = hotTotal = hotAverage = 0;

		if (logger.isDebugEnabled()) {
			logger.debug("Context cleared");
		}
	}

	public CombatLog getCombatLog() {
		return combatLog;
	}

	public ArrayList<Event> getEvents() {
		return events;
	}

	public ArrayList<Combat> getCombats() {
		return combats;
	}

	public Combat getCurrentCombat() {
		return combat;
	}

	public ArrayList<Effect> getEffects() {
		return effects;
	}

	public ArrayList<Effect> getCurrentEffects() {
		return currentEffects;
	}

	public ArrayList<Absorption> getAbsorptions() {
		return absorptions;
	}

	public Phase getCurrentPhase() {
		return currentBossPhase;
	}

	public ArrayList<Phase> getPhases() {
		return phases;
	}

	public Map<Actor, ActorState> getActorStates() {
		return actorStates;
	}

	public void setCombatLogFile(File logFile) throws Exception {
		// new file started
		if (events.size() > 0 || combats.size() > 0 || combat != null) {
			if (logger.isDebugEnabled()) {
				logger.debug(
						"Setting new file without finishing the last, discarding: " + events.size() + " events and " + combats.size() + " combats");
			}
		}

		// reset everything
		reset();

		// setup date
		fileMatcher = filePattern.matcher(logFile.getName());

		if (fileMatcher.matches()) {
			c.set(Calendar.YEAR, Integer.parseInt(fileMatcher.group("Year")));
			c.set(Calendar.MONTH, Integer.parseInt(fileMatcher.group("Month")) - 1);
			c.set(Calendar.DATE, Integer.parseInt(fileMatcher.group("Day")));
			lastHour = Integer.parseInt(fileMatcher.group("HH"));
		} else {
			// probably custom name (e.g. "420 parse 360 scope.txt")
			c.setTimeInMillis(logFile.lastModified());
		}

		// FIXME: year 1472
		if (c.get(Calendar.YEAR) < 1900) {
			c.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
		}

		combatLog = new CombatLog(++combatLogId, logFile.getCanonicalPath(), c.getTimeInMillis());
	}

	public void closeCombatLogFile() {
		if (combat != null) {
			closeCurrentCombat();
		}
	}

	/**
	 *
	 * @param line
	 * @throws Exception
	 */
	public void parseLogLine(final String line) throws ParserException {

		if (line == null) {
			logger.warn("Empty line, ignoring");
			return;
		}

		if (combatLog == null) {
			throw new RuntimeException("Enclosing combat log not set");
		}

		// match the combat log line
		Matcher baseMatcher = basePattern.matcher(line);

		if (!baseMatcher.matches()) {
			// fallback to 5.4 combat enter/exit
			baseMatcher = combat54Pattern.matcher(line);
			if (!baseMatcher.matches()) {
				if (gsfPattern.matcher(line).matches()) {
					// GSF combat line, ignore for now
					return;
				}
				throw new ParserException("Invalid line");
			}
		}

		// setup event
		final Event e = new Event(++eventId, combatLogId, getTimestamp(baseMatcher));

		if (e.getEventId() == 1) {
			// adjust combat log start from the very first event
			combatLog.setTimeFrom(e.getTimestamp());
		}

		// auto detect name if not already
		if (combatLog.getCharacterName() == null
				&& baseMatcher.group("SourcePlayerName") != null && !baseMatcher.group("SourcePlayerName").isEmpty()
				&& baseMatcher.group("SourcePlayerName").equals(baseMatcher.group("TargetPlayerName"))) {
			// found, set
			combatLog.setCharacterName(baseMatcher.group("SourcePlayerName"));

			if (logger.isDebugEnabled()) {
				logger.debug("Player name detected as [" + combatLog.getCharacterName() + "] at " + e.getTs());
			}

			// fix if already existed in the past (should be super-rare)
			if (context.getActors().containsKey(combatLog.getCharacterName())) {
				context.getActors().put(combatLog.getCharacterName(), new Actor(combatLog.getCharacterName(), Actor.Type.SELF));
				for (int i = 0; i < events.size(); i++) {
					if (events.get(i).getSource().getName().equals(combatLog.getCharacterName())) {
						events.get(i).setSource(context.getActors().get(combatLog.getCharacterName()));
					}
					if (events.get(i).getTarget().getName().equals(combatLog.getCharacterName())) {
						events.get(i).setTarget(context.getActors().get(combatLog.getCharacterName()));
					}
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Fixed player for previous events: " + events.size());
				}
			}
		}

		if (baseMatcher.group("Value") != null && "836045448945489".equals(baseMatcher.group("EffectGuid"))) {
			// enter, any ops details?
			Raid.Size s = null;
			Raid.Mode m = null;
			final String diff = baseMatcher.group("Value");
			if (diff.contains("16 ") || diff.contains("16 ")) {
				s = Size.Sixteen;
			} else if (diff.contains("8 ") || diff.contains("8�")) {
				s = Size.Eight;
			}
			if (diff.contains("Story") || diff.contains("histoire")) {
				m = Mode.SM;
			} else if (diff.contains("Veteran") || diff.contains("v�t�ran")) {
				m = Mode.HM;
			} else if (diff.contains("Master") || diff.contains("ma�tre")) {
				m = Mode.NiM;
			}
			if (s != null && m != null) {
				instanceMode = m;
				instanceSize = s;
				if (logger.isDebugEnabled()) {
					logger.debug("Instance set as " + s + " " + m + " at " + e.getTs());
				}
			}

		} else if ("836045448945490".equals(baseMatcher.group("EffectGuid"))) {
			// TBD
		}

		// source
		if (baseMatcher.group("Source") != null && !baseMatcher.group("Source").isEmpty()) {
			e.setSource(getActor(
					baseMatcher.group("SourcePlayerName"),
					baseMatcher.group("SourceCompanionName"),
					baseMatcher.group("SourceCompanionGuid"),
					baseMatcher.group("SourceNpcName"),
					baseMatcher.group("SourceNpcGuid"),
					baseMatcher.group("SourceNpcInstance")));
		} else {
			e.setSource(context.getActor("Unknown", Actor.Type.NPC));
		}

		// target
		if (baseMatcher.group("Target") != null && !baseMatcher.group("Target").isEmpty()) {
			e.setTarget(getActor(
					baseMatcher.group("TargetPlayerName"),
					baseMatcher.group("TargetCompanionName"),
					baseMatcher.group("TargetCompanionGuid"),
					baseMatcher.group("TargetNpcName"),
					baseMatcher.group("TargetNpcGuid"),
					baseMatcher.group("TargetNpcInstance")));
		} else {
			e.setTarget(context.getActor("Unknown", Actor.Type.NPC));
		}

		// ability
		if (baseMatcher.group("Ability") != null && !baseMatcher.group("Ability").isEmpty()) {
			e.setAbility(getEntity(
					baseMatcher.group("AbilityName"),
					baseMatcher.group("AbilityGuid")));
		}

		// action
		e.setAction(getEntity(
				baseMatcher.group("ActionName"),
				baseMatcher.group("ActionGuid")));

		// effect
		e.setEffect(getEntity(
				baseMatcher.group("EffectName"),
				baseMatcher.group("EffectGuid")));

		// value (healing / damage)
		if (baseMatcher.group("Value") != null
				&& !"836045448945489".equals(baseMatcher.group("EffectGuid"))
				&& !"836045448945490".equals(baseMatcher.group("EffectGuid"))) {
			e.setValue(Integer.parseInt(baseMatcher.group("Value")));
			// critical hit?
			e.setCrit(baseMatcher.group("IsCrit") != null);

			// damage
			if (baseMatcher.group("DamageType") != null) {
				e.setDamage(getEntity(
						baseMatcher.group("DamageType"),
						baseMatcher.group("DamageTypeGuid")));
			}

			// reflect
			if (baseMatcher.group("ReflectType") != null) {
				e.setReflect(getEntity(
						baseMatcher.group("ReflectType"),
						baseMatcher.group("ReflectTypeGuid")));
			}

			// mitigation
			if (baseMatcher.group("IsMitigation") != null) {
				if (baseMatcher.group("MitigationType") != null) {
					e.setMitigation(getEntity(
							baseMatcher.group("MitigationType"),
							baseMatcher.group("MitigationTypeGuid")));

					// attack type (PvE bosses and PvP players only)
					if (combat != null
							&& (combat.getBoss() != null || isSourceOtherPlayer(e))
							&& isTargetThisPlayer(e)) {
						processAttackType(e);
					}

				} else {
					// unknown mitigation
					e.setMitigation(getEntity(
							"unknown",
							"-1"));
				}
			}

			// absorption
			if (baseMatcher.group("AbsorbValue") != null) {
				e.setAbsorption(getEntity(
						baseMatcher.group("AbsorbType"),
						baseMatcher.group("AbsorbTypeGuid")));
				e.setAbsorbed(Integer.parseInt(baseMatcher.group("AbsorbValue")));
				if (e.getValue() != null && e.getAbsorbed() != null && e.getAbsorbed() > e.getValue()) {
					e.setAbsorbed(e.getValue());
				}
			}
		}

		// threat
		if (baseMatcher.group("Threat") != null) {
			e.setThreat(Long.parseLong(baseMatcher.group("Threat")));
		}

		// calculated context values
		// guard
		if (isTargetThisPlayer(e) || isSourceThisPlayer(e)) {
			processEventGuard(e);
		}

		// combat
		processEventCombat(e);

		// healing
		if (isEffectHeal(e)) {
			processEventHealing(e);
		}

		// effect
		if (isTargetThisPlayer(e) || isSourceThisPlayer(e)) {
			processEventEffect(e);
		}

		// absorption
		processEventAbsorption(e);

		// instance swap?
		if (isEffectEqual(e, EntityGuid.SafeLoginImmunity.getGuid()) && isActionApply(e)) {
			instanceMode = null;
			instanceSize = null;
			isUsingMimCrystal = false;
			if (logger.isDebugEnabled()) {
				logger.debug("Instance reset at " + e.getTs());
			}

			clearHotsTracking();
//		} else if (isEffectEqual(e, EntityGuid.Bolster.getGuid())) {
//			instanceMode = Raid.Mode.SM;

		} else if (isEffectEqual(e, 3638571739119616L) && isActionApply(e)) { // Nightmare Fury
			instanceMode = Raid.Mode.NiM;
			if (logger.isDebugEnabled()) {
				logger.debug("NiM crystal detected at " + e.getTs());
			}
			if (!isUsingMimCrystal && combat != null) {
				// activated mid fight?
				context.addCombatEvent(combat.getCombatId(), Event.Type.NIM_CRYSTAL, e.getTimestamp());
			}
			isUsingMimCrystal = true;
		}

		// hots tracking
		if (isSourceThisPlayer(e)
				&& e.getTarget() != null
				&& (combat == null || combat.getDiscipline() == null || CharacterRole.HEALER.equals(combat.getDiscipline().getRole()))) {
			processEventHots(e);
		}

		events.add(e);
	}

	/**
	 *
	 * @param m
	 * @return
	 */
	private Long getTimestamp(Matcher m) {

		c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group("HH")));
		c.set(Calendar.MINUTE, Integer.parseInt(m.group("MM")));
		c.set(Calendar.SECOND, Integer.parseInt(m.group("SS")));
		c.set(Calendar.MILLISECOND, Integer.parseInt(m.group("MS")));

		if (lastHour - c.get(Calendar.HOUR_OF_DAY) > 2) {
			// over midnight (2 = ignore daylight saving)
			c.add(Calendar.DAY_OF_MONTH, 1);
		}
		lastHour = c.get(Calendar.HOUR_OF_DAY);

		return c.getTimeInMillis();
	}

	/**
	 *
	 * @param playerName
	 * @param companionName
	 * @param companionGuid
	 * @param npcName
	 * @param npcGuid
	 * @param npcInstanceId
	 * @return
	 */
	private Actor getActor(String playerName,
			String companionName, String companionGuid,
			String npcName, String npcGuid, String npcInstanceId) {

		if (companionName != null) {
			return context.getActor(companionName,
					Actor.Type.COMPANION,
					Long.parseLong(companionGuid));
		}

		if (playerName != null) {
			return context.getActor(
					playerName,
					playerName.equals(combatLog.getCharacterName()) ? Actor.Type.SELF : Actor.Type.PLAYER);
		}

		if (npcGuid != null) {
			// detect raid encounter
			final long guid = Long.parseLong(npcGuid);
			if (combat != null && combat.getBoss() == null) {
				combat.setBoss(getRaidBoss(guid, instanceSize, instanceMode));
				if (combat.getBoss() != null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Boss detected as [" + combat.getBoss() + "] at " + eventId);
						if (Boolean.TRUE.equals(combat.isPvp())) {
							logger.debug("Removed previously set PvP flag at " + eventId);
						}
					}
					combat.setIsPvp(false); // explicitly set to false (as it may be both, e.g. open world PvE grieving)

					// upgrades available?
					combatBossUpgrade = combat.getBoss().getPossibleUpgrade();
					if (combatBossUpgrade != null && (instanceMode != null || instanceSize != null)) {
						// try to use the already-confident mode and size for this instance
						resolveCombatUpgrade(combatBossUpgrade.upgradeByModeAndSize(instanceMode, instanceSize), true);
					}
					if (combatBossUpgrade == null) {
						instanceMode = combat.getBoss().getMode();
						instanceSize = combat.getBoss().getSize();
					}
				}
			}
			if (combat != null && combatBossUpgrade != null) {
				resolveCombatUpgrade(combatBossUpgrade.upgradeByNpc(guid), true);
			}
			return context.getActor(
					npcName,
					Actor.Type.NPC,
					guid,
					npcInstanceId != null ? Long.parseLong(npcInstanceId) : 0L);
		}

		throw new IllegalArgumentException("Invalid actor");
	}

	/**
	 *
	 * @param name
	 * @param guid
	 * @return
	 */
	private Entity getEntity(String name, String guid) {
		return context.getEntity(name, Long.parseLong(guid));
	}

	public void processEventGuard(final Event e) throws ParserException {

		if (isEffectGuard(e) && (isTargetOtherPlayer(e) || isSourceOtherPlayer(e))) {
			if (isActionApply(e)) {
				// guard gained
				if (!actorStates.containsKey(e.getTarget())) {
					actorStates.put(e.getTarget(), new ActorState());
					if (combat != null && combat.getDiscipline() != null && isTargetThisPlayer(e)) {
						// already known
						actorStates.get(e.getTarget()).role = combat.getDiscipline().getRole();
					}
				}

				actorStates.get(e.getTarget()).guarded = (actorStates.get(e.getTarget()).guarded < 2
						? (actorStates.get(e.getTarget()).guarded + 1)
						: 2);

			} else if (isActionRemove(e)) {
				// guard lost
				if (actorStates.containsKey(e.getTarget())) {
					actorStates.get(e.getTarget()).guarded = (actorStates.get(e.getTarget()).guarded > 0
							? (actorStates.get(e.getTarget()).guarded - 1)
							: 0);
				}

			} else {
				throw new ParserException("Unknown guard action: " + e);
			}
		}

		// store context for current player always
		if (isSourceThisPlayer(e)) {
			e.setGuardState(actorStates.containsKey(e.getSource()) ? actorStates.get(e.getSource()).guarded : 0);
		} else/* target */ {
			e.setGuardState(actorStates.containsKey(e.getTarget()) ? actorStates.get(e.getTarget()).guarded : 0);
		}
	}

	/**
	 *
	 * @param e
	 * @throws Exception
	 */
	public void processEventCombat(final Event e) {

		// resolve combat
		if (combatConnectLimit != null && combatConnectLimit < e.getTimestamp()) {
			// window expired, close the running combat for good
			closeCurrentCombat();
		}

		if (isEffectEnterCombat(e)) {
			if (combatConnectLimit != null) {
				// within limit, reopen last combat
				combat.setEventIdTo(null);
				combat.setTimeTo(null);
				combatConnectLimit = null;

			} else if (combat != null) {
				// sometimes new combat is created even without exiting the previous one
				if (logger.isDebugEnabled()) {
					logger.debug("New combat was entered without exiting the previous one, silently connecting");
				}

			} else {
				// setup new combat
				combat = new Combat(++combatId, combatLogId, e.getTimestamp(), e.getEventId());
				for (final Actor a : actorStates.keySet()) {
					actorStates.get(a).role = null; // might have re-specced
				}
				if (isUsingMimCrystal) {
					context.addCombatEvent(combat.getCombatId(), Event.Type.NIM_CRYSTAL, e.getTimestamp());
				}
			}

		} else if (combat != null && combatConnectLimit == null
				&& (isEffectExitCombat(e) || (isTargetThisPlayer(e) && isEffectDeath(e)) || isEffectLoginImmunity(e))) {
			// exit event detected (and no window is set yet), setup candidates
			combat.setEventIdTo(e.getEventId());
			combat.setTimeTo(e.getTimestamp());

			if (isEffectExitCombat(e)) {
				context.addCombatEvent(combat.getCombatId(), Event.Type.COMBAT_EXIT, e.getTimestamp());
			} else if (isEffectDeath(e)) {
				context.addCombatEvent(combat.getCombatId(), Event.Type.DEATH, e.getTimestamp());
			}

			// ... and setup limit for either revive or quick reconnect (and for lagged damage and healing)
			combatConnectLimit = e.getTimestamp() + (isEffectDeath(e)
					// died - setup REVIVE limit
					? COMBAT_REVIVE_WINDOW
					: ((lastCombatDropEvent != null && (lastCombatDropEvent.getTimestamp() > (e.getTimestamp() - COMBAT_RETURN_WINDOW))
					// recent (yet already consumed) combat drop event - "drop->enter->exit[now]->?enter" sequence can happen - setup RETURN window
					? COMBAT_RETURN_WINDOW
					// just regular window for delayed damage/healing events
					: COMBAT_DELAY_WINDOW)));

		} else if (combat != null && combatConnectLimit != null) {
			if (isTargetThisPlayer(e) && isEffectRevive(e)) {
				// revived within REVIVE limit, setup RETURN limit and keep waiting
				combatConnectLimit = e.getTimestamp() + COMBAT_RETURN_WINDOW;

			} else if (isTargetThisPlayer(e) && isEffectCombatDrop(e)) {
				// combat drop detected within DELAY window, setup RETURN limit and keep waiting
				combatConnectLimit = e.getTimestamp() + COMBAT_RETURN_WINDOW;
				// all combat drops are suspicious as you can drop, enter, kill/discard add, exit combat and then enter again (e.g. Raptus challenge)
				lastCombatDropEvent = e;

			} else if (isSourceThisPlayer(e) && e.getValue() != null && (e.getThreat() != null || isEffectDamage(e))) {
				// gracefully include any delayed damage/healing abilities
				// (after dying, DOTs can be still ticking, although causing no threat)
				combat.setEventIdTo(e.getEventId());
				// combat.setTimeTo(e.getTimestamp()); // do not extend, keep the original time
			}
		}

		// resolve effective threat
		if (combat != null && isSourceThisPlayer(e) && e.getThreat() != null) {
			if (combatTotalThreat + e.getThreat() < 0) {
				e.setEffectiveThreat(combatTotalThreat * -1);
				combatTotalThreat = 0;
			} else {
				e.setEffectiveThreat(e.getThreat());
				combatTotalThreat += e.getThreat();
			}
		}

		// resolve combat phase
		if (combat != null && combat.getBoss() != null) {
			if ((newBossPhaseName = combat.getBoss().getRaid().getNewPhaseName(e, combat,
					currentBossPhase != null ? currentBossPhase.getName() : null)) != null) {
				// new phase detected
				if (currentBossPhase != null) {
					// close old - bounds are explicitly as <from, to)
					closePhase(currentBossPhase, e.getEventId() - 1, e.getTimestamp() - combat.getTimeFrom() - 1);
				}
				// setup new (if this is the very first one, assume it started at the beginning of this combat)
				currentBossPhase = new Phase(++phaseId, newBossPhaseName, Phase.Type.BOSS,
						combat.getCombatId(),
						(currentBossPhase == null ? combat.getEventIdFrom() : e.getEventId()),
						(currentBossPhase == null ? 0 : e.getTimestamp() - combat.getTimeFrom()));
			}

			if (combatBossUpgrade != null && e.getAbility() != null && e.getAbility().getGuid() != null) {
				resolveCombatUpgrade(combatBossUpgrade.upgradeByAbility(e.getAbility().getGuid(), e.getEffect().getGuid(), e.getValue()), false);
			}
		}

		// resolve damage phase
		if (combat != null && isSourceThisPlayer(e) && isEffectDamage(e) && e.getValue() > 0) {
			if (firstDamageEvent == null) {
				// open new damage phase
				firstDamageEvent = e;

			} else if (lastDamageEvent != null && (lastDamageEvent.getTimestamp() + PHASE_DAMAGE_WINDOW < e.getTimestamp())) {
				// close damage phase and open new one
				createDamagePhase(firstDamageEvent, lastDamageEvent);
				lastDamageEvent = null;
				firstDamageEvent = e;

			} else {
				// prolong current damage phase
				lastDamageEvent = e;
			}
		}

		// resolve discipline
		if (combat != null && combat.getDiscipline() == null && isSourceThisPlayer(e)) {
			if (!isDualWield && isEffectDualWield(e)) {
				isDualWield = true;
			}
			if (isEffectAbilityActivate(e)) {
				combat.setDiscipline(getDiscipline(e.getAbility().getGuid(), isDualWield));
				if (combat.getDiscipline() != null) {
					// self
					getActorState(e.getSource()).role = combat.getDiscipline().getRole();
//					if (logger.isDebugEnabled()) {
//						logger.debug("Discipline detected as [" + combat.getDiscipline() + "] at " + e.getTs());
//					}
					if (!CharacterRole.HEALER.equals(combat.getDiscipline().getRole())) {
						// make sure its reset
						clearHotsTracking();
					}
				}
			}
		}
	}

	private void closeCurrentCombat() {
		combatConnectLimit = null;
		combatTotalThreat = 0;
		combats.add(combat);

		if (currentBossPhase != null && combat.getEventIdTo() != null) {
			// close running phase - bounds explicitly set as <from, to>
			closePhase(currentBossPhase, combat.getEventIdTo(), ( // extend after the combat boundary if there was a delayed damage event
					lastDamageEvent != null && lastDamageEvent.getTimestamp() > combat.getTimeTo()
					? lastDamageEvent.getTimestamp()
					: combat.getTimeTo())
					- combat.getTimeFrom());
			currentBossPhase = null;
		}
		if (lastDamageEvent != null) {
			createDamagePhase(firstDamageEvent, lastDamageEvent);
		}
		firstDamageEvent = lastDamageEvent = lastCombatDropEvent = null;

		combat = null;
		combatBossUpgrade = null;

		TimerManager.stopAllTimers(BaseTimer.Scope.COMBAT);
	}

	private void createDamagePhase(final Event eventFrom, final Event eventTo) {
		if ((eventTo.getTimestamp() - eventFrom.getTimestamp()) < PHASE_DAMAGE_MININUM) {
			// too short, ignore
			return;
		}
		closePhase(new Phase(++phaseId, "Damage", Phase.Type.DAMAGE,
				combat.getCombatId(),
				eventFrom.getEventId(),
				eventFrom.getTimestamp() - combat.getTimeFrom()),
				eventTo.getEventId(), eventTo.getTimestamp() - combat.getTimeFrom());
	}

	private void closePhase(final Phase phase, int eventIdTo, long tickTo) {
		phase.setEventIdTo(eventIdTo);
		phase.setTickTo(tickTo);
		phases.add(phase);
	}

	public void processEventHealing(final Event e) throws ParserException {

		if (e.getThreat() != null) {
			if (isAbilityFakeHeal(e)) {
				// ignore
				return;
			}
			// continue normally

		} else if (isAbilityNoThreat(e) && isSourceThisPlayer(e) && isTargetThisPlayer(e)) {
			// consider as fully effective
			e.setEffectiveHeal(e.getValue());
			return;

		} else {
			// no effective, nothing to do
			return;
		}

		// resolve effective healing
		if (!actorStates.containsKey(e.getSource())) {
			// setup healer
			actorStates.put(e.getSource(), new ActorState());
			if (combat != null && combat.getDiscipline() != null && isSourceThisPlayer(e)) {
				// already known
				actorStates.get(e.getSource()).role = combat.getDiscipline().getRole();
			}
		}

		// detect healer if possible
		if (actorStates.get(e.getSource()).role == null) {
			final CharacterDiscipline actorDiscipline = getDiscipline(e.getAbility().getGuid(), false);
			if (actorDiscipline != null) {
				actorStates.get(e.getSource()).role = actorDiscipline.getRole();
//				if (logger.isDebugEnabled()) {
//					logger.debug("Healing threat ratio set to " + actorDiscipline.getRole() + " for " + e.getSource() + " at " + e.getTs());
//				}
			}
		}

		// shortcuts
		final boolean isGuarded = actorStates.get(e.getSource()).guarded > 0;
		final boolean isHealer = actorStates.get(e.getSource()).role == null || CharacterRole.HEALER.equals(actorStates.get(e.getSource()).role);
		final boolean isTank = !isHealer && CharacterRole.TANK.equals(actorStates.get(e.getSource()).role);

		// calculate effective heal using the threat generated
		int effectiveHeal = getEffectiveHeal(e.getThreat(),
				isHealer && !isAbilityNonreducedThreat(e),
				isGuarded,
				isTank);

		// sanity check
		if (effectiveHeal == e.getValue() || Math.abs(effectiveHeal - e.getValue()) <= HEALING_THREAT_TOLERANCE) {
			// fully effective (possibly with rounding issue)
			e.setEffectiveHeal(e.getValue());
			return;
		}

		if (effectiveHeal < e.getValue()) {
			// not fully effective
			e.setEffectiveHeal(effectiveHeal);

			if (!isGuarded) {
				// try with guard on
				effectiveHeal = getEffectiveHeal(e.getThreat(),
						isHealer && !isAbilityNonreducedThreat(e),
						true,
						isTank);
				if (Math.abs(effectiveHeal - e.getValue()) <= HEALING_THREAT_TOLERANCE) {
					// target is guarded, fix the flag
//					if (logger.isDebugEnabled()) {
//						logger.debug("Healing threat is reduced by guard, fixed for " + e.getSource() + " at " + e.getTs());
//					}
					actorStates.get(e.getSource()).guarded = 1;

					e.setEffectiveHeal(e.getValue());
					return;
				}
			}

			// nothing to be done (e.g. unable to detect not fully effective heals from guarded healers)
			return;
		}

		// value is too high - the ratios are off, try to detect the error ...
		if (isHealer && !isAbilityNonreducedThreat(e)) {
			// try without the 10% healing reduction
			effectiveHeal = getEffectiveHeal(e.getThreat(),
					false,
					isGuarded,
					isTank);

			if (effectiveHeal < (e.getValue() + HEALING_THREAT_TOLERANCE)) {
				if (CharacterRole.HEALER.equals(actorStates.get(e.getSource()).role)) {
					// we know for sure this is a healer, therefore 10% reduced, so assume
					// it is actually the ability not being affected by it (true for many HOTs etc)

				} else {
					// healing threat reduction not active (probably a DPS class off-healing), reset
					actorStates.get(e.getSource()).role = CharacterRole.DPS;
//					if (logger.isDebugEnabled()) {
//						logger.debug("Healing threat reduction removed for " + e.getSource() + " at " + e.getTs());
//					}
				}

				e.setEffectiveHeal(Math.min(e.getValue(), effectiveHeal));
				return;
			}
		}

		if (isGuarded) {
			// try without the guard 25% reduction
			effectiveHeal = getEffectiveHeal(e.getThreat(),
					isHealer && !isAbilityNonreducedThreat(e),
					false,
					isTank);
			if (effectiveHeal <= (e.getValue() + HEALING_THREAT_TOLERANCE)) {
				// supposedly guarded, but the threat is not reduced (possibly SWTOR bug) - reset the flag
				actorStates.get(e.getSource()).guarded = 0;
//				if (logger.isDebugEnabled()) {
//					logger.debug("Healing threat is not reduced by guard, cancelled for " + e.getSource() + " at " + e.getTs());
//				}

				e.setEffectiveHeal(Math.min(e.getValue(), effectiveHeal));
				return;
			}
		}

		if (isGuarded && isHealer && !isAbilityNonreducedThreat(e)) {
			// both of the above combined (without 35% reduction)
			effectiveHeal = getEffectiveHeal(e.getThreat(),
					false,
					false,
					isTank);
			if (effectiveHeal <= (e.getValue() + HEALING_THREAT_TOLERANCE)) {
				actorStates.get(e.getSource()).guarded = 0;
				if (logger.isDebugEnabled()) {
					logger.debug("Healing threat is not reduced by guard, cancelled for " + e.getSource() + " at " + e.getTs());
				}

				if (CharacterRole.HEALER.equals(actorStates.get(e.getSource()).role)) {
					// we know for sure this is a healer, therefore 10% reduced, so assume
					// it is actually the ability not being affected by it (true for many HOTs etc)

				} else {
					actorStates.get(e.getSource()).role = CharacterRole.DPS;
//					if (logger.isDebugEnabled()) {
//						logger.debug("Healing threat reduction removed for " + e.getSource() + " at " + e.getTs());
//					}
				}

				e.setEffectiveHeal(Math.min(e.getValue(), effectiveHeal));
				return;
			}
		}

		if (!isTank) {
			// try with tanking 200% ratio (and without the reduction if any)
			effectiveHeal = getEffectiveHeal(e.getThreat(),
					false,
					isGuarded,
					true);

			if (effectiveHeal <= (e.getValue() + HEALING_THREAT_TOLERANCE)) {
				actorStates.get(e.getSource()).role = CharacterRole.TANK;
				if (logger.isDebugEnabled()) {
					logger.debug("Healing threat ratio set to tank for " + e.getSource() + " at " + e.getTs());
				}

				e.setEffectiveHeal(Math.min(e.getValue(), effectiveHeal));
				return;
			}
		}

		if (!isTank && isGuarded) {
			// try again, this time even without a guard
			effectiveHeal = getEffectiveHeal(e.getThreat(),
					false,
					false,
					true);

			if (effectiveHeal <= (e.getValue() + HEALING_THREAT_TOLERANCE)) {
				actorStates.get(e.getSource()).guarded = 0;
				if (logger.isDebugEnabled()) {
					logger.debug("Healing threat is not reduced by guard, cancelled for " + e.getSource() + " at " + e.getTs());
				}

				actorStates.get(e.getSource()).role = CharacterRole.TANK;
				if (logger.isDebugEnabled()) {
					logger.debug("Healing threat ratio set to tank for " + e.getSource() + " at " + e.getTs());
				}

				e.setEffectiveHeal(Math.min(e.getValue(), effectiveHeal));
				return;
			}
		}

		e.setEffectiveHeal(Math.min(e.getValue(), effectiveHeal));
		logger.warn("Unknown heal ratio for event: " + e);
		//throw new ParserException("Unknown heal ratio for event: " + e);
	}

	public int getEffectiveHeal(long threat, boolean isReduced, boolean isGuarded, boolean isTank) {
		// default with guard: (0.5*0.75)-(0.5*0.1)
		return (int) Math.ceil(threat
				/ THREAT_HEAL
				/ (isTank ? THREAT_TANK : 1)
				/ ((isGuarded ? THREAT_GUARD : 1) - (isReduced ? THREAT_HEAL_REDUCTION : 0)));
	}

	public void processEventEffect(final Event e) {

		if (isEffectAbilityActivate(e) && !activatedAbilities.contains(e.getAbility().getGuid())) {
			activatedAbilities.add(e.getAbility().getGuid());
		}

		// effects
		if (isEffectHeal(e) || isEffectDamage(e)) {

			if (isSourceThisPlayer(e)) {
				for (int i = currentEffects.size() - 1; i > 0; i--) {
					if (currentEffects.get(i).getEffect().equals(e.getAbility()) && currentEffects.get(i).getTarget().equals(e.getTarget())) {
						// effect causing ticks on target, prolong by this event
						currentEffects.get(i).setEventIdTo(e.getEventId());
						currentEffects.get(i).setTimeTo(e.getTimestamp());
						break;
					}
				}
			}

			if (combat != null && Boolean.TRUE.equals(combat.isPvp()) && isEffectDamage(e)) {
				if (isSourceOtherPlayer(e) && e.getSource().isHostile() == null) {
					// bad person!
					e.getSource().setIsHostile(true);
//					if (logger.isDebugEnabled()) {
//						logger.debug("Hostile flag set for attacker " + e.getSource() + " at " + e.getTs());
//					}
				}
				if (isTargetOtherPlayer(e) && e.getTarget().isHostile() == null) {
					// bad person!
					e.getTarget().setIsHostile(true);
//					if (logger.isDebugEnabled()) {
//						logger.debug("Hostile flag set for target " + e.getTarget() + " at " + e.getTs());
//					}
				}
			}

		} else if ((isActionApply(e) || isActionRemove(e)) && !isEffectGeneric(e) && !isEffectGuard(e) && !isAbilityGeneric(e)) {
			// setup effect key as "source @ target: effect (ability)"
			effectKey = new EffectKey(e.getSource(), e.getTarget(), e.getEffect(), e.getAbility());

			// already running?
			effectInstances = runningEffects.get(effectKey);

			if (isActionApply(e)) {

				if (combat != null && combat.isPvp() == null && isEffectPvP(e)) {
					// flag as PVP
					combat.setIsPvp(true);
					if (logger.isDebugEnabled()) {
						logger.debug("PvP detected for " + combat.getCombatId() + " at " + e.getTs());
					}
				}

				if (effectInstances != null && (e.getTimestamp() - effectInstances.get(0).getTimeFrom() <= EFFECT_OVERLAP_TOLERANCE)) {
					// another start (buff) of the same effect within tolerance, just count it as another "stack", without actually creating new
					// instance
					// examples: Commando's Electro Net, Brontes' Static Field, etc
					stackedEffects.put(effectKey, (stackedEffects.containsKey(effectKey) ? stackedEffects.get(effectKey) + 1 : 1));
					return;
				}

				// start new effect
				final boolean isAbsorption = isEffectAbsorption(e);
				final Effect newEffect = new Effect(++effectId,
						e.getEventId(), e.getTimestamp(),
						e.getSource(), e.getTarget(),
						e.getAbility(), e.getEffect(),
						activatedAbilities.contains(e.getEffect().getGuid()),
						isAbsorption);

				if (effectInstances != null) {
					// another occurrence of known effect (debuff, overlapping etc)
					// (disallow multiple instances of absorption effects)
					while (effectInstances.size() > (isAbsorption ? 0 : 1)) {
						// more than two beginnings without any finish, clean-up
						// logger.debug("Discarding multiple effect: "+effectStack.get(0)+" at "+e);
						closeEffect(effectInstances.remove(0), null);
					}

				} else {
					// brand new
					runningEffects.put(effectKey, new ArrayList<Effect>());
					effectInstances = runningEffects.get(effectKey);
				}

				currentEffects.add(newEffect);
				effectInstances.add(newEffect);

				if (isAbsorption) {

					if (!absorptionEffectsConsumed.isEmpty()) {
						// new absorption effect = always discard all candidates
						while (absorptionEffectsConsumed.size() > 0) {
							absorptionEffectsRunning.remove(absorptionEffectsConsumed.remove(0));
						}
					}
					// new absorption effect, append to the stack
					absorptionEffectsRunning.add(newEffect);
				}

			} else {
				// finishing running effect

				if (effectInstances == null && e.getSource().getName().equals("Unknown")) {
					for (EffectKey k : runningEffects.keySet()) {
						if (k.getEffect().equals(e.getEffect()) && k.getTarget().equals(e.getTarget())) {
							// effect is being removed from a "missing" source, guess it by its type and target
							effectKey = k;
							effectInstances = runningEffects.get(effectKey);
							break;
						}
					}
				}

				if (stackedEffects.containsKey(effectKey)) {
					// just decrement the stack (buff) count and hope there is another event which will actually remove it
					if (stackedEffects.get(effectKey) == 1) {
						stackedEffects.remove(effectKey);
					} else {
						stackedEffects.put(effectKey, stackedEffects.get(effectKey) - 1);
					}
					return;
				}

				if (effectInstances != null) {

					if (effectInstances.size() > 1) {
						// multiple starting events found (start A ... start B ... [end A or B?])

						if (e.getTimestamp() - (effectInstances.get(effectInstances.size() - 1)).getTimeFrom() <= EFFECT_OVERLAP_TOLERANCE) {
							// allow overlapping events if within limit (start A ... start B ... [end A])

						} else {
							// not overlapping, discard (debuffs, death etc)
							closeEffect(effectInstances.remove(0), null);
						}
					}

					// close effect by this event
					closeEffect(effectInstances.remove(0), e);

					if (effectInstances.size() == 0) {
						// all occurrences finished
						runningEffects.remove(effectKey);
					}

				} else {
					if (logger.isDebugEnabled()) {
						// missing "beginning", can happen, just ignore
						logger.debug("Missing beginning: " + effectKey + " at " + e.getTs());
					}
				}
			}
		}
	}

	private void closeEffect(final Effect effect, final Event event) {

		if (event != null) {
			effect.setEventIdTo(event.getEventId());
			effect.setTimeTo(event.getTimestamp());
		}

		if (!effect.isActivated() && activatedAbilities.contains(effect.getEffect().getGuid())) {
			// activation ability picked up later
			if (logger.isDebugEnabled()) {
				logger.debug("Activating effect: " + effect);
			}
			effect.setIsActivated(true);
		}

		if (absorptionEffectsRunning.contains(effect)) {
			absorptionEffectsRunning.remove(effect);
			absorptionEffectsConsumed.remove(effect);

			if (effect.getTimeTo() != null) {
				absorptionEffectsClosing.add(effect);
				// flag all "consumed" candidates as closed as well (e.g. broken relics)
				while (absorptionEffectsConsumed.size() > 0) {
					absorptionEffectsRunning.remove(absorptionEffectsConsumed.remove(0));
				}
			}
		}

		// move
		currentEffects.remove(effect);
		effects.add(effect);
	}

	public void processEventAbsorption(final Event e) {

		// is this an absorption event to be linked?
		boolean isEventUnlinked = (isTargetThisPlayer(e) && e.getAbsorbed() != null);

		if (!absorptionEffectsClosing.isEmpty()) {
			// resolve already closed effects (in the order of their end)
			for (Effect effect : absorptionEffectsClosing.toArray(new Effect[absorptionEffectsClosing.size()])) {

				if (!absorptionEventsInside.isEmpty()) {
					// link pending INSIDE events to this effects as it was the first one ending
					linkAbsorptionEvents(absorptionEventsInside, effect);
				}

				if (effect.getTimeTo() < e.getTimestamp() - ((absorptionEffectsClosing.size() + absorptionEffectsRunning.size() > 1)
						// resolve possible delay (long if this is the only effect, short if another is available as well)
						? ABSORPTION_INSIDE_DELAY_WINDOW
						: ABSORPTION_OUTSIDE_DELAY_WINDOW)) {
					// effect is expiring

					if (!absorptionEventsOutside.isEmpty() && absorptionEffectsClosing.size() == 1) {
						// link pending OUTSIDE events to this effect as it is the only choice (should be only 1 anyway)
						linkAbsorptionEvents(absorptionEventsOutside, effect);
					}

					// effect was closed and expired, remove it for good
					absorptionEffectsClosing.remove(effect);
					continue;

				} else if (!isEventUnlinked) {
					// not expired yet and this is not an absorption event, nothing else to do
					continue;
				}

				// we are here = this event is an absorption & this closing effect is within a delay window
				if (!absorptionEventsOutside.isEmpty()) {
					// link queued OUTSIDE events to this effect (since this is another absorption, so there is nothing to wait for)
					linkAbsorptionEvents(absorptionEventsOutside, effect);

					if (e.getAbsorbed() + 1 < e.getValue()) {
						// ... and consume the effect as there are another active
						// (unless the mitigation was full - high chance the remaining charge will be used in a bit)
						absorptionEffectsClosing.remove(effect);
						continue;
					}
				}

				// try to link this absorption event
				if (absorptionEffectsClosing.size() > 1) {
					// not clear to which effect to link to, queue as OUTSIDE and wait
					absorptionEventsOutside.add(e.getEventId());

				} else {
					// link to the just closed effect
					absorptions.add(new Absorption(e.getEventId(), effect.getEffectId()));

					if (e.getAbsorbed() + 1 < e.getValue()) {
						// ... and consume the effect as there are another active
						// (unless the mitigation was full - high chance the remaining charge will be used in a bit)
						absorptionEffectsClosing.remove(effect);
					}
				}

				// flag event as linked
				isEventUnlinked = false;
			}
		}

		if (!isEventUnlinked) {
			// event was already linked to a closed effect or its not an absorption at all
			return;
		}

		if (absorptionEffectsRunning.isEmpty()) {
			// no absorption effect currently active
			if (logger.isDebugEnabled()) {
				logger.debug("Unknown absorption: " + e);
			}

		} else if (absorptionEffectsRunning.size() == 1) {
			// exactly one absorption effect active, link it
			absorptions.add(new Absorption(e.getEventId(), absorptionEffectsRunning.get(0).getEffectId()));
			// try to be smart - if the mitigation was not full, then its probably consumed
			if (e.getAbsorbed() + 1 < e.getValue()) {
				absorptionEffectsConsumed.add(absorptionEffectsRunning.get(0));
			}

		} else {
			// try to be smart - if the mitigation was not full, then its probably consumed by the first one activated
			if (e.getAbsorbed() + 1 < e.getValue()) {
				absorptions.add(new Absorption(e.getEventId(), absorptionEffectsRunning.get(0).getEffectId()));
				absorptionEffectsConsumed.add(absorptionEffectsRunning.get(0));
			} else {
				// multiple absorption effects currently active, queue as INSIDE and wait whichever finishes first ...
				absorptionEventsInside.add(e.getEventId());
			}
		}
	}

	private void linkAbsorptionEvents(final List<Integer> events, final Effect effect) {
		for (Integer pendingEventId : events) {
			absorptions.add(new Absorption(pendingEventId, effect.getEffectId()));
		}
		events.clear();
	}

	public void processAttackType(final Event e) {

		if (e.getAbility() == null || e.getAbility().getGuid() == null) {
			return;
		}

		if (e.getMitigation() == null || e.getMitigation().getGuid() == null) {
			return;
		}

		if (context.getAttacks().containsKey(e.getAbility().getGuid())) {
			// already classified
			return;
		}

		if (EntityGuid.Parry.getGuid() == e.getMitigation().getGuid()
				|| EntityGuid.Deflect.getGuid() == e.getMitigation().getGuid()
				|| EntityGuid.Dodge.getGuid() == e.getMitigation().getGuid()) {
			context.addAttack(AttackType.MR, e.getAbility().getGuid());

		} else if (EntityGuid.Resist.getGuid() == e.getMitigation().getGuid()) {
			context.addAttack(AttackType.FT, e.getAbility().getGuid());

		} else {
			// miss, glance, shield ... inconclusive
		}
	}

	/**
	 * Triggered if: - no discipline yet (brand new log) - outside of combat
	 * (possible discipline swap) - confirmed healer discipline (current combat)
	 *
	 * @param e
	 */
	public void processEventHots(final Event e) {

		final CharacterDiscipline currentDiscipline = (combat == null ? null : combat.getDiscipline());
		if (currentDiscipline == null || CharacterDiscipline.Medicine.equals(currentDiscipline)) {
			processEventHotsWithRefresh(e, EntityGuid.KoltoProbe.getGuid(), EntityGuid.SurgicalProbe.getGuid(), 17000);
			if (currentDiscipline != null) {
				return;
			}
		}

		if (currentDiscipline == null || CharacterDiscipline.Sawbones.equals(currentDiscipline)) {
			processEventHotsWithRefresh(e, EntityGuid.SlowReleaseMedpac.getGuid(), EntityGuid.EmergencyMedpac.getGuid(), 17000);
			if (currentDiscipline != null) {
				return;
			}
		}

		if (currentDiscipline == null || CharacterDiscipline.Bodyguard.equals(currentDiscipline)) {
			processEventHotsSimple(e, EntityGuid.KoltoShell.getGuid(), null);
			if (currentDiscipline != null) {
				return;
			}
		}

		if (currentDiscipline == null || CharacterDiscipline.CombatMedic.equals(currentDiscipline)) {
			processEventHotsSimple(e, EntityGuid.TraumaProbe.getGuid(), null);
			if (currentDiscipline != null) {
				return;
			}
		}

		if (currentDiscipline == null || CharacterDiscipline.Seer.equals(currentDiscipline)) {
			processEventHotsSimple(e, EntityGuid.ForceArmor.getGuid(), null); // 30000);
			if (currentDiscipline != null) {
				return;
			}
		}

		if (currentDiscipline == null || CharacterDiscipline.Corruption.equals(currentDiscipline)) {
			processEventHotsSimple(e, EntityGuid.StaticBarrier30.getGuid(), null); // 30000);
			if (currentDiscipline != null) {
				return;
			}
		}
	}

	private void processEventHotsWithRefresh(final Event e, final long abilityGuid, final long refresherGuid, final int defaultDuration) {
		final ActorState targetState = getActorState(e.getTarget());

		if (isAbilityEqual(e, abilityGuid)) {
			if (isEffectAbilityActivate(e)) {
				// lets wait who's the target ...
				pendingHealAbility = e.getAbility();
				return;
			}

			if ((isEffectEqual(e, abilityGuid) && isActionApply(e))
					|| (pendingHealAbility != null && isAbilityEqual(e, pendingHealAbility.getGuid()))) {
				if (isEffectEqual(e, abilityGuid)) {
					// explicit gain = 1 stack
					if (logger.isDebugEnabled() && targetState.hotStacks != 0) {
						logger.debug("Unexpected hot stack " + targetState.hotStacks + " since " + new Date(targetState.hotSince) + " at " + e);
					}
					targetState.hotStacks = 1;
				} else {
					// we got our target!
					// implicit stack increase
					targetState.hotStacks = 2;
				}
				targetState.hotEffect = e.getAbility();
				targetState.hotSince = targetState.hotLast = e.getTimestamp();
				targetState.hotDuration = (hotAverage == 0 ? defaultDuration : hotAverage);
				pendingHealAbility = null;
				return;
			}

			if (isActionRemove(e)) {
				// clear
				if (targetState.hotSince != null) {
					updateHotDurationAverage((int) (e.getTimestamp() - targetState.hotSince), defaultDuration);
				}
				targetState.hotStacks = 0;
				targetState.hotEffect = null;
				targetState.hotSince = null;
				targetState.hotDuration = 0;
				return;
			}

			return;
		}

		if (isAbilityEqual(e, refresherGuid)) {
			if (isEffectAbilityActivate(e)) {
				// lets wait who's the target ...
				pendingHealAbility = e.getAbility();
				return;
			}

			if (pendingHealAbility != null && isAbilityEqual(e, pendingHealAbility.getGuid())) {
				// we got our target!
				if (targetState.hotStacks == 2) {
					// implicit stack prolong
					targetState.hotSince = targetState.hotLast = e.getTimestamp();
				}
				pendingHealAbility = null;
			}
		}
	}

	private void processEventHotsSimple(final Event e, final long abilityGuid, final Integer duration) {
		final ActorState targetState = getActorState(e.getTarget());

		if (isAbilityEqual(e, abilityGuid)) {

			if ((isEffectEqual(e, abilityGuid) && isActionApply(e))) {
				if (logger.isDebugEnabled() && targetState.hotSince != null) {
					logger.debug("Unexpected hot since " + new Date(targetState.hotSince) + " at " + e);
				}
				targetState.hotStacks = 0;
				targetState.hotEffect = e.getAbility();
				targetState.hotSince = targetState.hotLast = e.getTimestamp();
				targetState.hotDuration = duration;
				return;
			}

			if (isActionRemove(e)) {
				if (logger.isDebugEnabled() && targetState.hotSince == null) {
					logger.debug("Unexpected fade of hot at " + e);
				}
				// clear
				targetState.hotStacks = 0;
				targetState.hotEffect = null;
				targetState.hotSince = null;
				targetState.hotDuration = null;
				return;
			}

			return;
		}
	}

	private void updateHotDurationAverage(final int last, final int def) {
		if (hotAverage == 0) {
			hotAverage = def;
			return;
		}
		hotCount++;
		hotTotal += last;
		final double newAverage = (hotTotal * 1.0 / hotCount);
		if (newAverage > hotAverage) {
			hotAverage = (int) Math.min(hotAverage * 1.05, newAverage);
		} else {
			hotAverage = (int) Math.max(hotAverage * 0.98, newAverage);
		}
	}

	private ActorState getActorState(final Actor actor) {
		if (!actorStates.containsKey(actor)) {
			actorStates.put(actor, new ActorState());
		}
		return actorStates.get(actor);
	}

	private void resolveCombatUpgrade(final RaidBoss upgradeBoss, boolean isConfident) {
		if (upgradeBoss == null) {
			// nothing to do, keep current
			return;
		}
		isConfident = isConfident || (Mode.NiM.equals(upgradeBoss.getMode())); // NiM upgrades are always confident
		if (upgradeBoss == combat.getBoss()) {
			// confirmed self
			if (logger.isDebugEnabled()) {
				logger.debug("Boss confirmed as [" + combat.getBoss() + "] at " + eventId);
			}
			combatBossUpgrade = null;

			if (isConfident) {
				instanceMode = upgradeBoss.getMode();
				instanceSize = upgradeBoss.getSize();
			}

		} else {
			// upgraded!
			combat.setBoss(upgradeBoss);

			// another upgrade available?
			if (isConfident) {
				instanceMode = upgradeBoss.getMode();
				instanceSize = upgradeBoss.getSize();
			}
			combatBossUpgrade = upgradeBoss.getPossibleUpgrade();

			if (logger.isDebugEnabled()) {
				logger.debug("Boss upgraded to [" + combat.getBoss() + "] (" + (combatBossUpgrade == null ? "final" : "tentative") + ") at " + eventId);
			}
		}
	}

	private void clearHotsTracking() {
		for (final ActorState as : actorStates.values()) {
			if (as.hotSince != null) {
				as.hotStacks = 0;
				as.hotEffect = null;
				as.hotSince = null;
				as.hotDuration = null;
			}
		}
	}
}
