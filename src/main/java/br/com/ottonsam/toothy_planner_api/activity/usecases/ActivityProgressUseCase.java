package br.com.ottonsam.toothy_planner_api.activity.usecases;

import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityProgressCountRequest;
import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityProgressDayRequest;
import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityProgressTimeRequest;
import br.com.ottonsam.toothy_planner_api.activity.dtos.ActivityResponse;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityProgressCountEntity;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityProgressDayEntity;
import br.com.ottonsam.toothy_planner_api.activity.entities.ActivityProgressTimeEntity;
import br.com.ottonsam.toothy_planner_api.activity.repositories.ActivityProgressCountRepository;
import br.com.ottonsam.toothy_planner_api.activity.repositories.ActivityProgressDayRepository;
import br.com.ottonsam.toothy_planner_api.activity.repositories.ActivityProgressTimeRepository;
import br.com.ottonsam.toothy_planner_api.auth.usecases.CurrentUserProvider;
import br.com.ottonsam.toothy_planner_api.config.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActivityProgressUseCase {

    private final ActivityUseCase activityUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final ActivityProgressDayRepository dayRepository;
    private final ActivityProgressCountRepository countRepository;
    private final ActivityProgressTimeRepository timeRepository;
    private final TimeTextParser timeTextParser;
    private final ActivityProgressReader activityProgressReader;

    public ActivityProgressUseCase(
            ActivityUseCase activityUseCase,
            CurrentUserProvider currentUserProvider,
            ActivityProgressDayRepository dayRepository,
            ActivityProgressCountRepository countRepository,
            ActivityProgressTimeRepository timeRepository,
            TimeTextParser timeTextParser,
            ActivityProgressReader activityProgressReader) {
        this.activityUseCase = activityUseCase;
        this.currentUserProvider = currentUserProvider;
        this.dayRepository = dayRepository;
        this.countRepository = countRepository;
        this.timeRepository = timeRepository;
        this.timeTextParser = timeTextParser;
        this.activityProgressReader = activityProgressReader;
    }

    public ActivityResponse registerDay(ActivityProgressDayRequest request) {
        var user = currentUserProvider.get();
        var activity = activityUseCase.findOwned(request.activityId(), user.getId());
        if (request.day() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Week day is required");
        }
        if (dayRepository.existsByActivityIdAndDay(activity.getId(), request.day())) {
            throw new ApiException(HttpStatus.CONFLICT, "Week day already registered for this activity");
        }
        dayRepository.save(ActivityProgressDayEntity.create(activity, request.day()));
        return activityProgressReader.toResponse(activity);
    }

    public ActivityResponse registerCount(ActivityProgressCountRequest request) {
        var user = currentUserProvider.get();
        var activity = activityUseCase.findOwned(request.activityId(), user.getId());
        if (request.value() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Progress value is required");
        }
        countRepository.save(ActivityProgressCountEntity.create(activity, request.value()));
        return activityProgressReader.toResponse(activity);
    }

    public ActivityResponse registerTime(ActivityProgressTimeRequest request) {
        var user = currentUserProvider.get();
        var activity = activityUseCase.findOwned(request.activityId(), user.getId());
        var minutes = timeTextParser.parse(request.time());
        timeRepository.save(ActivityProgressTimeEntity.create(activity, minutes));
        return activityProgressReader.toResponse(activity);
    }
}
