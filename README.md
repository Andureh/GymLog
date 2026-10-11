# GymLog 🏋️

A free Android app to log your gym workouts and track your progress, inspired by Hevy. No ads, no account, and your data stays on your phone.

**[⬇️ Download the latest version (APK)](../../releases/latest)** · Android 8.0 or newer

---

## Features

### Workouts
- Live workout logging with weight × reps, reps only, or duration. Set types: normal, warm-up, drop set and to failure.
- A **"Previous"** column shows what you did last time. If you tick a set without filling it in, the app uses those values.
- Automatic **rest timer**, configurable per exercise (±15 s or skip). It vibrates and shows a notification when the rest is over.
- **Personal record (PR) alerts** 🏆 the moment you beat a record: heaviest weight, estimated 1RM, best set volume, most reps or longest duration.
- Notes per workout and per exercise, keep-screen-on during workouts, and editing of finished workouts.

### Routines & exercises
- Routines with sets, reps, weight and rest time, started with one tap. You can also repeat a past workout or save it as a routine.
- **122 built-in exercises** with search and muscle filters, plus your own custom exercises.
- **Exercise photos** (start and end position, alternating) and a shortcut to technique videos on YouTube.

### Progress
- **Per-exercise charts**: estimated 1RM, heaviest weight, best set, volume and reps, over 1M / 3M / 6M / 1Y / All.
- Personal records and "best weight for each rep count".
- Unlimited history, grouped by month.
- Statistics: workouts and volume per week, sets per muscle group, weekly training streak.
- Body weight log with chart.

### Optional helpers
Each one can be switched on or off in **Profile → Training helpers**.
- 💡 **Progression suggestions**: proposes the next weight and reps based on your previous sessions (double progression, plus a ~10% deload when you stall).
- 🤖 **AI coach (Google Gemini)**: chat with a coach that sees your training history and can analyse workouts. Uses your own free API key (see below). **Off by default.**

### Data
- **Import from Hevy** (CSV file).
- Full backup to JSON (export and import).
- kg or lb.
- **6 languages**: 🇵🇹 Português · 🇬🇧 English · 🇪🇸 Español · 🇫🇷 Français · 🇩🇪 Deutsch · 🇮🇹 Italiano. The app follows the system language or the one you choose; exercise names are translated too.

---

## Installation

1. Download `GymLog.apk` from the [Releases page](../../releases/latest).
2. Open it on your phone and tap **Install**. If Android warns about "unknown sources", allow the installation.
3. New versions install over the old one and keep your data.

## Importing from Hevy

1. In Hevy: **Profile → Settings → Export & Import Data → Export Workouts**. This creates a `workout_data.csv` file.
2. In GymLog: **Profile → Data → Import from Hevy** and pick the file.

What is and isn't imported:
- Hevy exercises are matched to the app's exercises by name. Any that don't exist are created as custom exercises, with the muscle group and equipment guessed from the name.
- Workouts that already exist are skipped, so importing the same file twice is safe.
- Cardio distance, RPE and supersets are not imported.

## AI coach (optional and free)

1. Create a free key in **[Google AI Studio](https://aistudio.google.com/apikey)**.
2. In GymLog: **Profile → Training helpers → Enable AI coach** and paste the key. Use **"Test connection"** to check it works.
3. The app automatically picks the newest available *Flash* model; you can also choose one manually.

> **Privacy:** when you use the coach, a summary of your workouts is sent to Google. On the free tier of Gemini, Google may use that data to improve its products. The key is stored only on your phone. If you don't enable the coach, no data leaves your phone. Exercise photos are downloaded from the internet, but nothing is sent.

---

## Building from source

Requirements: **JDK 17** and the **Android SDK** (platform 34).

```bash
git clone https://github.com/Andureh/GymLog.git
cd GymLog
./gradlew assembleRelease      # APK in app/build/outputs/apk/release/
./gradlew testDebugUnitTest    # unit tests
```

The project also opens directly in Android Studio. The *release* build is signed with the local debug key so it can be installed directly.

> To update an installed app, the new APK must be signed with the same key. An APK built on a different computer requires uninstalling first, which deletes your data. Export a backup before doing that.

### Tech stack
Kotlin · Jetpack Compose (Material 3) · Room (SQLite) · Navigation Compose · coroutines/Flow. No third-party libraries: the charts, image loading and Gemini client are implemented in the app itself.

### Project structure

```
app/src/main/java/com/gymlog/app/
├── GymApp.kt, MainActivity.kt   # startup, dependencies, rest timer
├── data/        # Room (entities, DAOs, migrations), repository, settings,
│                #   backups, Hevy import, exercise photos
├── domain/      # records (PRs, 1RM), sessions, progression suggestions
├── ai/          # Gemini client, coach, and the training summary sent to the model
├── i18n/        # texts for the 6 languages + translated exercise catalog
├── ui/          # navigation, theme, components (charts, inputs, images) and screens
└── util/        # number/date/unit formatting, vibration
app/src/test/    # unit tests (records, translations, progression, Hevy import)
app/schemas/     # database schema per version (migrations)
```

### Translations
Each language is one file in [`i18n/`](app/src/main/java/com/gymlog/app/i18n). If a text is missing in any language, the project doesn't compile. Exercise, muscle and equipment names live in [`Catalog.kt`](app/src/main/java/com/gymlog/app/i18n/Catalog.kt), one line per exercise with all 6 languages.

---

## Credits

- Exercise photos: [Free Exercise DB](https://github.com/yuhonas/free-exercise-db) (public domain / Unlicense).
- Inspired by [Hevy](https://www.hevyapp.com/). This project is not affiliated with Hevy.

## License

[MIT](LICENSE)
