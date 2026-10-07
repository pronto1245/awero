# AWERO Android

Kotlin + Jetpack Compose application.

Critical alarm path:
AlarmManager → Wake Session → Mission Engine → Validation → Fallback.

Critical behavior must not require network or backend availability.
