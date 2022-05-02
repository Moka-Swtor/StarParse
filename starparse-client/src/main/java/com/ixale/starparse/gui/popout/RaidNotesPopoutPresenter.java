package com.ixale.starparse.gui.popout;

import com.ixale.starparse.domain.Combat;
import com.ixale.starparse.domain.stats.CombatStats;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class RaidNotesPopoutPresenter extends BasePopoutPresenter {

	@FXML
	private TextFlow raidNotes;

	@FXML
	private ScrollPane notesWrapper;

	private String currentNote;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		super.initialize(url, resourceBundle);

		this.offsetY = DEFAULT_OFFSET_Y;
		this.offsetX = DEFAULT_OFFSET_X + 2 * (DEFAULT_WIDTH + DEFAULT_OFFSET_X);

		width = 200;
		height = 190;
		minH = 114;
		maxH = 3 * height;

		notesWrapper.setOnMouseEntered(event -> notesWrapper.setVbarPolicy(ScrollBarPolicy.AS_NEEDED));
		notesWrapper.setOnMouseExited(event -> notesWrapper.setVbarPolicy(ScrollBarPolicy.NEVER));

		raidNotes.setPadding(new Insets(5));

		((Text) raidNotes.getChildren().get(0)).setFont(Font.font("System", FontWeight.BOLD, 12));
	}

	@Override
	protected void refreshCombatStats(final Combat combat, final CombatStats stats) throws Exception {
		// pass
	}

	@Override
	public void resetCombatStats() {
		// pass
	}

	public boolean updateNoteIfNeeded(String note, boolean preview) {
		final boolean updated;
		if (preview) {
			updated = false;
		} else {
			updated = !Objects.equals(note, currentNote);
			currentNote = note;
		}
		((Text) raidNotes.getChildren().get(0)).setText(note);
		return updated;
	}

	@Override
	public void setTextColor(Color color) {
		((Text) raidNotes.getChildren().get(0)).setFill(color);
		super.setTextColor(color);
	}

	public String getNote() {
		return currentNote;
	}

	@Override
	public void setOpacity(double opacity) {
		//((Text) raidNotes.getChildren().get(0)).setOpacity(opacity);
		super.setOpacity(opacity);
	}
}
