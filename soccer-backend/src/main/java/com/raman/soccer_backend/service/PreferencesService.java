package com.raman.soccer_backend.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

@Service
public class PreferencesService {

    private final DynamoDbClient dynamoDb;
    private static final String TABLE = "UserPreferences"; // your table name

    public PreferencesService(DynamoDbClient dynamoDb) {
        this.dynamoDb = dynamoDb;
    }

    // Get all prefs for a user
    public List<Map<String, String>> getPrefs(String userId) {
        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE)
                .keyConditionExpression("userId = :uid")
                .expressionAttributeValues(Map.of(
                        ":uid", AttributeValue.builder().s(userId).build()
                ))
                .build();

        QueryResponse response = dynamoDb.query(request);

        List<Map<String, String>> prefs = new ArrayList<>();
        for (Map<String, AttributeValue> item : response.items()) {
            Map<String, String> pref = new HashMap<>();
            pref.put("prefType", item.get("prefType").s());
            pref.put("valueName", item.get("valueName").s());
            prefs.add(pref);
        }
        return prefs;
    }

    // Save or overwrite a pref
    public void savePref(String userId, String prefType, String valueName) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("prefType", AttributeValue.builder().s(prefType).build());
        item.put("valueName", AttributeValue.builder().s(valueName).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE)
                .item(item)
                .build();

        dynamoDb.putItem(request);
    }

    // Delete all prefs for a user (TEAM + LEAGUE rows)
    public void deletePrefs(String userId) {
        // Query all prefTypes for this user
        QueryRequest query = QueryRequest.builder()
                .tableName(TABLE)
                .keyConditionExpression("userId = :uid")
                .expressionAttributeValues(Map.of(
                        ":uid", AttributeValue.builder().s(userId).build()
                ))
                .build();

        QueryResponse response = dynamoDb.query(query);

        // Delete each (userId, prefType) pair
        for (Map<String, AttributeValue> item : response.items()) {
            String prefType = item.get("prefType").s();

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("userId", AttributeValue.builder().s(userId).build());
            key.put("prefType", AttributeValue.builder().s(prefType).build());

            DeleteItemRequest delete = DeleteItemRequest.builder()
                    .tableName(TABLE)
                    .key(key)
                    .build();

            dynamoDb.deleteItem(delete);
        }
    }
}
