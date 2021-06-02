package com.ixale.starparse.domain;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public enum SwtorClass {
    VANGUARD(true, List.of("Shield Specialist", "Plasmatech", "Tactics")),
    COMMANDO(true, List.of("Combat Medic", "Gunnery", "Assault Specialist")),
    SCOUNDREL(true, List.of("Sawbones", "Scrapper", "Ruffian")),
    GUNSLINGER(true, List.of("Sharpshooter", "Saboteur", "Dirty Fighting")),
    JEDI_SHADOW(true, List.of("Kinetic Combat", "Infiltration", "Serenity")),
    JEDI_SAGE(true, List.of("Seer", "Telekinetic", "Balance")),
    JEDI_SENTINEL(true, List.of("Watchman", "Combat", "Concentration")),
    JEDI_GUARDIAN(true, List.of("Defense", "Vigilance", "Focus")),
    POWERTECH(false, List.of("Shield Tech", "Pyrotech", "Advanced Prototype")),
    MERCENARY(false, List.of("Bodyguard", "Arsenal", "Innovative Ordnance")),
    OPERATIVE(false, List.of("Medicine", "Concealment", "Lethality")),
    SNIPER(false, List.of("Marksmanship", "Engineering", "Virulence")),
    SITH_ASSASSIN(false, List.of("Darkness", "Deception", "Hatred")),
    SITH_SORCERER(false, List.of("Corruption", "Lightning", "Madness")),
    SITH_MARAUDER(false, List.of("Annihilation", "Carnage", "Fury")),
    SITH_JUGGERNAUT(false, List.of("Immortal", "Vengeance", "Rage"));

    private final String name;
    private final boolean republic;
    private final List<String> disciplines;

    SwtorClass(boolean republic, List<String> disciplines) {
        this.name = this.name().charAt(0) + this.name().substring(1).replaceAll("_", " ").toLowerCase(Locale.ROOT);
        this.republic = republic;
        this.disciplines = disciplines;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isRepublic() {
        return republic;
    }

    public List<String> getDisciplines() {
        return disciplines;
    }

    public static SwtorClass parse(String name) {
        return Stream.of(values()).filter(swtorClass -> swtorClass.name.equals(name)).findAny().orElse(null);
    }
}
