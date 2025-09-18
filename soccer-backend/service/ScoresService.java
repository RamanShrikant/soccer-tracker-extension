public List<Map<String, Object>> getTodayMatches() {
    String today = LocalDate.now().toString();

    try {
        JsonNode root = client.get()
                .uri("/matches?competitions=PL,PD,SA,BL1,FL1,CL&dateFrom=" + today + "&dateTo=" + today)
                .retrieve()
                .body(JsonNode.class);

        System.out.println("✅ Querying matches for: " + today);
        System.out.println("Football-Data API response: " + root.toPrettyString());

        List<Map<String, Object>> matches = new ArrayList<>();

        for (JsonNode m : root.path("matches")) {
            Map<String, Object> match = new HashMap<>();
            match.put("id", m.path("id").asText());
            match.put("league", m.path("competition").path("name").asText());
            match.put("kickoffIso", m.path("utcDate").asText());

            Map<String, Object> home = new HashMap<>();
            home.put("name", m.path("homeTeam").path("name").asText());
            home.put("logo", m.path("homeTeam").path("crest").asText(null));
            home.put("score", m.path("score").path("fullTime").path("home").isInt()
                    ? m.path("score").path("fullTime").path("home").asInt()
                    : null);

            Map<String, Object> away = new HashMap<>();
            away.put("name", m.path("awayTeam").path("name").asText());
            away.put("logo", m.path("awayTeam").path("crest").asText(null));
            away.put("score", m.path("score").path("fullTime").path("away").isInt()
                    ? m.path("score").path("fullTime").path("away").asInt()
                    : null);

            match.put("home", home);
            match.put("away", away);

            matches.add(match);
        }

        System.out.println("✅ Found " + matches.size() + " matches");
        return matches;

    } catch (Exception e) {
        System.err.println("❌ Error fetching matches: " + e.getMessage());
        e.printStackTrace();
        return Collections.emptyList();
    }
}
