package com.ixale.starparse.gui.dialog;

import com.ixale.starparse.calculator.AlacrityCalculator;
import com.ixale.starparse.gui.Config;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CalculatorDialogPresenter extends BaseDialogPresenter {
	private static final Logger LOGGER = LoggerFactory.getLogger(CalculatorDialogPresenter.class);

	@FXML
	private TabPane dialogRoot;

	@FXML
	private TextArea result;

	@FXML
	private TextField alacrityGoal, augmentNumber, enhancementNumber;

	@FXML
	private Button calculateButton;


	@SuppressWarnings("unused")
	private Config config;



	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		setContent(dialogRoot, "Calculators", null);
	}

	public void setConfig(final Config config) {
		this.config = config;
	}

    private boolean isValid(TextField textField, int min, int max) {
		if (textField.getText() == null || textField.getText().isEmpty()) {
			return false;
		}
		try {
			int x = Integer.parseInt(textField.getText());
			return x >= min && x <= max;
		} catch (NumberFormatException e) {
			return false;
		}
	}


	private void reset() {
		result.setText("");
		result.setEditable(false);
		clearFlash();
		disable(false);
	}

	private boolean validate() {
		clearFlash();
		if (!isValid(alacrityGoal, 108, 7000) || !isValid(augmentNumber, 0, 14) || !isValid(enhancementNumber, 0, 6)) {
			setFlash("Acceptable numbers:\n alacrity=[108-7000],\n augments=[0-14],\n enhancements=[0-6]");
			return false;
		}
		return true;
	}


	@Override
	public void show() {
		validate();
		super.show();
	}

	public void handleCompute(final ActionEvent event) {
		if (!validate()) {
			return;
		}
		try {
			int alacrity = Integer.parseInt(alacrityGoal.getText());
			int augments = Integer.parseInt(augmentNumber.getText());
			int enhancements = Integer.parseInt(enhancementNumber.getText());
//			Set<String[]> computations = AlacrityCalculator.computeCombinationsAsString(alacrity, enhancements, augments);
			Set<AlacrityCalculator.Combinaison> computations = AlacrityCalculator.computeCombinations(alacrity, enhancements, augments);
			if (computations.isEmpty()) {
				setFlash("No result, you should probably try with\n an higher number of enhancements or augments");
			} else if (computations.stream().map(AlacrityCalculator.Combinaison::toStringArray).flatMap(Stream::of).noneMatch(Predicate.not("80R-20"::equals))) {
				setFlash("when you only get R-20 as a result\n you should probably try with less enhancements or augments");
			}


			Set<String> lines = computations.stream()
					.map(combi -> '['+String.join(", ", combi.toStringArray())+"] alac="+combi.getTotalAlac()+", pow="+combi.getTotalPow())
					.collect(Collectors.toSet());

			this.result.setText(String.join("\n", lines));
		} catch (Exception e) {
			LOGGER.error("error while computing alacity", e);
		}
	}



	@Override
	public void handleClose(ActionEvent event) {
		reset();


		super.handleClose(event);
	}

	private void disable(boolean disabled) {
		calculateButton.setDisable(disabled);
	}
}
