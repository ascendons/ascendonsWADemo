package com.divisha.handler;

import com.divisha.enums.ConversationState;
import com.divisha.model.DoctorSchedule;
import com.divisha.model.Location;
import com.divisha.service.*;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SelectLocationHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final MetaMessageBuilder builder;
  private final LocationService locationService;
  private final DoctorScheduleService doctorSchedule;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.ASK_DOCTOR;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {
    String selectedLocation = context.getSelectedLocation(phone);

    if (selectedLocation == null) {
      String m = buttonReply.trim().toUpperCase(Locale.ROOT);
      List<Location> allActiveLocation = locationService.getAllActiveLocation();
      for (Location location : allActiveLocation) {
        if (location.getName().toUpperCase().equals(m)) {
          String loc = capitalize(m.toLowerCase(Locale.ROOT));
          context.setSelectedLocation(phone, loc);
          List<DoctorSchedule> allDoctorAvailableInThisLocation =
              doctorSchedule.getAllDoctorAvailableInThisLocation(location.getId());
          if (allDoctorAvailableInThisLocation.isEmpty()) {
            sender.text(phone, "Doctor is Unavailable at this location");
            context.clearState(phone);
          } else {
            context.setState(phone, ConversationState.ASK_DATE);
            if (allDoctorAvailableInThisLocation.size() == 1) {
              ConversationState state = context.getState(phone);
              ConversationHandler handler = registry.get(state);
              handler.handle(
                  phone,
                  "",
                  "DOC_" + allDoctorAvailableInThisLocation.get(0).getDoctorId(),
                  "",
                  null,
                  true);
            } else {
              sender.sendList(phone, builder.availableDoctorList(allDoctorAvailableInThisLocation));
            }
          }
        }
      }
    }
    //    if (text == null) return;
    //    String m = text.trim().toUpperCase(Locale.ROOT);
    //    if ("BANGALORE".equals(m) || "MUMBAI".equals(m) || "DELHI".equals(m)) {
    //      String loc = capitalize(m.toLowerCase(Locale.ROOT));
    //      context.setSelectedLocation(phone, loc);
    //      sender.buttons(phone, builder.doctorList(loc));
    //      context.setState(phone, ConversationState.ASK_DATE);
    //    }
  }

  private String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return Character.toUpperCase(s.charAt(0)) + (s.length() > 1 ? s.substring(1) : "");
  }
}
