# Fort Launcher
Cozy, zero-permission Android TV home launcher for the Whitingers.
Night-sky theme, twinkling stars, pastel Watch row (Netflix, Disney+, YouTube...), HDMI tiles, time-of-day greeting.

## Install (no computer)
On the TV: install **Downloader** (by AFTVnews) from the Play Store, then open it and enter:
`https://github.com/whitingerworksllc/fort-launcher/raw/main/fort-launcher.apk`
Allow the install prompt, then press HOME and choose Fort Launcher.

## Or via ADB
`adb connect <TV-IP>:5555 && adb install fort-launcher.apk`

## Privacy
Zero permissions. Not even INTERNET. The launcher cannot phone home.

## Build
Android SDK 34 + build.sh (javac/d8/aapt2/apksigner, no Gradle).
