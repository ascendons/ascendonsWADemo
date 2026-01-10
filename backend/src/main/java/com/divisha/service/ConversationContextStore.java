package com.divisha.service;

import com.divisha.enums.ConversationState;
import com.divisha.model.DoctorSchedule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Conversation context store with automatic expiration after configured timeout.
 *
 * <p>Uses Caffeine Cache to automatically clean up inactive user sessions, preventing memory leaks
 * and ensuring users start fresh after timeout.
 */
@Slf4j
@Component
public class ConversationContextStore {

  @Value("${divisha.conversation.timeout.minutes:30}")
  private int timeoutMinutes;

  private Cache<String, ConversationState> userStates;
  private Cache<String, String> userSelectedDate;
  private Cache<String, String> userSelectedHourSlot;
  private Cache<String, String> userSelectedDateRange;
  private Cache<String, String> userSelectedDoctor;
  private Cache<String, String> userSelectedLocation;
  private Cache<String, String> userSelectedBookingId;
  private Cache<String, Boolean> userEnabledCancellation;
  private Cache<String, String> userCurrentFlow;
  private Cache<String, String> userFlowId;
  private Cache<String, String> userFlowToken;
  private Cache<String, Map<String, Object>> userFlowData;
  private Cache<String, String> userName;
  private Cache<String, Integer> userAge;
  private Cache<String, String> userGender;
  private Cache<String, String> userPatientId;
  private Cache<String, DoctorSchedule> doctorScheduleMap;
  private Cache<String, String> reScheduleBookingMap;

  @PostConstruct
  public void init() {
    Duration expirationDuration = Duration.ofMinutes(timeoutMinutes);
    log.info("🚀 Initializing ConversationContextStore with {}min timeout", timeoutMinutes);

    userStates =
        Caffeine.newBuilder()
            .expireAfterAccess(expirationDuration)
            .removalListener(
                (key, value, cause) ->
                    log.debug(
                        "🧹 Cleared conversation state for phone: {} (reason: {})", key, cause))
            .build();

    userSelectedDate = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userSelectedHourSlot = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userSelectedDateRange = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userSelectedDoctor = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userSelectedLocation = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userSelectedBookingId = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userEnabledCancellation = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userCurrentFlow = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userFlowId = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userFlowToken = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userFlowData = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userName = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userAge = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userGender = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    userPatientId = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    doctorScheduleMap = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
    reScheduleBookingMap = Caffeine.newBuilder().expireAfterAccess(expirationDuration).build();
  }

  // -------------------------------------------------------------------------
  // CANCELLATION
  // -------------------------------------------------------------------------

  public boolean isCancellationEnabled(String phone) {
    Boolean enabled = userEnabledCancellation.getIfPresent(phone);
    return enabled != null && enabled;
  }

  public void setUserEnabledCancellation(String phone, boolean cancel) {
    userEnabledCancellation.put(phone, cancel);
  }

  // -------------------------------------------------------------------------
  // CONVERSATION STATE
  // -------------------------------------------------------------------------

  public ConversationState getState(String phone) {
    ConversationState state = userStates.getIfPresent(phone);
    return state != null ? state : ConversationState.START;
  }

  public void setState(String phone, ConversationState state) {
    userStates.put(phone, state);
  }

  /**
   * Manually clear all context for a user. Note: Caffeine will also auto-clear after 1 hour of
   * inactivity.
   */
  public void clearState(String phone) {
    log.info("🧹 Manually clearing conversation context for phone: {}", phone);
    userStates.invalidate(phone);
    userSelectedDate.invalidate(phone);
    userSelectedHourSlot.invalidate(phone);
    userSelectedDateRange.invalidate(phone);
    userSelectedDoctor.invalidate(phone);
    userSelectedLocation.invalidate(phone);
    userPatientId.invalidate(phone);
    userGender.invalidate(phone);
    userAge.invalidate(phone);
    userName.invalidate(phone);
    userCurrentFlow.invalidate(phone);
    userSelectedBookingId.invalidate(phone);
    userFlowId.invalidate(phone);
    userFlowToken.invalidate(phone);
    userFlowData.invalidate(phone);
    userPatientId.invalidate(phone);
    doctorScheduleMap.invalidate(phone);
    reScheduleBookingMap.invalidate(phone);
    userEnabledCancellation.invalidate(phone);
  }

  // -------------------------------------------------------------------------
  // DATE SELECTION
  // -------------------------------------------------------------------------

  public void setSelectedDate(String phone, String date) {
    userSelectedDate.put(phone, date);
  }

  public String getSelectedDate(String phone) {
    return userSelectedDate.getIfPresent(phone);
  }

  // -------------------------------------------------------------------------
  // TIME SLOT SELECTION
  // -------------------------------------------------------------------------

  public void setSelectedHourSlot(String phone, String slot) {
    userSelectedHourSlot.put(phone, slot);
  }

  public String getSelectedHourSlot(String phone) {
    return userSelectedHourSlot.getIfPresent(phone);
  }

  // -------------------------------------------------------------------------
  // DATE RANGE SELECTION
  // -------------------------------------------------------------------------

  public void setSelectedDateRange(String phone, String range) {
    userSelectedDateRange.put(phone, range);
  }

  public String getSelectedDateRange(String phone) {
    return userSelectedDateRange.getIfPresent(phone);
  }

  // -------------------------------------------------------------------------
  // DOCTOR SELECTION
  // -------------------------------------------------------------------------

