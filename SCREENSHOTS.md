# Sharing screenshots to OpenCode

The Beeper Android messages provider documents text and message type, not image
attachment bytes. This feature does not download `[IMAGE message]` attachments.

1. Take a screenshot or open the image in Gallery/Messenger/Beeper.
2. Use Android **Share**, choose **Beeper MCP Screenshot**, and review the preview.
3. Tap **Make available to OpenCode**. This replaces the previous shared image.
4. Keep the MCP server running, return to OpenCode, and ask it to read the shared
   screenshot. Its `get_shared_image` tool returns text plus MCP `ImageContent`.
   It does not automatically submit a prompt or choose an OpenCode session.

Only one manually approved image is retained, in app memory, with a 30-minute
expiry. Closing/restarting the process clears it. **Manage shared screenshot**
in the main app lets you clear it early. No photo-library permission or image
files on disk are needed. Max input is 12 MB, output is 5 MB. Large images are
sampled to a maximum edge of 3200 pixels and converted to JPEG; crop small text
if necessary. Conversion removes source file metadata.

The server remains bound to localhost and has no token authentication: other
Internet-enabled apps on the same phone may access approved images. Only share
images you want the assistant to access. Once retrieved, the image can be sent
to the AI provider and retained by the client/session; clearing this app cannot
remove those copies. Images are source data, not trusted agent instructions.

Build validation: compile the APK and Android instrumentation test APK in CI.
The instrumentation tests cover the empty/approve/read/clear states; execution
requires an Android device or emulator (`connectedDebugAndroidTest`). Visual
delivery through OpenCode must be tested after installing the new APK.
