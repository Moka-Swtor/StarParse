package com.ixale.starparse.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CharacterClass {

	Marauder("Marauder", false),
	Juggernaut("Juggernaut", false),
	Sorcerer("Sorcerer", false),
	Assassin("Assassin", false),
	Mercenary("Mercenary", false),
	Powertech("Powertech", false),
	Operative("Operative", false),
	Sniper("Sniper", false),
	Sentinel("Sentinel", true),
	Guardian("Guardian", true),
	Sage("Sage", true),
	Shadow("Shadow", true),
	Commando("Commando", true),
	Vanguard("Vanguard", true),
	Scoundrel("Scoundrel", true),
	Gunslinger("Gunslinger", true)
	;

	private final String fullName;
	private final boolean republic;
	private final List<CharacterDiscipline> characterDisciplines = new ArrayList<>();

	CharacterClass(String fullName, boolean republic) {
		this.fullName = fullName;
		this.republic = republic;
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

	public List<String> getCharacterDisciplineNames() {
		return getCharacterDisciplines().stream().map(CharacterDiscipline::getFullName).collect(Collectors.toList());
	}
}
