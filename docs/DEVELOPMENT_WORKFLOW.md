# Development and source synchronization

YourIPTV-GooglePlay/main is the permanent master. YourIPTV/main is a synchronized customer export, not a second development branch. Both distributions share app/src/main and com.youriptv.app. Distribution hooks preserve customer updater/preset/master policy and Play delivery/Admin policy.

## Owner's normal workflow

1. Develop and validate changes in the master checkout. Preserve existing owner changes and history.
2. Review the local diff and commit on main. The owner alone pushes master main to GitHub.
3. The approved main push runs .github/workflows/sync-youriptv.yml. It audits the pinned Git revision, tests both source selections and exports only .sync/export-policy.json application paths.
4. The customer README and default distribution are selected explicitly. .github workflows, repository configuration, unrelated history and unmanaged files are preserved.
5. .youriptv-sync.json records managed file hashes/modes. Subsequent additions, modifications and deletions affect only managed source. Independent customer changes stop sync for review; no force push, blanket mirror or empty synchronization commit is used.

## Initial activation and authentication

The initial destination baseline is pinned in .sync/export-policy.json and preserved by local backup refs. A local candidate must pass validation before the owner activates the workflow. On the first successful hosted run, the workflow creates only sync/bootstrap-master in YourIPTV. Customer main stays unchanged. Review its comparison and open/merge a pull request into main. Later approved master pushes synchronize customer main automatically. If the bootstrap branch exists but is not merged, the workflow stops; it never replaces it.

Configure master Actions secrets before the owner pushes:

- Preferred: SYNC_APP_CLIENT_ID repository variable plus SYNC_APP_PRIVATE_KEY Actions secret. Install the GitHub App only on the destination YourIPTV repository, with Contents read/write and mandatory Metadata read. It needs no Workflows, Administration, Issues or release permission. The workflow mints a short-lived destination-only installation token and the official action revokes it after the job.
- Alternative: SYNC_DESTINATION_TOKEN Actions secret containing a fine-grained PAT restricted to YourIPTV and Contents read/write. Leave SYNC_APP_CLIENT_ID unset when using this option.
- SYNC_RESTRICTED_TERMS_B64 Actions secret: base64 of a UTF-8 JSON list supplied privately. The public scripts never embed the restricted vocabulary. Missing/empty/malformed configuration stops export.

Keep the default GITHUB_TOKEN contents permission read-only. Enable the pinned actions used by these workflows. Destination branch rules must allow the authorized App to perform ordinary fast-forward source-sync commits after bootstrap approval. Do not bypass branch rules; if main requires a PR for every update, retain that review requirement and adapt the workflow rather than force-pushing.

Only the source-sync workflow may write the destination, on a hosted push to master main after validation. Pull requests and local helper invocations have no destination write authority. No workflow packages or publishes a release or changes Play Console. Actual hosted success can only be verified after the owner pushes the workflow. Check the Actions summary, target commit, ownership manifest and expected version in both repositories; do not infer success from a local dry run.

## Local validation and safe candidate

Use JDK21 and the Gradle wrapper:

```text
py -m unittest discover -s scripts/tests -p test_*.py
./gradlew -Pdistribution=play testReleaseUnitTest lintRelease
./gradlew -Pdistribution=customer testReleaseUnitTest lintRelease
py scripts/sync_repository.py audit --revision HEAD --terms-file <external-private-json>
py scripts/sync_repository.py plan --revision HEAD --destination <clean-local-candidate> --terms-file <external-private-json>
py scripts/sync_repository.py apply --revision HEAD --destination <clean-local-candidate> --terms-file <external-private-json>
```

Use a separate local clone at the reviewed customer baseline for candidate preparation. Do not apply over owner work. The local helper never pushes. Commit the migration before inspecting a pinned revision. Guard reports include sanitized categories/paths only; pattern checks supplement manual review and do not prove absence of every possible secret or text within images.

## Future explicitly authorized local releases

Version 1.1.1/code52 and its existing closed test are protected during this migration. Do not rebuild or republish it, bump the version for infrastructure, modify testers/links/tracks, or upload another AAB.

For a future version the owner explicitly requests, update version.properties once, synchronize CHANGELOG.md/docs/RELEASE_NOTES.md and confirm an unused versionCode above the highest Play upload. Signing properties and keys must be outside both repositories. Supply the independent environment variables YOURIPTV_UPLOAD_SIGNING_PROPERTIES and YOURIPTV_CUSTOMER_SIGNING_PROPERTIES, the private restricted-term JSON, JDK21, Android SDK and bundletool.

scripts/prepare_release.py provides plan (no build), prepare (requires --authorize-release), and verify (no build). See --help for required version/code, confirmed Play history, previous published customer code, release-notes file, exact customer APK URL and tool paths. A plan never changes the version or invents a URL. Prepare runs unit/lint checks, signed Play bundle/companion APK and signed customer APK sequentially, verifies compiled identity/permissions/signatures, generates actual hash/size metadata and activates an absent immutable version directory only after both pass. Failures do not activate or replace an existing release.

Future output root: Releases/YourIPTV/<version>/ outside source; on the owner's workstation it is D:\APK\Releases\YourIPTV. GooglePlay contains YourIPTV-GooglePlay-<version>.aab and release.json; GitHub contains YourIPTV-GitHub-<version>.apk, release.json and legacy update.json. Private verification metadata stays with the local pair. No new release folder is created merely to establish this system.

The customer updater still requires the canonical manually published YourIPTV.apk GitHub asset or the approved versioned website APK. The versioned local filename is not its download address. release.json records the public alias; manually attach the identical APK bytes under that alias with update.json. previousVersionCode controls optional/required prompts, never a minimum installed version or sequential upgrade requirement. An obsolete updater with no reachable known address or installation support cannot be repaired by metadata alone.

Build, synchronize source and publish remain separate operations. The owner manually publishes customer files and separately decides when to submit a future AAB. No GitHub Actions artifact is assumed to appear on the local drive.
