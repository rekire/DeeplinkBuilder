# The gradle plugin of DeeplinkBuilder

Nice that someone looks here too. Most parts of this plugin come with inspiration of the
[`SafeArgsPlugin`](https://github.com/androidx/androidx/blob/androidx-main/navigation/navigation-safe-args-gradle-plugin/src/main/kotlin/androidx/navigation/safeargs/gradle/SafeArgsPlugin.kt).

But this also means that it has one of their issues too. This plugin will break in later releases of
[AGP](https://developer.android.com/studio/releases/gradle-plugin-roadmap#agp_71_second_half_of_2021).
Currently I see there no option to work around, however when they fix their plugin I will get an
idea how to fix this plugin too.