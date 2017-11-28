package net.leejjon.bluffpoker.stages;

import net.leejjon.bluffpoker.actors.Logo;
import net.leejjon.bluffpoker.dialogs.CreditsDialog;
import net.leejjon.bluffpoker.enums.TextureKey;
import net.leejjon.bluffpoker.interfaces.StageInterface;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import net.leejjon.bluffpoker.state.GameState;

public class StartStage extends AbstractStage {
	public StartStage(Skin uiSkin, final StageInterface stageInterface) {
		super(true);

		Logo logo = new Logo(stageInterface.getTexture(TextureKey.LOGO));

		Label titleLabel = new Label("Bluff Poker", uiSkin);
		titleLabel.setColor(Color.WHITE);

		TextButton continueButton = new TextButton("Continue", uiSkin);
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                stageInterface.continuePlaying();
            }
        });

		TextButton playButton = new TextButton("Start", uiSkin);
		playButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				stageInterface.startSelectingPlayersToPlayWith();
			}
		});
		
		TextButton settingsButton = new TextButton("SettingsState", uiSkin);
		settingsButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				stageInterface.openSettingsStage();
			}
		});

        CreditsDialog creditsDialog = new CreditsDialog(uiSkin);
        TextButton creditsButton = new TextButton("Credits", uiSkin);
        creditsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                creditsDialog.show(StartStage.this);
            }
        });

		logo.add(titleLabel).padBottom(5f);
        logo.row();
        logo.add(playButton).padBottom(10f);
        logo.row();

		if (!GameState.getInstance().isNewGameState()) {
            logo.add(continueButton).padBottom(10f);
            logo.row();
        }
        logo.add(settingsButton).padBottom(10f);
        logo.row();
        logo.add(creditsButton);

		addActor(logo);
	}
}
