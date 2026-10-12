# Changelog

## 1.6
- Progress by routine now groups workouts by **routine**, not by title:
  - Workouts started from a routine are linked to it and stay in its charts even if you rename them, add or remove exercises mid-workout, or edit the routine later.
  - Older and imported workouts (e.g. from Hevy) are matched by their **exercises**, comparing with the routine and its recent sessions, so the same routine with different titles ends up in the same chart.
- Fix the grouping by hand in the workout details (**⋮ → Routine of this workout**); the details also show which routine a workout belongs to.
- Automatically detected routines can be saved as real routines with one tap.
- Each session in the list shows how many exercises were added or removed compared with the previous one, to explain jumps in volume.
- Database version 3 (automatic migration, no data loss).

## 1.5
- New **Progress by routine** screen: total volume and total sets of each routine over time, with average, best and change for the selected period, plus the list of sessions.
- Open it from a routine's menu (**View progress**) or from **Profile**.
- Workouts are grouped by name, so workouts imported from Hevy are included.

## 1.4
- 11 new exercises in the catalog (Bench Press (Smith), Iso-Lateral Chest Press, Chest Supported Incline Row, Chest Supported T Bar Row, Iso-Lateral High Row, Seated Lateral Raise, Leg Press Horizontal, Standing Calf Raise (Smith), Decline Crunch (Weighted), Cycling, Walking).
- Custom exercises with the same name are automatically converted into the catalog ones, keeping their history.
- The file picker for imports accepts any file (the Hevy CSV appeared greyed out in some file managers).

## 1.3
- Import workout history from Hevy (CSV file).

## 1.2
- Optional AI coach powered by Google Gemini (free tier).
- Automatic progression suggestions during workouts.
- Exercise photos (downloaded on demand) and a shortcut to technique videos on YouTube.

## 1.1
- Six languages: Portuguese, English, Spanish, French, German and Italian.
- Translated exercises, muscle groups and equipment; dates and numbers in each country's format.

## 1.0
- First version: workouts, routines, rest timer, personal records with alerts, per-exercise charts, statistics, body weight and backups.
