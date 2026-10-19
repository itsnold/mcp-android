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

## Runtime check still required

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

If only the documented fields exist, automatic attachments remain blocked on
the provider interface. Next supported paths are an upstream Android media API
request or a separately authorized Desktop/remote deployment. Neither has been
set up or filed by this investigation.

The 1.5 share-target feature is optional/manual and does NOT satisfy automatic
chat image delivery. Successful compilation is not verification of runtime
provider fields or end-to-end image delivery in OpenCode.
