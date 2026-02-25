# DayCard2

A simple Android app that generates an image every day at **00:01** with the weekday in **English (UPPERCASE)**, **Czech (UPPERCASE)**, and **Marathi (Devanagari)** and saves/overwrites the same image in your device Gallery.

- Package: `com.amogh.dailyday`
- Output image path: `Pictures/DayCard2/DAILY_DAY.png`
- Scheduler: WorkManager (periodic, persists across reboots)

## Build steps
1. Open the project folder in **Android Studio (Giraffe or newer)**.
2. Let Gradle sync. If the wrapper JAR is missing, Android Studio will **re-generate the Gradle Wrapper** automatically.
3. Click **Build > Build APK(s)** or run on a device.

## Notes
- On **Android 9 (API 28) and below**, the app requests `WRITE_EXTERNAL_STORAGE` to write to the Gallery.
- On some OEMs, background scheduling may be delayed by battery optimizations. Use the in-app button **"Improve Reliability"** to request exclusion.

## Customize time
Change `minutesUntilNextRun(hour = 0, minute = 1)` in `MainActivity.scheduleDailyWork()` to your preferred time.
