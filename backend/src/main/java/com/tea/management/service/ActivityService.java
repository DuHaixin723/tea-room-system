package com.tea.management.service;

import com.tea.management.domain.entity.Activity;
import com.tea.management.domain.entity.ActivityRegistration;
import com.tea.management.domain.enums.ActivityStatus;
import com.tea.management.domain.enums.RoleType;
import com.tea.management.dto.request.ActivityRegistrationRequest;
import com.tea.management.dto.request.ActivityRequest;
import com.tea.management.dto.request.ActivityStatusRequest;
import com.tea.management.dto.request.ActivitySummaryRequest;
import com.tea.management.exception.BusinessException;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.ActivityRegistrationRepository;
import com.tea.management.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
/**
 * ActivityService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityRegistrationRepository registrationRepository;
    private final NotificationService notificationService;

    public Page<Activity> listActivities(Pageable pageable) {
        return activityRepository.findAll(pageable);
    }

    public Page<Activity> listPublishedActivities(Pageable pageable) {
        return activityRepository.findByStatusAndEndTimeAfter(ActivityStatus.PUBLISHED, LocalDateTime.now(), pageable);
    }

    /**
     * 创建新活动；若发起人是茶室员，还会发送待审核通知。
     */
    public Activity createActivity(ActivityRequest request, Long creatorUserId, RoleType creatorRole, boolean adminMode) {
        validateTimeRange(request.startTime(), request.endTime());
        Activity activity = new Activity();
        activity.setCreatorUserId(creatorUserId);
        activity.setCreatorRole(creatorRole);
        apply(activity, request, adminMode);
        Activity saved = activityRepository.save(activity);
        if (!adminMode && creatorRole == RoleType.STAFF) {
            notificationService.notifyAdmins(
                    "ACTIVITY_REVIEW_PENDING",
                    "有新的活动待审核",
                    "茶室员发起的活动“" + saved.getTitle() + "”已提交审核。",
                    "ACTIVITY",
                    saved.getId(),
                    "/activities"
            );
        }
        return saved;
    }

    public Activity updateActivity(Long id, ActivityRequest request, boolean adminMode) {
        Activity activity = requireActivity(id);
        validateTimeRange(request.startTime(), request.endTime());
        apply(activity, request, adminMode);
        return activityRepository.save(activity);
    }

    public Activity requireActivity(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("活动不存在"));
    }

    /**
     * 更新活动审核状态，并把审核结果通知活动发起人。
     */
    public Activity updateStatus(Long id, ActivityStatusRequest request) {
        Activity activity = requireActivity(id);
        ActivityStatus next = ActivityStatus.valueOf(request.status().toUpperCase());
        if (!isReviewStatusAllowed(next)) {
            throw new BusinessException("活动审核状态不合法");
        }
        if (next == ActivityStatus.CLOSED && activity.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("活动未到结束时间，暂不能确认结束");
        }
        activity.setStatus(next);
        Activity saved = activityRepository.save(activity);
        if (saved.getCreatorRole() == RoleType.STAFF) {
            notificationService.create(
                    saved.getCreatorUserId(),
                    next == ActivityStatus.PUBLISHED ? "ACTIVITY_REVIEW_APPROVED" : "ACTIVITY_REVIEW_REJECTED",
                    next == ActivityStatus.PUBLISHED ? "活动审核已通过" : "活动审核结果已更新",
                    next == ActivityStatus.PUBLISHED
                            ? "你发起的活动“" + saved.getTitle() + "”已审核通过并发布。"
                            : "你发起的活动“" + saved.getTitle() + "”审核结果为：" + next.name(),
                    "ACTIVITY",
                    saved.getId(),
                    "/activities"
            );
        }
        return saved;
    }

    /**
     * 在活动结束或取消后保存活动总结内容。
     */
    public Activity submitSummary(Long id, Long operatorUserId, boolean adminMode, ActivitySummaryRequest request) {
        Activity activity = requireActivity(id);
        if (!adminMode && !operatorUserId.equals(activity.getCreatorUserId())) {
            throw new BusinessException("只有活动发起人才能提交活动总结");
        }
        if (activity.getStatus() != ActivityStatus.CLOSED && activity.getStatus() != ActivityStatus.CANCELLED) {
            throw new BusinessException("活动结束后才能提交总结");
        }
        activity.setSummaryContent(request.summaryContent().trim());
        activity.setSummarySubmittedAt(LocalDateTime.now());
        Activity saved = activityRepository.save(activity);
        if (!adminMode) {
            notificationService.notifyAdmins(
                    "ACTIVITY_SUMMARY_SUBMITTED",
                    "活动总结已提交",
                    "活动“" + saved.getTitle() + "”的发起人已提交活动总结。",
                    "ACTIVITY",
                    saved.getId(),
                    "/activities"
            );
        }
        return saved;
    }

    /**
     * 为用户报名已发布活动，并发送报名成功通知。
     */
    public ActivityRegistration register(ActivityRegistrationRequest request) {
        Activity activity = requireActivity(request.activityId());
        if (activity.getStatus() != ActivityStatus.PUBLISHED || !activity.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("只能报名未结束且已审核通过的活动");
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(request.activityId());
        registration.setUserId(request.userId());
        registration.setCancelled(false);
        ActivityRegistration saved = registrationRepository.save(registration);
        notificationService.create(
                request.userId(),
                "ACTIVITY_REGISTERED",
                "活动报名成功",
                "你已成功报名活动“" + activity.getTitle() + "”。",
                "ACTIVITY",
                activity.getId(),
                "/activities"
        );
        return saved;
    }

    public ActivityRegistration requireRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("报名记录不存在"));
    }

    /**
     * 取消一条报名记录而不是直接删除，便于后续追踪历史。
     */
    public ActivityRegistration cancelRegistration(Long id) {
        ActivityRegistration registration = requireRegistration(id);
        registration.setCancelled(true);
        ActivityRegistration saved = registrationRepository.save(registration);
        Activity activity = requireActivity(saved.getActivityId());
        notificationService.create(
                saved.getUserId(),
                "ACTIVITY_REGISTRATION_CANCELLED",
                "活动报名已取消",
                "你已取消活动“" + activity.getTitle() + "”的报名。",
                "ACTIVITY",
                activity.getId(),
                "/activities"
        );
        return saved;
    }

    public Page<ActivityRegistration> listRegistrations(Pageable pageable) {
        return registrationRepository.findAll(pageable);
    }

    public Page<ActivityRegistration> listUserRegistrations(Long userId, Pageable pageable) {
        return registrationRepository.findByUserId(userId, pageable);
    }

    private void apply(Activity activity, ActivityRequest request, boolean adminMode) {
        activity.setTitle(request.title());
        activity.setContent(request.content());
        activity.setStartTime(request.startTime());
        activity.setEndTime(request.endTime());
        activity.setCapacity(request.capacity());
        activity.setImageUrl(request.imageUrl());
        activity.setStatus(resolveStatus(request, adminMode));
    }

    private ActivityStatus resolveStatus(ActivityRequest request, boolean adminMode) {
        if (!adminMode) {
            return ActivityStatus.PENDING_REVIEW;
        }
        return ActivityStatus.valueOf(request.status().toUpperCase());
    }

    private boolean isReviewStatusAllowed(ActivityStatus status) {
        return status == ActivityStatus.PUBLISHED
                || status == ActivityStatus.REJECTED
                || status == ActivityStatus.CANCELLED
                || status == ActivityStatus.CLOSED;
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException("活动开始时间必须晚于当前时间");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("活动结束时间必须晚于开始时间");
        }
    }
}
