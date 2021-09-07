package com.ixale.starparse.domain;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CharacterClass {

	Marauder("Marauder", false, 1),
	Juggernaut("Juggernaut", false, 2),
	Sorcerer("Sorcerer", false,3),
	Assassin("Assassin", false, 4),
	Mercenary("Mercenary", false, 5),
	Powertech("Powertech", false, 6),
	Operative("Operative", false, 7),
	Sniper("Sniper", false, 8),
	Sentinel("Sentinel", true, 9),
	Guardian("Guardian", true, 10),
	Sage("Sage", true, 11),
	Shadow("Shadow", true, 12),
	Commando("Commando", true, 13),
	Vanguard("Vanguard", true, 14),
	Scoundrel("Scoundrel", true, 15),
	Gunslinger("Gunslinger", true, 16)
	;

	private final String fullName;
	private final boolean republic;
	private final int order;
	private final List<CharacterDiscipline> characterDisciplines = new ArrayList<>();

	CharacterClass(String fullName, boolean republic, int order) {
		this.fullName = fullName;
		this.republic = republic;
		this.order = order;
	}

	public String getFullName() {
		return fullName;
	}

	public String toString() {
		return fullName;
	}

	public static CharacterClass parse(String fullName) {
		return Stream.of(values()).filter(characterClass -> characterClass.fullName.equals(fullName)).findAny().orElse(null);
	}

	public boolean isRepublic() {
		return republic;
	}

	public List<CharacterDiscipline> getCharacterDisciplines() {
		if (characterDisciplines.isEmpty()) {
			Stream.of(CharacterDiscipline.values())
					.filter(characterDiscipline -> characterDiscipline.getCharacterClass() == this)
					.forEach(characterDisciplines::add);
		}
		return Collections.unmodifiableList(characterDisciplines);
	}

	public static Map<CharacterClass, List<CharacterDiscipline>> buildClassToDisciplineMap() {
		return Stream.of(CharacterDiscipline.values())
				.collect(Collectors.groupingBy(CharacterDiscipline::getCharacterClass));
	}

	public List<String> getCharacterDisciplineNames() {
		return getCharacterDisciplines().stream().map(CharacterDiscipline::getFullName).collect(Collectors.toList());
	}
}
