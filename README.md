# my-pixel-9-pro-ims

Pixel 9 Pro 的 VoNR / IMS carrier config 覆写模块（LSPosed）。

## 它做什么

在 `com.android.phone` 进程内拦截 `CarrierConfigLoader.getConfigForSubIdWithFeature`，
把几个 IMS 相关 key 合并进它返回的 `PersistableBundle`。

**当前阶段（Stage C）：只做 carrier config 覆写，不碰 IMS provisioning。**

覆写的 key：

| key | 值 |
|---|---|
| `vonr_enabled_bool` | `true` |
| `vonr_setting_visibility_bool` | `true` |
| `carrier_volte_available_bool` | `true` |
| `carrier_vt_available_bool` | `true` |
| `carrier_supports_ss_over_ut_bool` | `true` |
| `show_ims_registration_status_bool` | `true` |

## 为什么是「读取侧覆写」

`CarrierConfigManager.overrideConfig()` 需要 `MODIFY_PHONE_STATE`，在 Android 17 的
Pixel 上 `persistent=true` 已经写不进去，每次开机都得重写。

本模块改拦**读取**一侧：读取方本就在权限检查之内，因此不需要借任何权限，也不需要
Shizuku。模块不写任何磁盘状态——`dumpsys carrier_config` 看到的仍是运营商原值，
而每一个 `CarrierConfigManager` 调用方拿到的都是覆写后的值。

做法沿用 [NKU100/carrier-ims](https://github.com/NKU100/carrier-ims)（Apache-2.0）。

## 要求

- LSPosed v2.2.0+（模块用 libxposed API 102）
- KernelSU / Magisk + Zygisk（本机为 zygisk-next + zygisk_lsposed v2.2.1）

## 安装

1. 从 Actions 页面下载 `app-release` artifact，解压得到 APK
2. 安装，在 LSPosed 里启用
3. 作用域固定为 `com.android.phone`
4. 重启 phone 进程，或重启手机

## 验证

读取侧改值不会出现在 `dumpsys carrier_config` 里。用这两个观察点：

```sh
# 1. NR 语音能力标记
dumpsys telephony.registry | grep -oE "getRilVoiceRadioTechnology=[0-9]+\([A-Za-z]+\)" | tail

# 2. IMS 注册状态
dumpsys telephony.registry | grep -i "ImsRegistration"
```

## 编译

云端：push 到 `main` 即触发 GitHub Actions。

本地：`gradle :app:assembleRelease`（需 JDK 21 + Android SDK platform 36）。

## 许可

Apache-2.0。carrier config 的取值思路来自 NKU100/carrier-ims。

## 签名

`.ci/keystore.b64` 是一份固定的 PKCS12 keystore（密码 `android`，别名 `pixel9proims`），
由 [`openssl`](https://www.openssl.org/) 生成并提交在仓库里，CI 每次构建都用它签名。

这样做是为了让每次构建的签名一致，否则 Android 会因为签名不同而拒绝覆盖安装。

> ⚠️ 仓库是公开的，这份 keystore 也随之公开。对个人自用的侧载模块可以接受；
> 若在意，可改为放进 GitHub Secrets（`KEYSTORE_B64`）并从 workflow 读取。

首次从「每次重新生成 keystore」的旧构建升级时，需要先卸载旧版再装新版；
之后就都能直接覆盖安装了。
