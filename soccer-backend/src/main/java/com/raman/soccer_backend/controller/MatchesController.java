@RestController
@RequestMapping("/api/scores")
public class MatchesController {

    private final ScoresService scores;

    public MatchesController(ScoresService scores) {
        this.scores = scores;
    }

    @GetMapping("/fixtures")
    public List<Map<String, Object>> todayMatches() {
        return scores.getTodayMatches();
    }

    @GetMapping("/fixtures/events")
    public List<Map<String, Object>> getEvents(@RequestParam("fixture") String matchId) {
        return scores.getMatchEvents(matchId);
    }
}
