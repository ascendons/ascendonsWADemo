package com.divisha.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {

  @GetMapping(
      value = {"/{path:[^\\.]*}", "/**/{path:[^\\.]*}"},
      produces = "text/html")
  public String forwardToIndex() {
    return "forward:/index.html";
  }
}
