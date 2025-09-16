package com.raman.soccer_backend;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scores")
@CrossOrigin(origins = "http://localhost:3000") // allow React dev server
public class ScoresController {

  private final ScoresService svc;

  public ScoresController(ScoresService svc) {
    this.svc = svc;
  }

  @GetMapping("/today")
  public List<Map<String, Object>> today(
      @RequestParam(defaultValue = "false") boolean live,
      @RequestParam(defaultValue = "false") boolean force
  ) {
    return svc.fetchTodayBig5(live, force);
  }
}
