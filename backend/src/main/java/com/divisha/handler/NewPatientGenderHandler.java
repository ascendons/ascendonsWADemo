package com.divisha.handler;

import com.divisha.enums.ConversationState;
import com.divisha.enums.Gender;
import com.divisha.model.Location;
import com.divisha.model.Patient;
import com.divisha.model.RegisterPatient;
import com.divisha.service.*;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewPatientGenderHandler implements ConversationHandler {

  private final HandlerRegistry registry;
  private final ConversationContextStore context;
  private final MetaMessageSender sender;
  private final MetaMessageBuilder builder;
  private final PatientService patientService;
  private final LocationService locationService;

  @PostConstruct
  public void init() {
    registry.register(this);
  }

  @Override
  public ConversationState state() {
    return ConversationState.ASK_GENDER;
  }

  @Override
  public void handle(
      String phone,
      String text,
      String listReply,
      String buttonReply,
      Map<String, Object> payload,
      boolean isOwner) {

    if (isOwner) {
      return;
    }

    //    if (!isValidGender(text)) {
    //      sender.text(phone, "❗ Please enter a valid gender:\n*Male / Female / Other*");
    //      return;
    //    }

    context.setUserGender(phone, text.trim());
    RegisterPatient req = buildRegisterRequest(phone);

    try {
      Patient registered = patientService.createPatient(req);
      context.setSelectedPatientId(phone, registered.getPatientId());

      sender.text(
          phone,
          "✅ *Registration Successful!*\nYour Patient ID: *" + registered.getPatientId() + "*");

      proceedToLocationSelection(phone);

    } catch (Exception e) {
      sender.text(
          phone,
          "❗ *Registration failed:* "
              + e.getMessage()
              + "\n\n👉 Please enter your *full name* again.");
      context.setState(phone, ConversationState.ASK_NAME);
    }
  }

  private boolean isValidGender(String input) {
    if (input == null) return false;

    String g = input.trim().toLowerCase();
    return g.equals("male") || g.equals("female") || g.equals("other");
  }

  private RegisterPatient buildRegisterRequest(String phone) {
    return RegisterPatient.builder()
        .phone(phone)
        .name(context.getUserName(phone))
        .age(context.getUserAge(phone))
        .gender(Gender.from(context.getUserGender(phone)))
        .build();
  }

  private void proceedToLocationSelection(String phone) {

    List<Location> locations = locationService.getAllActiveLocation();

    if (locations.isEmpty()) {
      sender.text(
          phone,
          "🙏 Thank you!\nWe are currently not available in your area. "
              + "We will notify you once we launch.");
      context.clearState(phone);
      return;
    }
    sender.sendButtons(phone, builder.locationMenu(locations));
    context.setState(phone, ConversationState.ASK_DOCTOR);
  }
}
