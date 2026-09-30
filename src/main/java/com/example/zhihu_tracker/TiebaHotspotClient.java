package com.example.zhihu_tracker;

import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class TiebaHotspotClient {
    private static final String HOT_TOPIC_URL = "https://tieba.baidu.com/hottopic/browse/topicList";
    private final RestClient restClient = RestClient.builder()
            .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36")
            .defaultHeader("Referer", "https://tieba.baidu.com/")
            .build();

    public List<HotspotItem> fetchHotspots() {
        JsonNode response = restClient.get().uri(HOT_TOPIC_URL).retrieve().body(JsonNode.class);
        if (response == null) throw new IllegalStateException("贴吧热议接口没有返回数据");

        List<JsonNode> topics = new ArrayList<>();
        JsonNode topicList = response.path("data").path("bang_topic").path("topic_list");
        if (topicList.isArray()) topicList.forEach(topics::add);
        JsonNode cards = response.path("data").path("cards");
        if (topics.isEmpty() && cards.isArray()) {
            for (JsonNode card : cards) {
                JsonNode content = card.path("content");
                if (content.isArray()) content.forEach(topics::add);
            }
        }
        if (topics.isEmpty()) throw new IllegalStateException("贴吧热议接口返回了无法识别的数据格式");

        List<HotspotItem> result = new ArrayList<>();
        for (JsonNode topic : topics) {
            if (result.size() >= 30) break;
            String title = firstText(topic, "topic_name", "word", "title");
            if (title == null || title.isBlank()) continue;
            String topicId = firstText(topic, "topic_id", "topicId", "id");
            String url = firstText(topic, "topic_url", "url", "appUrl");
            if ((topicId == null || topicId.isBlank()) && url != null) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:topic_id|id)=([^&]+)").matcher(url);
                if (matcher.find()) topicId = matcher.group(1);
            }
            if ((url == null || url.isBlank()) && topicId != null) {
                url = "https://tieba.baidu.com/hottopic/browse/hottopic?topic_id=" + topicId;
            }
            String heat = firstText(topic, "hotScore", "hot_score", "heat", "hot", "topic_value");
            Integer postCount = integer(topic, "post_num", "post_count", "thread_num", "discussion_num", "reply_num");
            result.add(new HotspotItem(result.size() + 1, title, heat, null, null, topicId, postCount, url));
        }
        return result;
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) return value.asText();
        }
        return null;
    }

    private static Integer integer(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.canConvertToInt()) return value.asInt();
        }
        return null;
    }
}
