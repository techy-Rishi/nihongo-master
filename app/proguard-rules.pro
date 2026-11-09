# Keep all data models and enums in Nihongo Master
-keep class com.nihongo.master.** { *; }
-keepclassmembers enum com.nihongo.master.** { *; }

# Keep Android SpeechRecognizer and TextToSpeech callbacks safe
-keep class android.speech.** { *; }
-keep class android.speech.tts.** { *; }

# Compose stability rules
-dontwarn androidx.compose.**
