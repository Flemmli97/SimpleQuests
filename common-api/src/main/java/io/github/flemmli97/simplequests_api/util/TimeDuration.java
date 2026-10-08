package io.github.flemmli97.simplequests_api.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

public class TimeDuration {

    public static final Codec<TimeDuration> CODEC = Codec.either(Codec.INT, Codec.STRING)
            .xmap(e -> e.map(TimeDuration::new, TimeDuration::new), duration -> {
                if (duration.timeString == null) {
                    return Either.left(duration.time);
                }
                return Either.right(duration.timeString);
            });

    public static final TimeDuration NONE = new TimeDuration(0);

    private final String timeString;
    private final int time;

    private TimeDuration(int time) {
        this.timeString = null;
        this.time = time;
    }

    private TimeDuration(String time) {
        this.timeString = time;
        this.time = QuestUtils.tryParseTime(time);
    }

    public static TimeDuration of(int time) {
        return new TimeDuration(time);
    }

    public static TimeDuration of(String time) {
        return new TimeDuration(time);
    }

    public int time() {
        return this.time;
    }

    public String asString() {
        return this.timeString != null ? this.timeString : String.valueOf(this.time);
    }

    @Override
    public String toString() {
        return String.format("Time: %s", this.asString());
    }
}
