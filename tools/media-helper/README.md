# NV Windows media bridge

Original source: `Program.cs` and `SessionPolicy.cs`. Build with `./build.ps1 -Test` on Windows; no Python or downloaded .NET SDK is required for the music bridge. The compiler uses the installed Windows .NET Framework and WinRT metadata. Runtime requires Windows 10 1809 or newer and a player that exposes a Windows media session.

## Source selection

- Automatic mode considers up to 16 Windows media sessions together. It retains the selected playing session, otherwise prefers the OS current playing session; paused sessions remain available for resume.
- A three-second selection hold protects pause/resume commands against an immediate source switch. Each click makes one targeted SMTC call, with no second media-key/Python fallback.
- Manual filters: Yandex Music, VK Music, Spotify, Browser. Desktop application IDs and identifying metadata labels are matched. Browser IDs include Chrome, Edge, Firefox, Opera, Brave, Vivaldi, Waterfox, Floorp, Zen, Yandex Browser and browser-labelled IDs.
- Windows does not expose a tab URL in this API. If a browser gives only artist/title, the provider is unknown; Automatic/Browser still selects it. No guarantee is made that every browser exposes every tab as a separate Windows session or implements next/seek controls.
- Messenger application IDs and voice-message filenames are excluded; normal song/artist names are retained.

## Resource limits and protocol

Loopback only, ephemeral port, up to eight HTTP handlers, 8 KiB request headers, two-second network timeouts, 1.2-second WinRT operation deadline, 500 ms background polling, 2 MiB cover bound. Cover decoding is requested only after track changes. AudioSpectrum.cs reads WASAPI loopback from the default output device and computes 12 logarithmic FFT bands. It never opens a microphone and does not save audio. The spectrum describes the system output mix, so game sounds and other applications can also affect it. Unsupported devices return zero bands.

The cached timeline records its sampling time and playback speed. HTTP reads extrapolate a playing session between native polls; paused sessions retain their reported position. A transient stopped state during a control hold is exposed as paused instead of discarding the selected track. Missing selected sessions receive a three-second grace period.

GET `/health`, `/now-playing`, `/cover`, `/sessions`; POST `/source?filter=auto|yandex|vk|spotify|browser`, `/control/play-pause`, `/control/next`, `/control/previous`, `/control/seek?pos=MILLISECONDS`, `/shutdown`. Failed controls return 409. Requests carrying an Origin header are rejected. The JVM launches/stops its own process; legacy fixed-port discovery and legacy Windows binaries/Python controller have been removed.

`PolicyTests.cs` passes 28 checks covering source recognition, simultaneous players, selection retention, transient stopped states, extended browser IDs, voice exclusion and legitimate artist/song metadata. The Minecraft production-JAR test separately verifies bundled helper startup, five source filters, one HTTP toggle per click, media-gap grace and expiry, pause-clock stability, accepted song metadata and excluded messenger sources. Real Yandex/VK/Spotify playback controls still require player acceptance tests.

Official references: [Windows SMTC](https://learn.microsoft.com/en-us/uwp/api/windows.media.control.globalsystemmediatransportcontrolssession?view=winrt-26100), [browser Media Session](https://web.dev/articles/media-session).
