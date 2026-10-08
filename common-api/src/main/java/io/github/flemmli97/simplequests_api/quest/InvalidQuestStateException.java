package io.github.flemmli97.simplequests_api.quest;

public class InvalidQuestStateException extends RuntimeException {

    public InvalidQuestStateException() {
        super();
    }

    public InvalidQuestStateException(String message) {
        super(message);
    }
}
