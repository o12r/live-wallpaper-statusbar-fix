# 动态壁纸状态栏修复 · Live Wallpaper Status Bar Fix

[中文](#中文) · [English](#english)

---

## 中文

一个 LSPosed / Xposed 模块：让状态栏（以及中兴/努比亚锁屏上的时钟和小组件）的深浅颜色**跟随动态壁纸自己报告的深色/浅色文字提示**（`WallpaperColors.HINT_SUPPORTS_DARK_TEXT`）。

### 为什么需要它

动态壁纸可以告诉系统"我的画面是亮的，请用深色文字"。原生 Android 和 Pixel 会照做，但有些厂商系统不理会这个提示。例如中兴 MiFavor：

- **桌面**：根据壁纸某一帧的截图判断颜色，并在回到桌面、重新加载时恢复成主题默认的白色；
- **锁屏**：动态壁纸一律用白色。

于是浅色视频壁纸上，状态栏、锁屏时钟和小组件常常是白字，看不清。这个模块让它们改为使用壁纸自己的提示。

[WE 创意工坊](https://github.com/o12r/wallpaper-engine-android) 的"WE 视频壁纸"会正确报告提示，可在其设置里选择 自动 / 深色 / 浅色。官方 Wallpaper Engine 等其他动态壁纸也会报告。

### 需要

- Android 12 或更高
- 已 root，并安装 LSPosed（或 Vector 等 LSPosed 分支）

### 安装

1. 从 [Releases](../../releases) 下载 APK 并安装。
2. 在 LSPosed 管理器里启用 **动态壁纸状态栏修复**，作用域勾选：
   - **系统桌面**（例如 `com.zte.mifavor.launcher`）——主屏幕状态栏
   - **系统界面**（`com.android.systemui`）——锁屏状态栏、时钟和小组件
3. 重启桌面和系统界面（或直接重启手机）。

### 适配情况

| 部分 | 适配范围 | 已测试 |
|---|---|---|
| 主屏幕状态栏 | 用 `View.setSystemUiVisibility` 设置状态栏的 Launcher3 系桌面（需在作用域里勾选对应桌面） | 中兴 MiFavor 桌面（红魔 NX809J，Android 16） |
| 锁屏状态栏、时钟、日期、小组件、快捷按钮 | 中兴/努比亚 MiFavor 系统界面 | 红魔 NX809J，Android 16 |

- 只处理**动态壁纸**；静态壁纸交给系统自己处理。
- 息屏显示（AOD）时不修改时钟颜色；如果你在锁屏样式里手动选了时钟颜色，也不会覆盖。
- 在其他系统上，不匹配的部分会安全地跳过，什么都不改。

### 征集更多厂商系统的适配 🙌

目前锁屏部分只适配了中兴/努比亚。如果你的手机（小米/HyperOS、三星 One UI、OPPO/一加 ColorOS、vivo OriginOS、华为/荣耀 等）也有"浅色动态壁纸上状态栏或锁屏看不清"的问题，欢迎帮忙适配：

- **报告**：开一个 [适配请求](../../issues/new?template=oem-support.md)，写明机型、系统版本、桌面包名，附上截图，说明哪里不对（桌面状态栏 / 锁屏状态栏 / 锁屏时钟或小组件）。
- **提供 APK**（最有用）：用 adb 导出桌面和系统界面的 APK，上传到 issue（或网盘链接）：
  ```bash
  adb shell pm path com.android.systemui
  adb shell cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN
  adb pull <上面得到的路径>
  ```
- **直接提交 PR**：找到该系统设置状态栏或锁屏颜色的方法（反编译后搜索 `setSystemUiVisibility`、`setSystemBarsAppearance`、`KeyguardStatusBar`、`ForeColor`、`DarkIcon` 等），在 [`LauncherStatusBarHook.java`](module/src/main/java/com/weworkshop/statusbarfix/LauncherStatusBarHook.java) 里加一个对应的 hook；找不到的类会被安全跳过。

### 构建

需要 JDK 21 和 Android SDK。Xposed API 用的是仓库里自带的编译期存根（`stubs/`），不需要额外下载。

```bash
./gradlew :module:assembleRelease
```

没有 `keystore.properties` 时，会用调试密钥签名。

### 许可

[MIT](LICENSE)

---

## English

An LSPosed / Xposed module that makes the status bar (and, on ZTE/nubia, the lock screen clock and widgets) **follow a live wallpaper's own dark/light text hint** (`WallpaperColors.HINT_SUPPORTS_DARK_TEXT`).

### Why

A live wallpaper can tell the system "I'm bright, use dark text". Stock Android and Pixel follow that, but some OEM systems ignore it. ZTE MiFavor, for example:

- **Launcher**: judges the color from a screenshot of one frame, and resets it to the theme's default white when you return home or the workspace reloads;
- **Lock screen**: always uses white for live wallpapers.

So on a light video wallpaper the status bar, lock screen clock and widgets often end up white and unreadable. This module makes them use the wallpaper's own hint instead.

"WE Video Wallpaper" from [WE Workshop](https://github.com/o12r/wallpaper-engine-android) reports the hint correctly (choose Auto / Dark / Light in its settings). Other live wallpapers, such as the official Wallpaper Engine app, report it too.

### Requirements

- Android 12 or later
- Root, with LSPosed (or a fork such as Vector)

### Install

1. Download the APK from [Releases](../../releases) and install it.
2. In the LSPosed manager, enable **Live Wallpaper Status Bar Fix** with this scope:
   - **the system launcher** (e.g. `com.zte.mifavor.launcher`): home screen status bar
   - **System UI** (`com.android.systemui`): lock screen status bar, clock and widgets
3. Restart the launcher and System UI (or reboot).

### Support

| Part | Works with | Tested on |
|---|---|---|
| Home screen status bar | Launcher3-based launchers that set the status bar with `View.setSystemUiVisibility` (tick the launcher in the scope) | ZTE MiFavor launcher (RedMagic NX809J, Android 16) |
| Lock screen status bar, clock, date, widgets, shortcuts | ZTE/nubia MiFavor System UI | RedMagic NX809J, Android 16 |

- Only **live** wallpapers are changed; static wallpapers are left to the system.
- The clock isn't recolored on the always-on display, nor when you picked a clock color in the lock screen style settings.
- On other systems, parts that don't match are skipped safely and change nothing.

### Help add more OEM systems 🙌

The lock screen part currently supports ZTE/nubia only. If your phone (Xiaomi/HyperOS, Samsung One UI, OPPO/OnePlus ColorOS, vivo OriginOS, Huawei/Honor, …) also has unreadable status bar or lock screen text on light live wallpapers, please help:

- **Report it**: open an [OEM support request](../../issues/new?template=oem-support.md) with your model, OS version, launcher package, screenshots, and what's wrong (home status bar / lock screen status bar / lock screen clock or widgets).
- **Share APKs** (most useful): pull your launcher and System UI APKs with adb and attach them (or a download link):
  ```bash
  adb shell pm path com.android.systemui
  adb shell cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN
  adb pull <path from above>
  ```
- **Send a PR**: find where that system sets the status bar or lock screen colors (decompile and search for `setSystemUiVisibility`, `setSystemBarsAppearance`, `KeyguardStatusBar`, `ForeColor`, `DarkIcon`, …) and add a hook in [`LauncherStatusBarHook.java`](module/src/main/java/com/weworkshop/statusbarfix/LauncherStatusBarHook.java). Classes that don't exist are skipped safely.

### Build

Requires JDK 21 and the Android SDK. The Xposed API comes from compile-only stubs in this repo (`stubs/`), so nothing extra is downloaded.

```bash
./gradlew :module:assembleRelease
```

Without a `keystore.properties`, release builds are signed with the debug key.

### License

[MIT](LICENSE)
