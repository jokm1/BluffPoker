package net.leejjon.bluffpoker.stages;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import net.leejjon.bluffpoker.actors.BlackBoard;
import net.leejjon.bluffpoker.actors.Cup;
import net.leejjon.bluffpoker.actors.Dice;
import net.leejjon.bluffpoker.assets.TextureKey;
import net.leejjon.bluffpoker.dialogs.*;
import net.leejjon.bluffpoker.interfaces.StageInterface;
import net.leejjon.bluffpoker.interfaces.UserInterface;
import net.leejjon.bluffpoker.logic.*;
import net.leejjon.bluffpoker.state.Settings;
import net.leejjon.bluffpoker.ui.ClickableLabel;

import java.util.List;

public class GameStage extends AbstractStage implements UserInterface {
    private Game currentGame;
    private StageInterface stageInterface;

    private SpriteBatch batch;
    private Sound diceRoll;
    private final Cup cup;
    private Dice leftDice;
    private Dice middleDice;
    private Dice rightDice;

    private final Label thirdLatestOutputLabel;
    private final Label secondLatestOutputLabel;
    private final Label latestOutputLabel;

    private CallTooLowDialog callTooLowDialog;
    private CallNotThreeIdenticalNumbersDialog callNotThreeIdenticalNumbersDialog;
    private WarningDialog throwAtLeastOneDice;
    private WarningDialog throwAllDices;
    private WinnerDialog winnerDialog;

    private Label callInputField;

    private ClickableLabel autoButton;
    private ClickableLabel callButton;

    private static final String THROW_AT_LEAST_ONE_DICE = "Throw at least one dice!";
    private static final String THROW_WITH_ALL_DICES = "Throw with all dices!";
    private static final String AUTO_BUTTON_LABEL = "Auto";
    private static final String CALL_BUTTON_LABEL = "Call";
    private static final String TAG = "bluffpoker";
    private static final String COULD_NOT_THROW_BECAUSE_CUP_IS_MOVING = "Could not throw. Cup moving: '%b Cup is open : %b";

    private boolean autoButtonPressed = false;

