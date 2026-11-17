# Vyro (repo: khatabill) - project notes for Claude

Vyro, tagline "Every Rupee Accounted.", is a Kannada-first, offline-first Android billing / GST / inventory / udhaar app by Teczo Softwares, Mysuru.
Package `com.teczo.khatabill` (internal names such as KhataBill* classes are unchanged on purpose; do not rename the applicationId without asking).

## Working rules (from the owner)
- State the files you will touch before coding. Wait for approval on architecture, dependency or schema changes.
- Give complete files. Never invent library versions: read `gradle/libs.versions.toml` and look versions up.
- Every user-visible string goes in `values`, `values-kn` and `values-hi` (StringsCompletenessTest enforces it).
- Money is integer paise (Long), shown with Indian grouping (1,23,456.00). Quantity in thousandths, rates and discounts in basis points.
- Must work fully offline.
- The owner wants a preview (screenshots or APK) before commit and push of new work.
- After each phase, report: done / untested / assumed.

## Stack
Kotlin, Jetpack Compose, Material 3, MVVM + repositories, Hilt (+ Hilt-Work), Room (schema exported, migrations tested), DataStore, WorkManager, ZXing core (QR), Android PdfDocument with bundled Noto Sans Kannada/Devanagari, FileProvider.
AGP 9.5.1 (built-in Kotlin), Gradle 9.9.1, KSP, JDK 21, minSdk 26, compile/target 37.

## Key rules in the code
- GST: `domain/gst/GstCalculator` (CGST=SGST intra-state, IGST inter-state, inclusive prices, pro-rata overall discount, round to rupee).
- Invoice number `{prefix<=2}{series}/{26-27}/{0000}`; never reused (cancel, never delete).
- Stock is derived from StockMovement rows written in the same transaction as the invoice.
- Party balance is always derived, never stored (positive = party owes us).
- Payments: Payment + PaymentAllocation (oldest-first or chosen invoice, remainder on account). `invoice.paidAmount` is a cached sum updated in the same transaction.
- DB is at version 2 (`MIGRATION_1_2` tested). Any schema change needs a new migration, the exported schema JSON and a migration test.
- Design: teal #1B4B5A, orange #F5A54A, peach #FDE9D7, raised orange centre Billing button. The logo is navy/green; matching the theme to the logo is an open question.

## Status
- Done: Phase 0-1 (foundation, GST billing, items, parties, A4 PDF), Phase 2 (payments, ledger, aging, statements, UPI QR, WhatsApp share, due reminders), Phase 3 (expenses, Day/Cash/Bank books, sales/GSTR-1/GSTR-3B style reports, CSV/PDF export, Home dashboard), Vyro rebrand.
- Not tested: real Android phone (Android 8 / 2 GB RAM), tablet, dark mode, large font, airplane mode, low storage; WhatsApp, notification delivery and receipt camera on a device; CSV in Excel.
- Assumed: Kannada and Hindi strings are drafts needing native review; amount-in-words is English only; logo/signature on invoices deferred; Home quick-action labels may clip on small screens.
- Next: hardening on a real phone, then Phase 4 (barcode scan, quick bill, thermal printing, voice input, templates). Firebase sync, Play Billing, backup and app lock are not started. A signed release build needs a signing key (not created).

## Build and test
```
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest   # needs an emulator, e.g. Pixel 7 API 35
```
CI (`.github/workflows/ci.yml`) runs the first line and uploads the debug APK as the `vyro-debug-apk` artifact. CI does not run emulator tests.
