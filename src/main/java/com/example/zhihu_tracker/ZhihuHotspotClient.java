package com.example.zhihu_tracker;

import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class ZhihuHotspotClient {
    private static final String HOT_LIST_URL = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=30&desktop=true";
    private final RestClient restClient;

    public ZhihuHotspotClient(@Value("${zhihu.cookie:}") String cookie) {
        RestClient.Builder builder = RestClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36")
                .defaultHeader(HttpHeaders.REFERER, "https://www.zhihu.com/hot");
        if (!cookie.isBlank()) builder.defaultHeader(HttpHeaders.COOKIE, cookie);
        this.restClient = builder.build();
    }

    public List<HotspotItem> fetchHotspots() {
        JsonNode response = restClient.get().uri(HOT_LIST_URL).retrieve().body(JsonNode.class);
        JsonNode rows = response == null ? null : response.path("data");
        if (rows == null || !rows.isArray()) {
            throw new IllegalStateException("知乎热榜返回了无法识别的数据格式");
        }

        List<HotspotItem> result = new ArrayList<>();
        for (JsonNode row : rows) {
            if (result.size() >= 30) break;
            JsonNode target = row.path("target");
            String title = text(target, "title");
            if (title == null || title.isBlank()) continue;

            String id = text(target, "id");
            String targetUrl = text(target, "url");
            if ((id == null || id.isBlank()) && targetUrl != null) {
                int slash = targetUrl.lastIndexOf('/');
                id = slash >= 0 ? targetUrl.substring(slash + 1).split("\\?")[0] : targetUrl;
            }
            if (id != null && !id.matches("[0-9]+")) id = null;

            String heat = firstText(target, "heat", "hot", "hot_value", "detail_text");
            if (heat == null) heat = firstText(row, "detail_text");
            Integer answerCount = integer(target, "answer_count", "answerCount");
            String url = id == null ? targetUrl : "https://www.zhihu.com/question/" + id;
            result.add(new HotspotItem(result.size() + 1, title, heat, id, answerCount, null, null, url));
        }
        return result;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) return value;
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
