package com.ixale.starparse.gui.popout;


import com.ixale.starparse.parser.TimerState;
import com.ixale.starparse.time.TimeUtils;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;

import java.net.URL;
import java.util.*;


abstract public class GridPopoutPresenter extends BasePopoutPresenter {

	protected static final int DEFAULT_SLOT_WIDTH = 100,
		DEFAULT_SLOT_HEIGHT = 50,
		DEFAULT_SLOT_COLS = 2,
		DEFAULT_SLOT_ROWS = 4,
		// timeouts
		TIMEOUT_WITH_DURATION = 5000, // 5s
		TIMEOUT_WITHOUT_DURATION = 10 * 60 * 1000; // 10min

	protected static final int TITLE_HEIGHT = 20;

	@FXML
	private AnchorPane frames, popoutHeader;


	private class TimerFrame {
		int col, row;
		final AnchorPane pane;
		final TimerState state;

		TimerFrame(final AnchorPane pane, final TimerState state) {
			this.pane = pane;
			this.state = state;
		}
	}

	private final TimerFrame[][] matrix = new TimerFrame[6][8];
	private final Line[] lines = new Line[16];
	private final Map<String, TimerFrame> timers = new HashMap<>();
	private final Map<String, Long> ignoreTimers = new HashMap<>();


	protected int slotWidth = DEFAULT_SLOT_WIDTH,
		slotHeight = DEFAULT_SLOT_HEIGHT,
		slotCols = DEFAULT_SLOT_COLS,
		slotRows = DEFAULT_SLOT_ROWS;

	protected void setSlotWidth(int slotWidth) {
		this.slotWidth = slotWidth;
	}

	protected void setSlotHeight(int slotHeight) {
		this.slotHeight = slotHeight;
	}

	protected void setSlotCols(int slotCols) {
		this.slotCols = slotCols;
	}

