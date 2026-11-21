# Adhan wallpaper photo credits

The Android build downloads these photographs from Wikimedia Commons and packages them inside the APK/AAB as local drawable resources. The installed app does not download these photographs at runtime.

- Al-Azhar Mosque — Mohamed Ouda — CC BY-SA 3.0
  https://commons.wikimedia.org/wiki/File:AlAzhar_Mosque.jpg
- Holy Kaaba — MohmmadRjab — CC0
  https://commons.wikimedia.org/wiki/File:Holy_Kaaba.jpg
- Al-Masjid an-Nabawi — Aymanzaid2 — CC BY-SA 4.0
  https://commons.wikimedia.org/wiki/File:Al_Masjid_an_Nabawi.jpg
- Al-Aqsa Mosque — جمان المحتسب — CC0 1.0
  https://commons.wikimedia.org/wiki/File:المسجد_الأقصى_المبارك١.jpg
- Quran photograph — Ali139099 — CC0 1.0
  https://commons.wikimedia.org/wiki/File:A_photo_of_a_Quran.jpg

The CI workflow validates that all five downloads are complete JPEG files before Gradle builds the app. A failed download stops the build instead of silently shipping blank or placeholder wallpapers.
