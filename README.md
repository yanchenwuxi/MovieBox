# MovieBox

轻量视频聚合客户端（TVBox 配置 / 苹果 CMS）。

## Phase 1 — 播放器架构升级

- 统一内核接口 `IKernel` + 门面 `MoviePlayer`
- 默认内核：Media3 ExoPlayer（`ExoKernel`）
- 设置页支持选择：自动 / ExoPlayer
- 为阶段2（IJK / 系统内核）预留工厂扩展点

## 本地构建

```bash
./gradlew assembleDebug
```

## CI

Push 到 `main` 后自动执行 GitHub Actions 构建，成功可在 Artifacts 下载 `app-debug.apk`。

## 包名

`com.zaka.moviebox`
