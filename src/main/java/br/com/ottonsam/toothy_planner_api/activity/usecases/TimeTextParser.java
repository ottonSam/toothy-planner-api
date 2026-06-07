package br.com.ottonsam.toothy_planner_api.activity.usecases;

import br.com.ottonsam.toothy_planner_api.config.ApiException;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TimeTextParser {

    private static final Pattern TIME_PART_PATTERN = Pattern.compile("(\\d+)\\s*([hm])", Pattern.CASE_INSENSITIVE);

    public int parse(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Time value is required");
        }

        var matcher = TIME_PART_PATTERN.matcher(value.trim());
        var totalMinutes = 0;
        var lastEnd = 0;
        var found = false;
        while (matcher.find()) {
            if (!value.substring(lastEnd, matcher.start()).isBlank()) {
                throw invalidTime();
            }
            var amount = Integer.parseInt(matcher.group(1));
            var unit = matcher.group(2).toLowerCase(java.util.Locale.ROOT);
            totalMinutes += "h".equals(unit) ? amount * 60 : amount;
            lastEnd = matcher.end();
            found = true;
        }

        if (!found || !value.substring(lastEnd).isBlank() || totalMinutes <= 0) {
            throw invalidTime();
        }
        return totalMinutes;
    }

    private ApiException invalidTime() {
        return new ApiException(HttpStatus.BAD_REQUEST, "Time must use hours and minutes, for example 3h 20m");
    }
}