  public void setSelectedDoctor(String phone, String doctor) {
    userSelectedDoctor.put(phone, doctor);
  }

  public String getSelectedDoctor(String phone) {
    return userSelectedDoctor.getIfPresent(phone);
  }

  // -------------------------------------------------------------------------
  // LOCATION SELECTION
  // -------------------------------------------------------------------------

  public void setSelectedLocation(String phone, String loc) {
    userSelectedLocation.put(phone, loc);
  }

  public String getSelectedLocation(String phone) {
    return userSelectedLocation.getIfPresent(phone);
  }

  // -------------------------------------------------------------------------
  // USER PROFILE (NAME, AGE, GENDER)
  // -------------------------------------------------------------------------

  public void setUserName(String phone, String name) {
    userName.put(phone, name);
  }

  public String getUserName(String phone) {
    return userName.getIfPresent(phone);
  }

  public void setUserAge(String phone, Integer age) {
    userAge.put(phone, age);
  }

  public Integer getUserAge(String phone) {
    return userAge.getIfPresent(phone);
  }

  public void setUserGender(String phone, String gender) {
    userGender.put(phone, gender);
  }

  public String getUserGender(String phone) {
    return userGender.getIfPresent(phone);
  }

  // -------------------------------------------------------------------------
  // RESCHEDULE BOOKING
  // -------------------------------------------------------------------------

  public void setReScheduleBookingId(String phone, String bookingId) {
    reScheduleBookingMap.put(phone, bookingId);
  }

  public String getRescheduledBookingId(String phone) {
    return reScheduleBookingMap.getIfPresent(phone);
  }

  // -------------------------------------------------------------------------
  // PATIENT ID
  // -------------------------------------------------------------------------

  public void setSelectedPatientId(String phone, String id) {
    userPatientId.put(phone, id);
  }

  public String getSelectedPatientId(String phone) {
    return userPatientId.getIfPresent(phone);
  }

  // -------------------------------------------------------------------------
  // DOCTOR SCHEDULE
  // -------------------------------------------------------------------------

  public void setDoctorSchedule(String phone, DoctorSchedule doctorSchedule) {
    doctorScheduleMap.put(phone, doctorSchedule);
  }

  public DoctorSchedule getDoctorSchedule(String phone) {
    return doctorScheduleMap.getIfPresent(phone);
  }

  // -------------------------------------------------------------------------
  // BOOKING ID
  // -------------------------------------------------------------------------

  public String getUserSelectedBookingId(String phone) {
    return userSelectedBookingId.getIfPresent(phone);
  }

  public void setUserSelectedBookingId(String phone, String bookingId) {
    userSelectedBookingId.put(phone, bookingId);
  }

  // -------------------------------------------------------------------------
  // FLOW TRACKING
  // -------------------------------------------------------------------------

  public String getUserCurrentFlow(String phone) {
    return userCurrentFlow.getIfPresent(phone);
  }

  public void setUserCurrentFlow(String phone, String currentFlow) {
    userCurrentFlow.put(phone, currentFlow);
  }

  public void setFlowId(String phone, String flowId) {
    userFlowId.put(phone, flowId);
  }

  public String getFlowId(String phone) {
    return userFlowId.getIfPresent(phone);
  }

  public void setFlowToken(String phone, String flowToken) {
    userFlowToken.put(phone, flowToken);
  }

  public String getFlowToken(String phone) {
    return userFlowToken.getIfPresent(phone);
  }

  public void setFlowData(String phone, Map<String, Object> data) {
    userFlowData.put(phone, data);
  }

  public Map<String, Object> getFlowData(String phone) {
    return userFlowData.getIfPresent(phone);
  }

  // -------------------------------------------------------------------------
  // DUPLICATE METHODS (kept for backward compatibility)
  // -------------------------------------------------------------------------

  public void setName(String phone, String name) {
    userName.put(phone, name);
  }

  public String getName(String phone) {
    return userName.getIfPresent(phone);
  }

  public void setAge(String phone, Integer age) {
    userAge.put(phone, age);
  }

  public Integer getAge(String phone) {
    return userAge.getIfPresent(phone);
  }

  public void setGender(String phone, String gender) {
    userGender.put(phone, gender);
  }

  public String getGender(String phone) {
    return userGender.getIfPresent(phone);
  }

  public void setLocation(String phone, String locationId) {
    userSelectedLocation.put(phone, locationId);
  }

  public String getLocation(String phone) {
    return userSelectedLocation.getIfPresent(phone);
  }

  public void setDoctor(String phone, String doctorId) {
    userSelectedDoctor.put(phone, doctorId);
  }

  public String getDoctor(String phone) {
    return userSelectedDoctor.getIfPresent(phone);
  }

  public void setDate(String phone, String date) {
    userSelectedDate.put(phone, date);
  }

  public String getDate(String phone) {
    return userSelectedDate.getIfPresent(phone);
  }

  public void setTimeSelected(String phone, String time) {
    userSelectedHourSlot.put(phone, time);
  }

  public String getTimeSelected(String phone) {
    return userSelectedHourSlot.getIfPresent(phone);
  }

  // -------------------------------------------------------------------------
  // CACHE STATS (for monitoring)
  // -------------------------------------------------------------------------

  /**
   * Get cache statistics for monitoring
   *
   * @return Stats about cache usage
   */
  public String getCacheStats() {
    return String.format(
        "ConversationContextStore Stats - Active users: %d, States: %d",
        userStates.estimatedSize(), userStates.estimatedSize());
  }
}
