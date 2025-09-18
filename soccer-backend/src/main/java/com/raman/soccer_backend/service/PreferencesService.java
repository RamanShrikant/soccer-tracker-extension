package com.raman.soccer_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

@Service
public class PreferencesService {

    private final DynamoDbClient dynamo;

    @Value("${aws.dynamo.table:UserPreferences}")
    private String table;

    public PreferencesService() {
        this.dynamo = DynamoDbClient.builder()
                .region(Region.US_EAST_2) // ⚡ adjust this to your AWS region
                .build();
    }

    public void savePref(String userId, String prefType, String valueName) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("prefType", AttributeValue.builder().s(prefType).build());
        item.put("valueName", AttributeValue.builder().s(valueName).build());

        System.out.println("🔎 Saving pref: " + item);

        try {
            dynamo.putItem(PutItemRequest.builder()
                    .tableName(table)
                    .item(item)
                    .build());
            System.out.println("✅ Successfully saved preference to DynamoDB");
        } catch (Exception e) {
            System.err.println("❌ Error saving pref: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Map<String, String>> getPrefs(String userId) {
        QueryRequest req = QueryRequest.builder()
                .tableName(table)
                .keyConditionExpression("userId = :u")
                .expressionAttributeValues(Map.of(":u", AttributeValue.builder().s(userId).build()))
                .build();

        try {
            QueryResponse resp = dynamo.query(req);
            System.out.println("🔎 Fetching prefs for userId=" + userId + ", found " + resp.count() + " items");

            List<Map<String, String>> prefs = new ArrayList<>();
            for (Map<String, AttributeValue> i : resp.items()) {
                prefs.add(Map.of(
                        "prefType", i.get("prefType").s(),
                        "valueName", i.get("valueName").s()
                ));
            }
            return prefs;
        } catch (Exception e) {
            System.err.println("❌ Error fetching prefs: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
