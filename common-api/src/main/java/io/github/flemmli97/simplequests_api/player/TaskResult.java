package io.github.flemmli97.simplequests_api.player;

/**
 * Result of trying to complete a task
 * <p>
 * NONE = Nothing was done for the task
 * <br>
 * ADVANCED = A change occured at the task but it was not enough to complete it. E.g. not enough entities killed yet
 * <br>
 * COMPLETED = Task has been completed
 */
public enum TaskResult {
    NONE,
    ADVANCED,
    COMPLETED,
}
