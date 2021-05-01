package com.ixale.starparse.gui.popout;

import com.ixale.starparse.domain.Actor;
import com.ixale.starparse.domain.Combat;
import com.ixale.starparse.domain.stats.CombatStats;
import com.ixale.starparse.parser.Parser;
import com.ixale.starparse.parser.TimerState;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Screen;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HotsPopoutPresenter2 extends GridPopoutPresenter {
    @FXML
    private Button autosizeButton;
    private String currentCharacterName;

    private class UiEntry {
        final int type;
        final double value;

        UiEntry(int t, double v) {
            type = t;
            value = v;
        }

        public String toString() {
            return type + " (" + value + ")";
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        super.initialize(url, resourceBundle);
        autosizeButton.setVisible(false);
    }

    @Override
    protected void refreshCombatStats(final Combat combat, final CombatStats stats) throws Exception {
        // nothing here
    }

    @Override
    public void resetCombatStats() {
        // nothing here
    }

    public void setActorStates(final Map<Actor, Parser.ActorState> actorStates, final String currentCharacterName) {
        this.currentCharacterName = currentCharacterName;
        autosizeButton.setVisible(currentCharacterName != null);

        Map<String, TimerState> collect = actorStates.entrySet().stream()
                .filter(this::isSelfOrPlayer)
                .collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
        super.setTimersStates(collect);
    }

    private boolean isSelfOrPlayer(Map.Entry<Actor, Parser.ActorState> entry) {
        Actor.Type type = entry.getKey().getType();
        return Actor.Type.SELF == type || Actor.Type.PLAYER == type;
    }

    public void handleAutosize(ActionEvent event) {
        if (currentCharacterName == null) {
            return;
        }
        try {
            detectPosition(currentCharacterName);
            hidePopout();
            showPopout();
            bringPopoutToFront();

        } catch (Exception e) {
            // tough luck
        }

    }

    private void detectPosition(final String characterName) throws Exception {
        final File settingsDir = new File(System.getenv("LOCALAPPDATA") + "/SWTOR/swtor/settings/");
        final File uiDir = new File(settingsDir, "GUIProfiles");
        if (!settingsDir.exists() || !settingsDir.isDirectory()
                || !uiDir.exists() || !uiDir.isDirectory()) {
            throw new Exception("Missing dirs " + settingsDir.exists() + " " + uiDir.exists());
        }

        File clientConfig = null;
        for (String item: Objects.requireNonNull(settingsDir.list((dir, name) -> name != null && !name.isEmpty() && name.contains(characterName)))) {
            final File f = new File(settingsDir, item);
            if (clientConfig == null || (clientConfig.lastModified() < f.lastModified())) {
                clientConfig = f;
            }
        }
        if (clientConfig == null) {
            throw new Exception("Missing client config");
        }

        String profileName = null;
        try {
            for (String line: Files.readAllLines(clientConfig.toPath())) {
                if (line != null && !line.isEmpty() && line.startsWith("GUI_Current_Profile = ")) {
                    profileName = line.substring("GUI_Current_Profile = ".length()).trim();
                    break;
                }
            }
        } catch (Exception e) {
            throw new Exception("Unable to read config: " + e.getMessage(), e);
        }

        if (profileName == null) {
            return;
        }

        final Map<String, UiEntry> ui = new HashMap<>();

        final File profileFile = new File(uiDir, profileName + ".xml");
        if (profileFile.exists()) {
            loadProfile(profileFile, ui);

        } else if ("loc:160030:preferences".equals(profileName)) { // Extended QB
            ui.put("anchorAlignment", new UiEntry(3, 2));
            ui.put("anchorXOffset", new UiEntry(3, 144));
            ui.put("anchorYOffset", new UiEntry(3, -184));
            ui.put("scale", new UiEntry(3, 1));
            ui.put("NumPerRow", new UiEntry(3, 3));
            ui.put("GroupsVisible", new UiEntry(3, 6));
            ui.put("HealthWidth", new UiEntry(3, 102));
            ui.put("HealthHeight", new UiEntry(3, 4));
            ui.put("PartySpacing", new UiEntry(3, 0));

        } else if ("loc:160031:preferences".equals(profileName)) { // Retro
            ui.put("anchorAlignment", new UiEntry(3, 3));
            ui.put("anchorXOffset", new UiEntry(3, 0));
            ui.put("anchorYOffset", new UiEntry(3, 17));
            ui.put("scale", new UiEntry(3, 1));
            ui.put("NumPerRow", new UiEntry(3, 3));
            ui.put("GroupsVisible", new UiEntry(3, 6));
            ui.put("HealthWidth", new UiEntry(3, 102));
            ui.put("HealthHeight", new UiEntry(3, 4));
            ui.put("PartySpacing", new UiEntry(3, 0));

        } else { // Default (loc:160029:preferences)
            ui.put("anchorAlignment", new UiEntry(3, 2));
            ui.put("anchorXOffset", new UiEntry(3, 84));
            ui.put("anchorYOffset", new UiEntry(3, -184));
            ui.put("scale", new UiEntry(3, 1));
            ui.put("NumPerRow", new UiEntry(3, 3));
            ui.put("GroupsVisible", new UiEntry(3, 6));
            ui.put("HealthWidth", new UiEntry(3, 102));
            ui.put("HealthHeight", new UiEntry(3, 4));
            ui.put("PartySpacing", new UiEntry(3, 0));

        }

        // position
        double minX = Screen.getPrimary().getVisualBounds().getMinX();
        double maxX = Screen.getPrimary().getVisualBounds().getMaxX();
        double minY = Screen.getPrimary().getVisualBounds().getMinY();
        double maxY = Screen.getPrimary().getVisualBounds().getMaxY();

        int slotWidth = (int) Math.round((13 + ui.get("HealthWidth").value + ui.get("PartySpacing").value * 1.25) * ui.get("scale").value);
        int slotCols = 2; //(int) (ui.get("GroupsVisible").value > ui.get("NumPerRow").value ? ui.get("NumPerRow").value : ui.get("GroupsVisible").value);
        int slotHeight = (int) Math.round((41 + ui.get("HealthHeight").value + ui.get("PartySpacing").value * 1.2) * ui.get("scale").value);
        int slotRows = 4; //(ui.get("GroupsVisible").value > ui.get("NumPerRow").value ? 8 : 4);

        super.setSlotWidth(slotWidth);
        super.setSlotCols(slotCols);
        super.setSlotHeight(slotHeight);
        super.setSlotRows(slotRows);

        double x = ui.get("anchorXOffset").value + 1;
        double y = ui.get("anchorYOffset").value;
        double h = slotHeight * (ui.get("GroupsVisible").value > ui.get("NumPerRow").value ? 8 : 4); /* raid control button */
        double w = slotWidth * Math.min(ui.get("GroupsVisible").value, ui.get("NumPerRow").value);

        int pos = (int) ui.get("anchorAlignment").value;
        switch (pos) {
            case 1:
                // top left
                break;
            case 7:
                // top mid
                x += getCenterX(w, x, minX, maxX);
                break;
            case 4:
                // top right
                x += maxX - w;
                break;
            case 6:
                // center right
                x += maxX - w;
                y += getCenterY(h, y, minY, maxY);
                break;
            case 5:
                // bottom right
                x += maxX - w;
                y += maxY - h;
                break;
            case 8:
                // bottom mid
                x += getCenterX(w, x, minX, maxX);
                y += maxY - h;
                break;
            case 2:
                // bottom left
                y += maxY - h;
                break;
            case 3:
                // center left
                y += getCenterY(h, y, minY, maxY);
                break;
            case 9:
                // center
                x += getCenterX(w, x, minX, maxX);
                y += getCenterY(h, y, minY, maxY);
                break;
            default:
                throw new Exception("Unknown position: " + ui.get("anchorAlignment").value);
        }
        y += -21 + (30 /* * ui.get("scale").value) */); /* popout title - ops frames title */

        characterConfig().setPositionX(x);
        characterConfig().setPositionY(y);
        characterConfig().setWidth((double) slotWidth * slotCols);
        characterConfig().setHeight((double) slotHeight * slotRows + TITLE_HEIGHT);
        characterConfig().setCols(slotCols);
        characterConfig().setRows(slotRows);
        updateDimensions();
    }

    private void loadProfile(final File profileFile, final Map<String, UiEntry> ui) throws Exception {

        final Pattern p = Pattern.compile("^\\<(?<name>[a-z]+) Type=\"(?<type>[0-9]+)\" Value=\"(?<value>[\\-0-9.]+)\" /\\>$",
                Pattern.CASE_INSENSITIVE);
        try {
            Boolean read = null;
            for (String line: Files.readAllLines(profileFile.toPath())) {
                if (Boolean.TRUE.equals(read)) {
                    if (line != null && !line.isEmpty() && line.contains("</RaidFrames>")) {
                        break;
                    }
                    final Matcher m = p.matcher(line.trim());
                    if (!m.matches()) {
                        throw new Exception("Unable to match: " + line);
                    }
                    ui.put(m.group("name"), new UiEntry(Integer.parseInt(m.group("type")), Double.parseDouble(m.group("value"))));
                    continue;
                }
                if (line != null && !line.isEmpty() && line.contains("<RaidFrames>")) {
                    read = true;
                    continue;
                }
            }
        } catch (Exception e) {
            throw new Exception("Unable to read config: " + e.getMessage(), e);
        }
    }

    private long getCenterX(double w, double x, double minX, double maxX) {
        double offset = w / 2;
        double anchor = (maxX - minX) / 2;
        if ((offset + x) > anchor) { // overflow
            offset = offset + (offset + x - anchor);
        }
        return Math.round(anchor - offset);
    }

    private long getCenterY(double h, double y, double minY, double maxY) {
        double offset = h / 2;
        double anchor = (maxY - minY) / 2;
        if ((offset + y) > anchor) { // overflow
            offset = offset + (offset + y - anchor);
        }
        return Math.round(anchor - offset);
    }

}
