# ExCoda (https://excoda.app)

A gesture-controlled music sheet viewer for Android tablets, supporting guitar tablature (GPX/GP), MusicXML and PDF files.  
Open files, annotate, and use face gestures completely offline.

## Overview

ExCoda is an Android application designed to make reading tablatures and sheet music efficient.  
Built with a focus on tablets, it allows musicians to turn pages using facial gestures while keeping their hands on their instrument.  
The app supports guitar tablature formats (GPX/GP) and MusicXML through AlphaTab integration.  
It also supports standard PDF files and creating PDF annotations through PDF.js.  
[Registra](https://github.com/appexcoda/registra) - optional companion REST API server to access your sheet music library and tabs.

No tracking or analytics are performed. Face detection is on‑device.  
MediaPipe (used for face‑gesture detection) has its own telemetry, explained later.

## Key Features

### Gesture Control
Control page turns without touching your device. Two gesture modes available:
- **Mouth movements**: Move your pressed lips to the left or right to navigate pages
- **Brow and smile**: Raise right eyebrow to advance, open smile to go back

Face tracking indicator shows when gestures are active. Transparent preview overlay helps with initial setup.  
Tap the center of the screen to fade the controls.

### Per-File Settings
Settings for GP/GPX and MusicXML files are stored individually per file, allowing customized configurations:
- Systems spacing control
- Stave profiles (auto, tab, score, mixed layouts)
- Scale and stretch adjustments
- Bars per system (automatic or manual)
- Tab rhythm display modes
- Notation element visibility (tempo, tuning, chord diagrams, dynamics, lyrics, etc.)
- Switch between instrument tracks in multitrack files

### PDF Support
View standard PDF sheet music with the same gesture controls, plus text notes, highlights, drawings, and adjustable page layouts.

### [Registra](https://github.com/appexcoda/registra) Integration
Optional companion REST API server for managing your sheet music library and tabs:
- Self-hosted solution with API key authentication
- Works with your existing folder layout
- Quick setup via QR code scan from your Registra server
- Smart search - search for files by text, artist or title (fuzzy matching)
- Upload new files to your collection
- Download files to your device
- Supports self-signed SSL certificates via TOFU (Trust On First Use) certificate pinning

### Clean Interface
Minimal, distraction-free UI focused on content.

## Who Can Benefit

- **Guitarists** practicing with tablature who need hands-free page turns
- **Musicians** reading PDF sheet music during practice
- **Teachers** demonstrating pieces without interrupting playback
- **Students** building a searchable library of practice material
- **Anyone** who wants to keep their hands on their instrument while reading music

## Tested Devices

Developed and tested on:
- TCL NXTPAPER 14
- Samsung Galaxy Tab A8 10.5"

## Privacy

This app performs no tracking or telemetry. The only telemetry involved comes from MediaPipe’s built‑in components.  
Your music files, annotations, and usage patterns are never collected or transmitted.  
The [Registra](https://github.com/appexcoda/registra) feature is optional and connects only to servers you explicitly configure.

## MediaPipe Telemetry

MediaPipe’s Android components send limited diagnostic telemetry to Google.  
No images, video, or gesture data is transmitted.  
This telemetry describes how the library itself performs, not the content this app processes.  
More details about what is collected can be found in the [MediaPipe TOS](https://ai.google.dev/edge/mediapipe/legal/tos).

## Gesture Control Disclaimer

**Important: Gesture detection is experimental and inherently unreliable.**

Face gesture recognition depends on multiple factors:
- Lighting conditions
- Camera quality and positioning
- Device performance
- Individual facial features and movements
- Environmental interference

**Do not rely on gestures for time-critical or professional performances.** The technology may fail to detect gestures, trigger false positives, or stop working unexpectedly during use.

## Technical Details

- **Architecture**: Modular design with separate feature modules
- **Face tracking**: MediaPipe Face Landmarker with BlendShape detection
- **Rendering**: WebView-based with AlphaTab and PDF.js
- **Storage**: DataStore for settings persistence, per-file configuration
- **Networking**: Retrofit with OkHttp for Registra API, custom SSL handling

## License

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
