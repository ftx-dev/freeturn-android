<div align="center">

[![Release](https://img.shields.io/github/v/release/ftx-dev/freeturn-android?label=FreeTurnX&logo=github)](https://github.com/ftx-dev/freeturn-android/releases/latest)
[![Core](https://img.shields.io/badge/Core-free--turn--proxy-blue?logo=github&logoColor=white)](https://github.com/hackdiaz-dev/free-turn-proxy)
![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white)
![Material 3](https://img.shields.io/badge/Material-3-757575?logo=materialdesign&logoColor=white)
![License](https://img.shields.io/badge/license-GPL--3.0-blue)
</div>

![FreeTurnX для Android: подключение, серверы, раздельное туннелирование и настройка](freeturnx-banner.png)

**FreeTurnX** - клиент на основе [FreeTurn для Android](https://github.com/hackdiaz-dev/turn-proxy-android): всё из оригинала плюс собственные функции в разделе настроек **«FreeTurnX: Фичи»**. Обновления оригинала регулярно вливаются в FreeTurnX.

Версия `0.1-5.3.4` означает: FreeTurnX `0.1` на основе FreeTurn `5.3.4`. Приложение обновляется из [релизов FreeTurnX](https://github.com/ftx-dev/freeturn-android/releases). FreeTurnX подписан своим ключом, поэтому поверх FreeTurn не ставится: сначала удалите FreeTurn (сохраните параметры подключения или сделайте бэкап).

### Фичи FreeTurnX

- **Раздача через Shizuku** - точка доступа Wi-Fi раздаёт VPN без root, на подключённых устройствах ничего настраивать не нужно. Нужны Android 13+ и приложение [Shizuku](https://shizuku.rikka.app/).
- **SOCKS5-клиент** - подключает телефон к SOCKS5-прокси: например, к SOCKS5 Hotspot другого телефона с FreeTurn, к чьему Wi-Fi вы подключены. Адрес подставляется из шлюза Wi-Fi; на раздающем телефоне включите UDP в SOCKS5 Hotspot.

> **Disclaimer.** Проект предназначен **исключительно для образовательных и исследовательских целей.**

> [!WARNING]
> **Переход на версию 5 требует настройки приложения с нуля.** Перед обновлением сохраните данные доступа к своему VPS и параметры подключения. После обновления заново добавьте сервер или импортируйте новую ссылку на вкладке "Соединение" (+), затем проверьте параметры VPN и раздельного туннелирования. При первом запуске v5 после обновления приложение покажет однократное предупреждение.

## Возможности

- **Добавление нескольких серверов**
- **Клонирование конфигурации серверов**
- **Быстрая установка на VPS**
- **Возможность делиться конфигами**
- **Встроенный VPN** (WireGuard / AmneziaWG) без отдельного VPN-клиента
- **Локальный прокси** для внешнего клиента
- **Relay / Direct** - подключение через TURN или напрямую
- **Импорт подключения** по ссылке или QR-коду
- **Бэкапы**
- **Раздельное туннелирование**

## Требования

- **Android 7.0+** (API 24)
- **Архитектура процессора:** `arm64-v8a` или `armeabi-v7a`
- **VPS**
- **Ссылка на звонок** для режима Relay; для Direct не требуется

## Благодарности

- **[@Moroka8](https://github.com/Moroka8)** - форк ядра [vk-turn-proxy](https://github.com/Moroka8/vk-turn-proxy)
- **[@alexmac6574](https://github.com/alexmac6574)** - форк ядра [vk-turn-proxy](https://github.com/alexmac6574/vk-turn-proxy)
- **[@cacggghp](https://github.com/cacggghp)** - оригинальное [vk-turn-proxy](https://github.com/cacggghp/vk-turn-proxy)
- **[@MYSOREZ](https://github.com/MYSOREZ)** - оригинальный Android-клиент [vk-turn-proxy-android](https://github.com/MYSOREZ/vk-turn-proxy-android)
