package net.leejjon.bluffpoker.interfaces;

public interface UserInterface {
    void log(String message);

    void finishGame(String winner);

    void restart();

    void enableCallUserInterface();

    void disableCallUserInterface();

    void setCallField(String call);

    void call();

    void resetCall();
}