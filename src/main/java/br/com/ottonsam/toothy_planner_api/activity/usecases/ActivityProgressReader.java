package br.com.ottonsam.toothy_planner_api.activity.usecases;

import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityResponse;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityEntity;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityType;
import br.com.ottonsam.toothy_planner_api.activity.entities.WeekDay;
import br.com.ottonsam.toothy_planner_api.activity.repositories.ActivityProgressCountRepository;
import br.com.ottonsam.toothy_planner_api.activity.repositories.ActivityProgressDayRepository;
import br.com.ottonsam.toothy_planner_api.activity.repositories.ActivityProgressTimeRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ActivityProgressReader {

    private final ActivityProgressDayRepository dayRepository;
    private final ActivityProgressCountRepository countRepository;
    private final ActivityProgressTimeRepository timeRepository;

    public ActivityProgressReader(
            ActivityProgressDayRepository dayRepository,
            ActivityProgressCountRepository countRepository,
            ActivityProgressTimeRepository timeRepository) {
        this.dayRepository = dayRepository;
        this.countRepository = countRepository;
        this.timeRepository = timeRepository;
    }

    public ActivityResponse toResponse(ActivityEntity activity) {
        var progressDays = progressDays(activity);
        return ActivityResponse.from(activity, progress(activity, progressDays), progressDays);
    }

    private int progress(ActivityEntity activity, List<WeekDay> progressDays) {
        if (activity.getType() == ActivityType.DAYS) {
            return progressDays.size();
        }
        if (activity.getType() == ActivityType.COUNT) {
            return countRepository.sumByActivityId(activity.getId());
        }
        return timeRepository.sumByActivityId(activity.getId());
    }

    private List<WeekDay> progressDays(ActivityEntity activity) {
        return dayRepository.findAllByActivityIdOrderByCreatedAtAsc(activity.getId()).stream()
                .map(progress -> progress.getDay())
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