    public GameStage(Skin uiSkin, final StageInterface stageInterface) {
        super(false);
        this.stageInterface = stageInterface;

        Texture callBoardTexture = stageInterface.getTexture(TextureKey.CALL_BOARD);
        Texture closedCupTexture = stageInterface.getTexture(TextureKey.CLOSED_CUP);
        Texture openCupTexture = stageInterface.getTexture(TextureKey.OPEN_CUP);
        Texture diceLockTexture = stageInterface.getTexture(TextureKey.DICE_LOCK);
        Texture cupLockTexture = stageInterface.getTexture(TextureKey.CUP_LOCK);

        batch = new SpriteBatch();
        diceRoll = Gdx.audio.newSound(Gdx.files.internal("sound/diceroll.mp3"));

        callTooLowDialog = new CallTooLowDialog(uiSkin);
        callNotThreeIdenticalNumbersDialog = new CallNotThreeIdenticalNumbersDialog(uiSkin);
        throwAtLeastOneDice = new WarningDialog(THROW_AT_LEAST_ONE_DICE, uiSkin);
        throwAllDices = new WarningDialog(THROW_WITH_ALL_DICES, uiSkin);
        winnerDialog = new WinnerDialog(stageInterface, this, uiSkin);

        Table topTable = new Table();
        topTable.setFillParent(true);
        topTable.top();

        callInputField = new Label(NumberCombination.JUNK.toString(), uiSkin, "arial64", Color.WHITE);

        final CallInputDialog callInputDialog = new CallInputDialog(this);

        float padding = 5f;

        topTable.add(callInputField).pad(padding).padTop(padding * 2).colspan(2).center();
        topTable.row();

        autoButton = new ClickableLabel(AUTO_BUTTON_LABEL, uiSkin);
        autoButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setAutoValue();
            }
        });
        autoButton.setDisabled(true);

        callButton = new ClickableLabel(CALL_BUTTON_LABEL, uiSkin);
        callButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (currentGame.isAllowedToCall()) {
                    String prefilledText = "";
                    if (autoButtonPressed) {
                        prefilledText = callInputField.getText().toString();
                    }
                    Gdx.input.getTextInput(callInputDialog, CallInputDialog.ENTER_YOUR_CALL, prefilledText, CallInputDialog.ENTER_THREE_DIGITS);
                }
            }
        });
        callButton.setDisabled(true);

        float extraClickableSpace = 10f;
        topTable.add(autoButton).width(autoButton.getWidth() + (extraClickableSpace*2)).height(autoButton.getHeight() + extraClickableSpace).colspan(1).left();
        topTable.add(callButton).width(callButton.getWidth() + (extraClickableSpace*2)).height(callButton.getHeight() + extraClickableSpace).colspan(1).right();

        // Font used in console is Microsoft JingHei
        final String console = "console";
        thirdLatestOutputLabel = new Label("", uiSkin, console, Color.BLACK);
        secondLatestOutputLabel = new Label("", uiSkin, console, Color.BLACK);
        latestOutputLabel = new Label("", uiSkin, console, Color.BLACK);

        table.left();
        table.bottom();
        float bottomPadding = 3f;
        table.add(thirdLatestOutputLabel).left().padLeft(padding).padBottom(bottomPadding);
        table.row();
        table.add(secondLatestOutputLabel).left().padLeft(padding).padBottom(bottomPadding);
        table.row();
        table.add(latestOutputLabel).left().padLeft(padding).padBottom(bottomPadding);

        // Putting certain images at the foreground or background usually goes via z index. However the z index seems broken
        // unless I pull off crazy hacks. What Actor is painted first is simply decided by the order you add them to the stage.
        // So I decided to create two groups and switch the actors between the groups.
        Group foreGroundActors = new Group();
        Group backgroundActors = new Group();
        Group dicesUnderCupActors = new Group();
        Group dicesBeforeCupActors = new Group();

        foreGroundActors.addActor(table);

        BlackBoard callBoard = new BlackBoard(callBoardTexture);

        cup = new Cup(closedCupTexture, openCupTexture, cupLockTexture, foreGroundActors, backgroundActors);

        // Load the textures of the dices.
        Texture dice1 = stageInterface.getTexture(TextureKey.DICE1);
        Texture dice2 = stageInterface.getTexture(TextureKey.DICE2);
        Texture dice3 = stageInterface.getTexture(TextureKey.DICE3);
        Texture dice4 = stageInterface.getTexture(TextureKey.DICE4);
        Texture dice5 = stageInterface.getTexture(TextureKey.DICE5);
        Texture dice6 = stageInterface.getTexture(TextureKey.DICE6);

        Texture[] diceTextures = new Texture[] {dice1, dice2, dice3, dice4, dice5, dice6};

        leftDice = new Dice(cup, diceTextures, diceLockTexture, 6, DiceLocation.LEFT, dicesUnderCupActors, dicesBeforeCupActors);
        leftDice.calculateAndSetPosition();

        middleDice = new Dice(cup, diceTextures, diceLockTexture, 4, DiceLocation.MIDDLE, dicesUnderCupActors, dicesBeforeCupActors);
        middleDice.calculateAndSetPosition();

        rightDice = new Dice(cup, diceTextures, diceLockTexture, 3, DiceLocation.RIGHT, dicesUnderCupActors, dicesBeforeCupActors);
        rightDice.calculateAndSetPosition();

        addActor(backgroundActors);
        addActor(dicesBeforeCupActors);
        addActor(foreGroundActors);
        addActor(dicesUnderCupActors);
        addActor(callBoard);
        addActor(topTable);
    }

    public void startGame(List<String> players, Settings settings) {
        resetCall();
        resetLog();

        if (currentGame == null) {
            currentGame = new Game(cup, leftDice, middleDice, rightDice, diceRoll, this, settings);
        }

        currentGame.resetPlayerIterator();
        currentGame.startGame(players);
    }

    @Override
    public void call() {
        if (currentGame.isAllowedToCall()) {
            boolean validCall = false;
            try {
                currentGame.validateCall(getNewCall());
                validCall = true;
            } catch (InputValidationException e) {
                if (currentGame.hasBelieved666()) {
                    callNotThreeIdenticalNumbersDialog.setInvalidCallMessage(currentGame.getLatestCall());
                    callNotThreeIdenticalNumbersDialog.show(this);
                } else {
                    callTooLowDialog.callTooLow(currentGame.getLatestCall());
                    callTooLowDialog.show(this);
                }
            }

            if (validCall) {
                disableCallUserInterface();
            }
        } else {
            throwAtLeastOneDice.show(this);
        }
    }

    public static int getMiddleX() {
        return Gdx.graphics.getWidth() / 2;
    }

    public static int getMiddleY() {
        return Gdx.graphics.getHeight() / 2;
    }

    public static int getTopY() {
        return Gdx.graphics.getHeight();
    }

    public void shake() {
        if (currentGame.isAllowedToThrow()) {
            int numberOfDicesToBeThrown = 0;

            if ((leftDice.isUnderCup() && !cup.isLocked()) || !leftDice.isLocked()) {
                numberOfDicesToBeThrown++;
            }

            if ((middleDice.isUnderCup() && !cup.isLocked()) || !middleDice.isLocked()) {
                numberOfDicesToBeThrown++;
            }

            if ((rightDice.isUnderCup() && !cup.isLocked()) || !rightDice.isLocked()) {
                numberOfDicesToBeThrown++;
            }

            if (currentGame.hasBelieved666()) {
                // You must throw all dices at once.
                if (numberOfDicesToBeThrown < 3) {
                    throwAllDices.show(this);
                } else {
                    currentGame.throwDices();
                    enableCallUserInterface();
                }
            } else {
                // You must throw at least one dice.
                if (numberOfDicesToBeThrown > 0 /* or is blind call and you dont need this warning. */) {
                    currentGame.throwDices();
                    enableCallUserInterface();
                } else {
                    throwAtLeastOneDice.show(this);
                }
            }
        } else {
            Gdx.app.log(TAG, String.format(COULD_NOT_THROW_BECAUSE_CUP_IS_MOVING, cup.isMoving(), (cup.isWatchingOwnThrow() | cup.isBelieving())));
        }
    }

    @Override
    public void disableCallUserInterface() {
        autoButtonPressed = false;
        callButton.setDisabled(true);
        autoButton.setDisabled(true);
    }

    @Override
    public void setCallField(String call) {
        callInputField.setText(call);
    }

    @Override
    public void enableCallUserInterface() {
        callButton.setDisabled(false);
        autoButton.setDisabled(false);
    }

    private void setAutoValue() {
        if (currentGame.isAllowedToCall()) {
            NumberCombination currentTextBoxValue;
            NumberCombination generatedCall;
            try {
                currentTextBoxValue = NumberCombination.validNumberCombinationFrom(callInputField.getText().toString());
                if (currentGame.hasBelieved666()) {
                    generatedCall = currentTextBoxValue.incrementAll();
                } else {
                    generatedCall = currentTextBoxValue.increment();
                }
            } catch (InputValidationException e) {
                // Just put something in there.
                generatedCall = NumberCombination.JUNK;
            }
            callInputField.setText(generatedCall.toString());
            autoButtonPressed = true;
        }
    }

    public void dispose() {
        batch.dispose();
        diceRoll.dispose();
        super.dispose();
    }

    @Override
    public void log(String message) {
        thirdLatestOutputLabel.setText(secondLatestOutputLabel.getText());
        secondLatestOutputLabel.setText(latestOutputLabel.getText());
        latestOutputLabel.setText(message);
    }

    @Override
    public void finishGame(String winner ) {
        winnerDialog.setWinner(winner);
        winnerDialog.show(this);
    }

    @Override
    public void restart() {
        resetCall();
        resetLog();
        currentGame.restart();
    }

    private void resetLog() {
        thirdLatestOutputLabel.setText("");
        secondLatestOutputLabel.setText("");
        latestOutputLabel.setText("");
    }

    private NumberCombination getNewCall() throws InputValidationException {
        return NumberCombination.validNumberCombinationFrom(callInputField.getText().toString());
    }

    @Override
    public void resetCall() {
        autoButton.setDisabled(true);
        callInputField.setText(NumberCombination.MIN.toString());
    }
}