	protected void setSlotRows(int slotRows) {
		this.slotRows = slotRows;
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		super.initialize(url, resourceBundle);

		offsetY = 400;
		offsetX = (int) Screen.getPrimary().getVisualBounds().getMaxX() - 290;
		itemGap = 0;

		updateDimensions();
		freeform = true; // no scaling

		for (int c = 0; c <= matrix.length; c++) {
			lines[c] = new Line();
			lines[c].setStroke(Color.RED);
			lines[c].setStrokeWidth(1);
			frames.getChildren().add(lines[c]);
		}

		for (int r = 0; r <= matrix[0].length; r++) {
			lines[matrix.length + 1 + r] = new Line();
			lines[matrix.length + 1 + r].setStroke(Color.RED);
			lines[matrix.length + 1 + r].setStrokeWidth(1);
			frames.getChildren().add(lines[matrix.length + 1 + r]);
		}

		// target node
		frames.setOnDragEntered(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent dragEvent) {
				dragEvent.consume();
			}
		});
		frames.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent dragEvent) {
				// drop to folders or root
				if (dragEvent.getDragboard().hasString()) {
					dragEvent.acceptTransferModes(TransferMode.MOVE);
				}
				dragEvent.consume();
			}
		});
		frames.setOnDragExited(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent dragEvent) {
				dragEvent.consume();
			}
		});
		frames.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent dragEvent) {
				// lookup who is being moved
				final TimerFrame playerToMove = timers.get(dragEvent.getDragboard().getString());
				if (playerToMove == null) {
					return;
				}

				// translate to x,y
				int toCol = (int) Math.floor(dragEvent.getX() / slotWidth);
				int toRow = (int) Math.floor(dragEvent.getY() / slotHeight);

				// anyone already there?
				for (final TimerFrame frame: timers.values()) {
					if (frame.col == toCol && frame.row == toRow) {
						if (frame == playerToMove) {
							// nothing to do
							return;
						}
						// swap places
						frame.col = playerToMove.col;
						frame.row = playerToMove.row;
						break;
					}
				}

				playerToMove.col = toCol;
				playerToMove.row = toRow;

				dragEvent.consume();
				repaintPlayers();
			}
		});
	}


	protected void updateDimensions() {
		height = slotHeight * slotRows + TITLE_HEIGHT;
		width = slotWidth * slotCols;
		itemHeight = slotHeight;
		itemWidth = slotWidth;

		resizeStepW = slotWidth;
		resizeStepH = slotHeight;
		minW = slotWidth * 2;
		maxW = slotWidth * 6;
		minH = slotHeight * getMinRows() + TITLE_HEIGHT;
		maxH = slotHeight * 8 + TITLE_HEIGHT;

		if (hasPopout()) {
			getPopout().setResizeDimensions(
				resizeStepW, minW, maxW,
				resizeStepH, minH, maxH);
		}
	}

	protected int getMinRows() {
		return 4;
	}


	public void setTimersStates(final Map<String, TimerState> timersStates) {

		if (!isEnabled()) {
			return;
		}

		if (timers.size() > matrix.length * matrix[0].length) {
			// safety
			return;
		}

		final List<Object[]> newTimers = new ArrayList<>();
		timersStates.forEach((timerName, timerState) -> {
			if (timerState.getLast() != null) {
				// ignore extremely old
				if (timerState.getLast() < TimeUtils.getCurrentTime() - TIMEOUT_WITHOUT_DURATION) {
					return;
				}

				if (!timers.containsKey(timerName)) {
					if (ignoreTimers.containsKey(timerName)) {
						if (ignoreTimers.get(timerName) >= timerState.getLast()) {
							return;
						}
						ignoreTimers.remove(timerName);
					}
					newTimers.add(new Object[]{timerName, timerState});
				}
			}
		});




		for (final String timerName: timersStates.keySet()) {
			final TimerState state = timersStates.get(timerName);

		}
		if (newTimers.isEmpty()) {
			return;
		}
		// ensure the players are added according to their HOT application to allow simpler overlay setup
		newTimers.sort(Comparator.comparing(o -> ((TimerState) o[1]).getLast()));
		for (final Object[] pair: newTimers) {
			addPlayer((String) pair[0], (TimerState) pair[1]);
		}
	}

	public void resetTimers() {
		for (final TimerFrame frame: timers.values()) {
			frames.getChildren().remove(frame.pane);
		}
		timers.clear();
		for (int c = 0; c < matrix.length; c++) {
			for (int r = 0; r < matrix[c].length; r++) {
				matrix[c][r] = null;
			}
		}
		ignoreTimers.clear();
	}

	public void tickHots() {
		for (final TimerFrame frame: timers.values()) {
			boolean hotActive = false;
			int hotStacks = 0;
			if (frame.state.getEffect() != null && frame.state.getSince() != null) {
				hotActive = repaintHot(frame, null);
				if (hotActive) {
					hotStacks = frame.state.getStacks();
				}
			}
			if (hotActive != frame.pane.getChildren().get(1).isVisible()) {
				frame.pane.getChildren().get(1).setVisible(hotActive);
			}
			if ((hotStacks > 0) != frame.pane.getChildren().get(2).isVisible()) {
				frame.pane.getChildren().get(2).setVisible(hotStacks > 0);
			}
			if (hotStacks > 0) {
				((Label) frame.pane.getChildren().get(2)).setText(String.valueOf(hotStacks));
			}
		}
	}

	public void setSolid(boolean solid) {
		// always transparent
	}

	public void setBackgroundColor(final Color color) {
		super.setBackgroundColor(color);
		this.backgroundColor = Color.web("#000000aa");

		if (popoutBackground != null) {
			popoutBackground.setFill(backgroundColor);
		}
	}

	private void addPlayer(final String characterName, final TimerState state) {

		if (state == null || characterName == null || characterName.isEmpty()) {
			return; // safety
		}

		final AnchorPane pane = new AnchorPane();
		pane.setPrefWidth(slotWidth);
		pane.setPrefHeight(slotHeight);
		pane.setCursor(Cursor.MOVE);
		pane.backgroundProperty().bind(Bindings.when(pane.hoverProperty()).then(
			new Background(new BackgroundFill(Color.web("#00000066"), CornerRadii.EMPTY, Insets.EMPTY)))
			.otherwise((Background) null));

		final Label title = new Label(characterName);
		title.setTextFill(Color.WHITE);
		title.setFont(Font.font("System", 14));
		title.setVisible(!mouseTransparent);

		AnchorPane.setLeftAnchor(title, 15d);

		final Canvas hot = new Canvas(1, 1);
		hot.setOpacity(0.7);

		AnchorPane.setTopAnchor(hot, 1d);
		AnchorPane.setLeftAnchor(hot, 1d);

		final Label stacks = new Label("");
		stacks.setFont(Font.font("System", FontWeight.BOLD, 13));
		stacks.setTextFill(Color.WHITE);
		stacks.getStyleClass().add("outlined");
		stacks.setPrefWidth(15);
		stacks.setPrefHeight(15);
		stacks.setAlignment(Pos.CENTER);

		AnchorPane.setTopAnchor(stacks, 1d);
		AnchorPane.setLeftAnchor(stacks, 1d);

		final Button button = new Button("X");
		button.opacityProperty().bind(Bindings.when(pane.hoverProperty()).then(1.0).otherwise(0.0));
		button.setFont(Font.font("System", 12));
		button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				removePlayer(characterName);
			}
		});

		AnchorPane.setTopAnchor(button, 5d);
		AnchorPane.setRightAnchor(button, 18d);

		pane.getChildren().addAll(title, hot, stacks, button);

		// source node
		pane.setOnDragDetected(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				final Dragboard dragBoard = pane.startDragAndDrop(TransferMode.MOVE);
				dragBoard.setDragView(pane.snapshot(null, null));
				final ClipboardContent content = new ClipboardContent();
				content.put(DataFormat.PLAIN_TEXT, characterName);
				dragBoard.setContent(content);
				event.consume();
			}
		});
		pane.setOnDragDone(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent dragEvent) {
				dragEvent.consume();
			}
		});

		// try to find space in visible area
		final TimerFrame frame = new TimerFrame(pane, state);

		Integer col = null, row = null;
		m: for (int c = 0; c < slotCols; c++) {
			for (int r = 0; r < slotRows; r++) {
				if (matrix[c][r] == null) {
					col = c;
					row = r;
					matrix[c][r] = frame;
					break m;
				}
			}
		}
		// fall back to anything available
		m: if (col == null) {
			for (int r = 0; r < matrix[0].length; r++) {
				for (int c = 0; c < matrix.length; c++) {
					if (matrix[c][r] == null) {
						col = c;
						row = r;
						matrix[c][r] = frame;
						break m;
					}
				}
			}
		}
		if (col == null) {
			// error?
			return;
		}
		frame.col = col;
		frame.row = row;

		timers.put(characterName, frame);

		frames.getChildren().add(pane);

		repaintPlayer(frame);
	}

	public void removePlayer(final String characterName) {
		final TimerFrame frame = timers.remove(characterName);
		if (frame == null) {
			return;
		}
		frames.getChildren().remove(frame.pane);
		matrix[frame.col][frame.row] = null;

		if (frame.col < slotCols && frame.row < slotRows) {
			// use the space for someone else
			m: for (int c = 0; c < matrix.length; c++) {
				for (int r = 0; r < matrix[c].length; r++) {
					if ((c >= slotCols || r >= slotRows) && matrix[c][r] != null) {
						final TimerFrame other = matrix[c][r];
						other.col = frame.col;
						other.row = frame.row;
						repaintPlayer(other);
						matrix[frame.col][frame.row] = other;
						matrix[c][r] = null;
						break m;
					}
				}
			}
		}

		ignoreTimers.put(characterName, TimeUtils.getCurrentTime());
	}

	@Override
	public void repaint(Object source) {
		super.repaint(source);

		final double width = popoutBackground.getWidth();
		final double height = popoutBackground.getHeight();

		if (source != null) {
			if (resizeN == source || resizeE == source) {
				// slot count change
				slotCols = (int) Math.round(width / slotWidth);
				slotRows = (int) Math.round(height / slotHeight);

				characterConfig().setCols(slotCols);
				characterConfig().setRows(slotRows);

			} else {
				// slot width change
				slotWidth = (int) Math.round(width / slotCols);
				slotHeight = (int) Math.round(height / slotRows);
			}
			updateDimensions();
		}

		frames.setPrefWidth(width);
		frames.setPrefHeight(height);

		for (int c = 0; c <= matrix.length; c++) {
			lines[c].setStartX(c == 0 ? 1 : (c == matrix.length ? frames.getPrefWidth() - 1 : c * slotWidth));
			lines[c].setStartY(0);
			lines[c].setEndX(c == 0 ? 1 : (c == matrix.length ? frames.getPrefWidth() - 1 : c * slotWidth));
			lines[c].setEndY(height);
		}

		for (int r = 0; r <= matrix[0].length; r++) {
			lines[matrix.length + 1 + r].setStartX(0);
			lines[matrix.length + 1 + r].setStartY(r == 0 ? 1 : (r == matrix[0].length ? frames.getPrefHeight() - 1 : r * slotHeight));
			lines[matrix.length + 1 + r].setEndX(width);
			lines[matrix.length + 1 + r].setEndY(r == 0 ? 1 : (r == matrix[0].length ? frames.getPrefHeight() - 1 : r * slotHeight));
		}

		repaintPlayers();
	}

	private void repaintPlayers() {

		for (final TimerFrame frame: timers.values()) {
			frame.pane.setPrefWidth(slotWidth);
			frame.pane.setPrefHeight(slotHeight);

			repaintPlayer(frame);
		}
	}

	private void repaintPlayer(TimerFrame frame) {
		if (frame.col >= slotCols || frame.row >= slotRows) {
			frame.pane.setVisible(false);
			return;
		}

		// title width
		((Label) frame.pane.getChildren().get(0)).setMaxWidth(slotWidth - 15d);

		// align frame
		AnchorPane.setTopAnchor(frame.pane, (double) frame.row * slotHeight);
		AnchorPane.setLeftAnchor(frame.pane, (double) frame.col * slotWidth);

		// align label
		final Label l = (Label) frame.pane.getChildren().get(0);
		AnchorPane.setBottomAnchor(l, Math.min(slotHeight - 35, 21d));

		// align stacks
		final int w = Math.max(15, Math.min(slotHeight / 4, 30));
		if (w != ((Canvas) frame.pane.getChildren().get(1)).getWidth()) {
			repaintHot(frame, w);
		}
		final Label s = (Label) frame.pane.getChildren().get(2);
		s.setPrefHeight(w);
		s.setPrefWidth(w);

		if (!frame.pane.isVisible()) {
			frame.pane.setVisible(true);
		}
	}

	abstract void repaintTimer(GraphicsContext gc, double width, double height, TimerState timerState);

	private boolean repaintHot(final TimerFrame frame, final Integer newSize) {
		final Integer duration = frame.state.getDuration();
		final Long since = frame.state.getSince();
		final Canvas canvas = (Canvas) frame.pane.getChildren().get(1);
		if (newSize != null) {
			canvas.setHeight(newSize);
			canvas.setWidth(newSize);
		}
		if (since == null || (duration != null && duration < (TimeUtils.getCurrentTime() - since - TIMEOUT_WITH_DURATION))) { // arbitrary tolerance
			return false;
		}
		if (TimeUtils.getCurrentTime() - since > TIMEOUT_WITHOUT_DURATION) {
			// expired (out of range etc.)
			return false;
		}

		final GraphicsContext gc = canvas.getGraphicsContext2D();
		this.repaintTimer(gc, canvas.getWidth(), canvas.getHeight(), frame.state);
		return true;
	}

	@Override
	public void showPopout() {

		super.showPopout();

		if (characterConfig().getCols() != null) {
			slotCols = characterConfig().getCols();
		}
		slotWidth = (int) Math.round(popoutBackground.getWidth() / slotCols);
		if (characterConfig().getRows() != null) {
			slotRows = characterConfig().getRows();
		}
		slotHeight = (int) Math.round(popoutBackground.getHeight() / slotRows);

		updateDimensions();
		repaint(null);
	}











	public void setMouseTransparent(boolean mouseTransparent) {

		if (mouseTransparent == this.mouseTransparent) {
			return;
		}

		popoutBackground.setVisible(!mouseTransparent);
		popoutHeader.setVisible(!mouseTransparent);

		for (final TimerFrame frame: timers.values()) {
			frame.pane.getChildren().get(0).setVisible(!mouseTransparent); // title

		}
		for (final Line line: lines) {
			line.setVisible(!mouseTransparent);
		}
		repaintPlayers();

		super.setMouseTransparent(mouseTransparent);
	}
}
