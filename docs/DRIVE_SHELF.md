# Kho truyện Drive

Kho truyện is a separate screen, opened from Home or the cloud icon in Library.
The bottom navigation retains its existing three tabs and independent Search.
The library shows imported books only. Source management moves into a sheet during
search, so folder cards do not crowd the results. An empty
search shows sources only; a non-empty query searches a persisted filename index,
with 40 results per page. It does not open or download EPUBs to build that index.

Search folds Vietnamese accents (including decomposed filenames), treats punctuation
and underscores as word separators, and requires all query words. The final word
may be a prefix while typing; arbitrary matches inside words are excluded. Words
may be entered in another order. Ranking prefers exact titles, title prefixes and
phrases, then title-word matches, author/filename matches, and incomplete final words.
The author suffix after the last spaced dash remains searchable but has lower priority.

Existing cached filenames are reindexed locally in batches of 500 once after
upgrading, preserving import links and scan cursors. No Drive/EPUB download is
needed for that repair. Results and loading state belong to the latest request;
changing input clears the previous results immediately with a 250ms typing debounce.
These search improvements are source changes after APK2.0.37; they require a newer APK.

Only Google Drive folder links are accepted. With a configured restricted Drive
API key, the app reads public folders without Google account sign-in. REST requests
send the Android package and actual signing-certificate SHA-1. Tokens and keys are
never written into shelf source records or source URLs.

Without a public key, the existing optional account authorization path is retained
for private folders (read-only Drive scope). Backup drive.file permissions are
unchanged.

Folder listing is paginated and recursive (200 file descriptors per request, at most
two source scans and one EPUB download concurrently). Each successfully received page is
committed together with its next cursor. Interrupted scans resume; old records
are removed only after the entire scan succeeds. Expired page tokens restart the
scan once without clearing the existing searchable catalog. Refresh currently
enumerates file descriptors again; it does not use an account-wide Drive Changes
feed or download the EPUB payloads. Refresh is manual or on opening
the shelf when stale; it is not a scheduled background job while the app is closed.

EPUB downloads are streamed to a temporary file, checked, then imported through
the existing ImportEpub use case. Downloaded file paths are converted using the
platform file-URI constructor: Android requires a `file://` URI for ContentResolver;
desktop keeps its raw path. This repairs the import error after successful Drive
downloads. Paths containing spaces, accents, `#` or `%` remain intact.
There is no bulk EPUB/cover download. Removing a
source removes only its index; it never deletes imported library books or Drive
files. Downloading an updated EPUB follows the existing import policy and does
not replace saved chapters or reading progress automatically.

The supplied folder redirected unauthenticated requests to Google sign-in during
initial inspection. After the user configured a restricted API key and updated sharing, public API
access to the folder and paginated EPUB records was verified without OAuth.
Physical A9 results remain unverified.

## Public-folder configuration

1. Enable Google Drive API in your Google Cloud project.
2. Create an API key restricted to **Google Drive API** and **Android apps**.
3. Add package `ir.kazemcodes.infinityreader.debug` with signing SHA-1
   `A3:76:AB:C8:1E:EB:7A:4A:0D:2D:23:E5:CE:61:7D:CC:9E:98:6B:E2`.
4. Save the key in ignored root `local.properties`:
   ```properties
   truyenplus.drive.api.key=YOUR_KEY
   ```
   CI can instead use `TRUYENPLUS_DRIVE_API_KEY`. Do not commit the key.
5. Share the Drive folder as **Anyone with the link → Viewer**. Paste its folder
   link into Story sources; no Google account sign-in is used in public mode.

The key is embedded in Android app metadata when building and therefore must be
restricted; it is not an OAuth token and cannot grant access to private files.
Production requests derive the package/certificate from the installed app.

Private-folder OAuth is optional and requires an Android OAuth client and consent
for `https://www.googleapis.com/auth/drive.readonly` in Google Cloud. Interactive
account authorization is Android-only; desktop tests inject test credentials.

References: [API key configuration](https://developers.google.com/workspace/guides/create-credentials),
[Android key restrictions](https://docs.cloud.google.com/api-keys/docs/add-restrictions-api-keys),
[Drive list pagination](https://developers.google.com/workspace/drive/api/reference/rest/v3/files/list).

Verification: Android compilation and 20 selected desktop tests passed, including
18,000 SQLite fixture entries, accent-insensitive search, literal SQL wildcards,
recursive interrupted scans, expired cursor handling, streaming download bounds,
temporary file cleanup, import linkage, migration preserving saved books, and
existing navigation/EPUB regressions. Live account authorization and A9 performance remain unverified. Public-key
verification is recorded separately after configuration.

Public-mode verification after key configuration: the actual shared folder exposed 18316 EPUBs across 92 pages. A streamed sample download matched its size and contained a valid ZIP/container/OPF with real metadata. No Google account/OAuth token was used. This was a workstation verification, not an A9 performance or device-install test.

The separate shelf and Android file-URI repair are source changes after 2.0.37, pending a new APK.
