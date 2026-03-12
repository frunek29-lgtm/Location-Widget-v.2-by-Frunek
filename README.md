# Location Widget Stable

Stabilniejsza wersja widgetu lokalizacji dla Androida 10+.

## Co poprawiono
- Fused Location Provider zamiast surowego LocationManagera
- fallback do współrzędnych GPS, gdy geokoder nie zwróci adresu
- lepsza obsługa błędów, bez crashowania widgetu
- status w widgecie i w aplikacji
- przycisk do wejścia w ustawienia aplikacji i włączenia "Zezwalaj zawsze"

## Budowanie APK bez Android Studio
1. Wrzuć cały projekt na GitHub.
2. Wejdź w Actions.
3. Uruchom "Build Android APK".
4. Pobierz artefakt `app-debug-apk`.
