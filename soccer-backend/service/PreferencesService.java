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
                .region(Region.US_EAST_1) // adjust to your AWS region
                .build();
    }

    public void savePref(String userId, String prefType, String valueName) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("prefType", AttributeValue.builder().s(prefType).build());
        item.put("valueName", AttributeValue.builder().s(valueName).build());

        dynamo.putItem(PutItemRequest.builder()
                .tableName(table)
                .item(item)
                .build());
    }

    public List<Map<String, String>> getPrefs(String userId) {
        QueryRequest req = QueryRequest.builder()
                .tableName(table)
                .keyConditionExpression("userId = :u")
                .expressionAttributeValues(Map.of(":u", AttributeValue.builder().s(userId).build()))
                .build();

        QueryResponse resp = dynamo.query(req);

        List<Map<String, String>> prefs = new ArrayList<>();
        for (Map<String, AttributeValue> i : resp.items()) {
            prefs.add(Map.of(
                    "prefType", i.get("prefType").s(),
                    "valueName", i.get("valueName").s()
            ));
        }
        return prefs;
    }
}
