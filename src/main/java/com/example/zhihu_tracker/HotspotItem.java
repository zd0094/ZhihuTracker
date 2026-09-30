package com.example.zhihu_tracker;

public record HotspotItem(
        int rank,
        String title,
        String heat,
        String questionId,
        Integer answerCount,
        String topicId,
        Integer postCount,
        String url
) {
}
