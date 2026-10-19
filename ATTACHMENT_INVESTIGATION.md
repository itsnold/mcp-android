# Automatic chat images: investigation, 2026-10-19

Requirement: when a selected chat is read, its images should arrive as image
content alongside the text, without manually sharing screenshots.

## Confirmed from public interfaces

- The official Android messages provider documentation lists roomId, originalId,
  senderContactId, timestamp, isSentByMe, isDeleted, type, text_content, reactions,
  displayName, is_search_match, paging_offset and last_read. It does not document
  an attachment URL, byte column, or media-download endpoint.
  https://developers.beeper.com/android/content-providers/api-reference
- The published Android provider changelog does not announce attachment access;
  its latest listed version is 4.32.0 (2025-08-15). This may lag the installed app.
  https://developers.beeper.com/android/content-providers/changelog
- The original MCP handler requests all cursor columns (null projection) but
  reads fixed text/metadata columns. Non-text messages become type placeholders.
  Thus handler behavior alone does not establish which additional fields an
  installed version might return.
- Public upstream MCP issues checked: no attachment feature was present in the
  returned issue list. No public Android-app provider implementation was found
  in the sources checked; absence from these sources is not proof none exists.
  https://github.com/beeper/mcp-android
  https://developers.beeper.com/open-source
- Beeperbox can download attachments, but runs against Beeper Desktop, either
  supplied by the user or bundled inside Docker. Its Node lite mode does not
  replace the Desktop dependency and is not an Android provider fix.
  https://github.com/hamr0/beeperbox
- Beeper's on-device and cloud bridges are different paths. The observed local
  room IDs end in beeper.local. Do not assume a Matrix cloud login exposes the
  same local conversations or their media.
  https://developers.beeper.com/bridges

## Runtime result: Beeper Android 4.61.1

The user's installed version was inspected through the diagnostic tool. In
addition to the documented columns it returns `image_path`, `image_width`,
`image_height`, `video_path`, `video_width`, `video_height`, and `order`.
The five sampled rows had null image paths; no attachment location or image
bytes have been verified yet. The docs alone were not sufficient to rule out
automatic Android attachments.

Version 1.7 adds bounded image reads to `get_messages` for explicitly selected
`roomIds`: `includeImages` defaults to true, `maxImages` to 2 (cap 3), and the
combined base64 image payload is limited to 8 MiB. The tool now advertises its
filter/pagination/image parameters in its input schema. It returns attributed
MCP ImageContent only when the Beeper-supplied location can actually be read.
Deleted messages and global-inbox queries never trigger image reads. Text and
image captions remain available even when image access fails.

Only Beeper content authorities and canonical paths inside Beeper's expected
storage roots are attempted, using ordinary Android permission checks. No
arbitrary HTTP downloads, path rewriting, sandbox bypass, or broad photo
permissions are used. Failures report the location format (and content URI
authority), not private filenames or tokens. Actual location accessibility and
delivery into the model's context still require installation and a selected
chat with an image. This is an attachment-reader candidate, not a verified fix.

## Diagnostic tool

### Version 1.7 runtime test result

Reading the latest 40 SYSDEV messages found the two IMAGE messages from Ralph
on October 15 at 18:16. Both failed before decoding with IllegalStateException
and "unsupported URI scheme". No image bytes were returned. This is not an
Android permission-denied result, and does not yet identify the URI scheme.
The original diagnostic hid it too aggressively; do not infer that these are
local files, Matrix media, or any other specific format from this result.

Version 1.8 reports the exact syntactically valid scheme in failure messages
and adds `inspect_chat_media(roomIds, limit, offset)`, which reports media
schemes and relevant hosts from explicitly selected chats without returning
message text, media filenames, URL tokens, or bytes. It does not attempt any
new resolver, cloud request, credential access, or permission bypass. Its purpose
is to identify the actual media format before implementing another reader.

Version 1.6 adds `inspect_message_provider`. It uses only the existing read
permission and the documented messages query, requesting all columns, and
reports the installed Beeper version plus actual column names and types from
up to five rows. It never reads or returns message values or blob contents.
This lets us check whether the installed version exposes undocumented media
fields before building an attachment reader. No root, sandbox access, credential
extraction, guessed hidden media paths, or broad photo permissions are used.

If a usable media field is found, verify the authorized retrieval method first,
then implement bounded fetching of images from the selected rooms/messages and
return MCP ImageContent with source attribution. Cap image size and image count;
never attach every photo from every conversation automatically.

If returned paths cannot be read under Android permissions, automatic attachments
remain blocked on an accessible provider interface. Next supported paths are an upstream Android media API
request or a separately authorized Desktop/remote deployment. Neither has been
set up or filed by this investigation.

The 1.5 share-target feature is optional/manual and does NOT satisfy automatic
chat image delivery. Successful compilation is not verification of runtime
provider fields or end-to-end image delivery in OpenCode.
