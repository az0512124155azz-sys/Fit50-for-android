# System inset regression checks

The native FrameLayout owns system-bar, display-cutout and keyboard insets.
Its padding resizes the WebView viewport and consumes the insets, so HTML
fixed elements keep their original spacing without double-counting system bars.
Do not reintroduce page-load JavaScript offsets for Android system bars.

## Verified on 2026-09-20

Pixel 6 emulator, Android API 37, 1080 x 2400, density 2.625.
The test opened packaged HTML pages through WebView debugging without signing
into an account. This verifies layout and language selection, not account data.

| Check | Observed result |
| --- | --- |
| Gesture navigation, Home | WebView y=128, height=2209; ends at y=2337, above the 63 px gesture area. Floating menu retains approximately 16 CSS px bottom spacing. |
| Three-button navigation, Home | WebView y=128, height=2146; ends at y=2274, above the 126 px button area. Floating menu retains approximately 16 CSS px bottom spacing. Screenshot inspected. |
| Settings | Original approximately 20 CSS px menu spacing retained. |
| Device language default | English emulator selects English/LTR with preference `auto`. |
| Manual language selection | Hebrew and Arabic select RTL. Hebrew selection survives navigation from Home to Settings. All 16 languages plus Auto remain available. |
| Return to Auto | Restores English/LTR on the English emulator. |
| Keyboard on login | WebView height changes from 2146 to 1389 px and returns to 2146 after Back dismisses the keyboard. |
| System Back | Returns from Login to the preceding Settings page through the AndroidX dispatcher. |
| Build and lint | `:app:assembleDebug :app:lintDebug` pass; existing lint warnings remain. CI now runs both tasks. |

## Repeat before future layout changes

1. Check Home and Settings in gesture and three-button modes, including a cold launch.
2. Open bottom sheets; verify their controls remain above the system controls.
3. Open and dismiss a keyboard; check that the viewport restores without accumulated padding.
4. Switch Auto, Hebrew and Arabic, then navigate between pages and restart the app.
5. Exercise system Back with and without WebView history.
6. Also verify a physical device and an older supported Android version before
   claiming coverage across all devices; those were not tested in this session.
