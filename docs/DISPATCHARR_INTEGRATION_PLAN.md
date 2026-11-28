# Dispatcharr integration plan

Status: discovery complete; implementation not started.

## Goals

1. Add an optional Dispatcharr source switch to the existing OwnTV live-player Sources dialog.
2. Keep the current playback URL and channel selected in OwnTV. Ask Dispatcharr to change the upstream stream attached to the currently playing Dispatcharr channel.
3. Preserve the existing playlist-based Sources feature for non-Dispatcharr playlists.
4. In a separate phase, add a self-hosted remote configuration service and web dashboard. Do not expose OwnTV's LAN-only sync listener to the internet.

## Existing OwnTV extension points

- `app/src/main/java/tv/own/owntv/player/PlayerHud.kt`: `HudSources` and `HudDialog.SOURCES`.
- `app/src/main/java/tv/own/owntv/player/PlayerHudDialogs.kt`: `SourcesDialog` renders the current playlist-version list.
- `app/src/main/java/tv/own/owntv/features/shell/OwnTVShell.kt`: supplies `HudSources` using `ChannelVersions.of(channel)` and `LiveViewModel.ensurePlaying`.
- `OwnTV_Core/core/src/main/java/tv/own/owntv/core/database/entity/ContentEntities.kt`: `ChannelEntity.streamUrl` retains the imported stream URL.
- `OwnTV_Core/core/src/main/java/tv/own/owntv/core/live/ChannelVersions.kt`: current Sources implementation matches equivalent channels across OwnTV playlists. It does not query Dispatcharr.

## Dispatcharr API verified against its current source

Base URL is the Dispatcharr origin (for example, `https://dispatcharr.example`).

Authentication:
- Send the Dispatcharr API key in `X-API-Key`.
- The manual switch and live status endpoints require an administrator account (`IsAdmin`).
- Never put API keys in source code, logs, URLs, or Git commits.

Endpoints:
- `POST /api/channels/channels/by-uuids/` with JSON `{"uuids":["<channel-uuid>"]}`: resolve a Dispatcharr channel UUID to its numeric database ID.
- `GET /api/channels/channels/{channel-id}/streams/`: list the streams assigned to that channel, in configured order.
- `GET /proxy/ts/status/{channel-uuid}`: inspect live status, including the active `stream_id`, when the channel is running.
- `POST /proxy/ts/change_stream/{channel-uuid}` with JSON `{"stream_id":123}`: request a switch to a specific assigned stream.
- `GET /api/m3u/accounts/` (or retrieve the referenced account) can resolve M3U account IDs to provider names.

The API paths above are rooted at the Dispatcharr origin; the channel endpoints are under `/api`, while status/switch are under `/proxy/ts`.

## Channel matching

OwnTV's M3U parser stores the original URL in `ChannelEntity.streamUrl`. Dispatcharr's live proxy route is `/proxy/ts/stream/<channel-id>`; for the standard Dispatcharr M3U output the expected channel identifier is a UUID.

Implementation must:
1. Parse the UUID only from a recognized Dispatcharr live-proxy URL path.
2. Avoid fuzzy name matching for a switch operation.
3. Resolve the UUID through `by-uuids` and verify that the returned channel is the expected channel before showing or switching streams.
4. If the URL is not a recognized Dispatcharr URL, hide the Dispatcharr controls and leave existing Sources behavior unchanged.

The exact URL shape must be checked against a real M3U URL from the user's Dispatcharr instance before declaring support complete.

## First implementation slice

- Add a small typed Dispatcharr API client in Core, using the existing OkHttp client.
- Add tests for URL/UUID recognition, API response parsing, and error mapping.
- Add a Dispatcharr source model that contains stream ID, stream name, provider name, optional technical stats, and active state.
- Extend the player Sources dialog to show Dispatcharr alternatives and the active stream.
- On selection, call the specific-stream switch endpoint. Keep the current OwnTV channel/player URL unchanged; show success/failure feedback and refresh active state.
- Keep the existing OwnTV playlist Sources list available as a separate section or fallback.

## Credentials and deployment decision

The manual switch endpoint is administrator-only. Do not use a low-privilege playlist credential for it.

For the first local prototype, the API key must be held only in protected device storage (Android Keystore-backed encryption), never in plain preferences or logs. Before building the remote dashboard, evaluate moving Dispatcharr credentials to the self-hosted backend so TV clients do not hold the administrator key. The final architecture should prefer the backend as the credential-holding gateway.

## Remote synchronization: separate phase

OwnTV's existing `LocalAutoSyncClient/Server` is a paired-device LAN protocol, not a cloud service. Its requests are HMAC-signed and restricted to the local network. Do not expose its listener publicly.

`BackupManager` already exports/imports profiles, sources, customizations, favorites, history, resume positions, manual ordering, and settings in sectioned containers. Reuse its tested data model/merge semantics where appropriate, but design a separate authenticated HTTPS backend and dashboard for remote synchronization. Add device identity, per-device overrides, conflict/version handling, and audit-safe credential handling.

## Test plan

- Verify a Dispatcharr M3U live URL from the user's actual instance and the UUID parsed from it.
- Verify API-key authentication with an admin account.
- Test channel UUID resolution, stream listing, active stream status, and a manual switch from Swagger before connecting OwnTV.
- Test the switch while OwnTV is playing the channel; confirm the player URL stays unchanged and playback resumes after Dispatcharr's stream discontinuity.
- Test invalid UUID, expired/revoked key, unavailable channel, inactive channel, failed switch, and Dispatcharr offline.
- Build and run existing Core unit tests and Android app build before producing an APK.
