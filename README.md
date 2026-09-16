# Zenith

Zenith is a study helper and attention-management home experience for students. It is designed to make unconscious behavior more visible while keeping the final choice with the user.

## GitHub updates

Zenith can check this repository for the latest tagged release from **Settings → Updates**. If a newer release contains `zenith-release.apk`, Zenith downloads it and hands it to Android's package installer.

### Publishing an update

1. Create a Git tag such as `v1.0.1` and push it.
2. GitHub Actions builds a signed release APK and publishes it to the GitHub Release.
3. Users can open **Settings → Updates → Check for updates**.
4. Zenith downloads the APK and Android completes the installation/update flow.

The release APK **must be signed with the same signing key as the installed Zenith build**. Android will reject an APK signed with a different key, even when the package name is identical.

### Required GitHub Actions secrets

Configure these repository secrets once:

- `ZENITH_KEYSTORE_BASE64` — base64-encoded release keystore
- `ZENITH_KEYSTORE_PASSWORD` — keystore password
- `ZENITH_KEY_ALIAS` — signing key alias
- `ZENITH_KEY_PASSWORD` — signing key password

Keep the keystore and passwords private. Do not commit the keystore to the repository.

Main-branch pushes still create a debug APK artifact for development. Only `v*` tags publish update releases.
