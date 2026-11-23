# YourIPTV

One Android player for phones and Android TV. The current source is **1.1.2 / code54**, including the playback-options auto-hide correction. The last independently confirmed published Play baseline is **1.1.1 / code52**; the hotfix submission status is recorded separately. This checkout also contains unpublished development and workflow changes; changing the source does not replace an installed release.

This is the synchronized customer distribution of YourIPTV. YourIPTV-GooglePlay is the permanent development master. Approved owner pushes to main activate a controlled one-way export to the YourIPTV customer repository. The first export is prepared on a review branch; later exports update only managed application files. The owner publishes application releases manually.

Shared features include Live TV, EPG, user-selected M3U/Xtream sources, movies and series, resume/history/progress, optional next-episode autoplay and touch/remote navigation. Settings Lock defaults off; an existing saved choice remains in effect. Supply content you have permission to use. HTTP and HTTPS sources are supported; HTTP is unencrypted, HTTPS retains certificate validation and HTTPS-to-HTTP redirects are blocked.

Use JDK21 and the Gradle wrapper. The default distribution in this export is customer; customer delivery retains its existing APK updater and customer configuration through selected source hooks. Both use com.youriptv.app.

```text
./gradlew -Pdistribution=play testReleaseUnitTest lintRelease
./gradlew -Pdistribution=customer testReleaseUnitTest lintRelease
```

These checks compile and test source without packaging a new release. Do not rebuild or republish protected 1.1.1 as part of the development migration.

See [Development workflow](docs/DEVELOPMENT_WORKFLOW.md), [Signing and security](docs/SECURITY_AND_SIGNING.md) and [release notes](docs/RELEASE_NOTES.md). Future owner-authorized releases are prepared locally with scripts/prepare_release.py into immutable version folders containing separate signed AAB/APK files and matching JSON records. No workflow uploads packages to Google Play or publishes GitHub releases.
