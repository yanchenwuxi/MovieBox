# MovieBox

轻量视频聚合客户端（TVBox 配置 / 苹果 CMS）。

## Phase 3 — 体验完善

- 手势：左亮度 / 右音量 / 横向快进快退 / 长按临时倍速
- 播完自动下一集（可关）
- 音轨 / 字幕选择（Exo 内核）
- 历史记录显示「看到第 X 集 · 进度」

## Phase 2 — 容错播放

- 系统播放器内核 + AUTO 下 Exo→系统容错
- 失败重试、MediaUrlHelper 直链判断

## Phase 1 — 播放器架构

- `IKernel` / `MoviePlayer` / `ExoKernel`

## 构建

```bash
./gradlew assembleDebug
```

Push 到 `main` 触发 GitHub Actions。

包名：`com.zaka.moviebox`
