# MovieBox

轻量视频聚合客户端（TVBox 配置 / 苹果 CMS）。

## Phase 2 — 容错播放

- 系统播放器内核 `SystemKernel`（MediaPlayer + TextureView）
- 内核选项：自动(Exo→系统) / ExoPlayer / 系统播放器
- 播放失败：同内核重试 + AUTO 下自动切换系统内核
- `MediaUrlHelper` 增强直链判断
- IJK 需 native so，接口预留，默认不启用

## Phase 1 — 播放器架构

- 统一内核接口 `IKernel` + 门面 `MoviePlayer`
- 默认内核：Media3 ExoPlayer（`ExoKernel`）

## 本地构建

```bash
./gradlew assembleDebug
```

## CI

Push 到 `main` 后自动执行 GitHub Actions 构建。

## 包名

`com.zaka.moviebox`
