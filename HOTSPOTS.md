# 热点采集与归档

## 数据规则

- 数据保存在项目工作目录的 `data/YYYY.json`，每个日历年一个 JSON 文件。`data/` 已加入 `.gitignore`。
- 每天按本地配置时区分成 12 个时段：00、02、04……22 点。每个平台每个时段最多保存 30 条，批次由 `platform` 区分。
- 定时任务在每个偶数整点分别抓取知乎和贴吧，并替换对应平台、同一天、同一时段的旧批次。
- 手动抓取写入下一个偶数整点对应的时段。例如 05 点抓取会先写入当天 06 点档；06 点定时任务执行后会覆盖它。22 点之后手动抓取写入次日 00 点档。
- 每条数据保存标题、热度、问题/话题 ID、回答/帖子数、链接和榜单名次。平台接口未返回的字段会留空。

## 抓取配置

默认使用 `Asia/Tokyo` 时区，数据目录默认是 `./data`。可在 `application.properties` 中调整 `zhihu.time-zone` 和 `zhihu.data-dir`。

如果知乎拒绝匿名请求，可在启动应用前设置环境变量 `ZHIHU_COOKIE` 为本机浏览器获取的知乎 Cookie；不要把 Cookie 提交到代码仓库。

## API

- `GET /api/hotspots?date=YYYY-MM-DD&platform=zhihu|tieba`：读取某平台某一天的已保存批次。
- `GET /api/hotspots/slot?date=YYYY-MM-DD&hour=HH&platform=zhihu|tieba`：读取一个两小时档，`hour` 为 0 到 22 的偶数。
- `POST /api/hotspots/crawl?platform=zhihu|tieba`：抓取指定平台并写入下一个时段，省略 `platform` 时默认为知乎。
