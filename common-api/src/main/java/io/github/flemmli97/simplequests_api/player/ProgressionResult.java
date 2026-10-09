package io.github.flemmli97.simplequests_api.player;

import io.github.flemmli97.simplequests_api.quest.entry.ResolvedQuestTask;

public record ProgressionResult<R extends ResolvedQuestTask>(String name, R task, TaskResult result) {
}
