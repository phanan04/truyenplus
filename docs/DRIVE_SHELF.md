# Kho truyện Drive

The library keeps downloaded books separate from connected Drive folders. An empty
search shows sources only; a non-empty query searches a persisted filename index,
with 40 results per page. It does not open or download EPUBs to build that index.

Only Google Drive folder links are accepted. Android uses Google Play Services
AuthorizationClient and the read-only Drive scope. The existing backup scope
(drive.file) cannot list an arbitrary existing 18,000-file collection. Drive API
must be enabled and an Android OAuth client registered for the app package and
signing certificate. Tokens stay out of source files and source URLs.

Folder listing is paginated and recursive (200 file descriptors per request, at most
two source scans and one EPUB download concurrently). Each successfully received page is
committed together with its next cursor. Interrupted scans resume; old records
are removed only after the entire scan succeeds. Expired page tokens restart the
scan once without clearing the existing searchable catalog. Refresh currently
enumerates file descriptors again; it does not use an account-wide Drive Changes
feed or download the EPUB payloads. Refresh is manual or on opening
the shelf when stale; it is not a scheduled background job while the app is closed.

EPUB downloads are streamed to a temporary file, checked, then imported through
the existing ImportEpub use case. There is no bulk EPUB/cover download. Removing a
source removes only its index; it never deletes imported library books or Drive
files. Downloading an updated EPUB follows the existing import policy and does
not replace saved chapters or reading progress automatically.

The supplied folder redirected unauthenticated requests to Google sign-in during
initial inspection. Actual full-shelf sync requires an authorized account with
access to that folder; no live 18,000-book or physical A9 result is assumed.

## Google Cloud setup for the Android build

1. Enable Google Drive API in a Google Cloud project.
2. Configure the OAuth consent screen for the intended accounts and include
   `https://www.googleapis.com/auth/drive.readonly`. For a private testing app,
   add the actual user's Google account as a test user.
3. Register an Android OAuth client with package
   `ir.kazemcodes.infinityreader.debug` and signing certificate SHA-1
   `A3:76:AB:C8:1E:EB:7A:4A:0D:2D:23:E5:CE:61:7D:CC:9E:98:6B:E2`.
4. In the app, tap Connect Google Drive, grant read access, and paste the folder
   link. The Android AuthorizationClient uses the package/certificate identity;
   no client secret, service-account private key, or token is embedded in the APK.

References: [Google authorization on Android](https://developer.android.com/identity/authorization),
[Drive list pagination](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/list),
[link resource keys](https://developers.google.com/workspace/drive/api/guides/resource-keys).

A link shared to “anyone with the link” still uses this authenticated API path in
this first implementation. There is no anonymous Drive HTML scraper or developer
API key. Desktop can inspect cached shelf data; interactive Drive authorization
is Android-only for now.

Verification: Android compilation and 20 selected desktop tests passed, including
18,000 SQLite fixture entries, accent-insensitive search, literal SQL wildcards,
recursive interrupted scans, expired cursor handling, streaming download bounds,
temporary file cleanup, import linkage, migration preserving saved books, and
existing navigation/EPUB regressions. Live account authorization and A9 performance
remain unverified until Google Cloud setup and account consent are completed.
