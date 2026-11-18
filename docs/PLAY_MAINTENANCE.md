# Independent maintenance and release

> **Current status, November 17:** [Active Play 1.0.9 / proposed code 48](../GooglePlay-v1.0.9/CURRENT_STATUS.md) is owner-approved local preparation, pending unused Console-history verification. Code 45 artifacts are historical. No `.150` testing/reinstall without a new explicit owner request. The owner attributes the separate playback issue to the server; no encoder/decoder changes are included.

Play and customer projects may diverge. Play source/Git/CI/versions/artifacts/issues belong here. Do not run customer archive/sync helper for Play-only changes. No secrets from customer source are copied into Git/archive.

## Manual port

1. Read verified local customer change/tests without changing that project; record the local source hash and any owner-provided remote provenance. Current workspace tools must not call GitHub.
2. Review applicability to public generic HTTPS-only sources, default-OFF fresh Settings, Admin/parental security and Play delivery. Never restore cleartext transport or HTTPS-to-HTTP downgrade redirects. Never restore customer shortcuts/installer.
3. Apply selected source/test edits here; document omissions/adaptations. No automatic merge/sync/customer branch/worktree.
4. Run Play regressions/lint/artifact checks and safe form-factor QA; review permissions/privacy/dependencies.
5. Commit here locally. The owner handles any PRIVATE remote creation, verification and push manually; workspace tools must not call GitHub or publish anything under the current local-only instruction.

## Version/release

One versionName/code across devices. Code 45 is the historical Play delivery. A future compatible production code must exceed known diagnostic 47 and any later applicable code, and be unused in Console all tracks/App bundle explorer including drafts/retired uploads. 48 is locally proposed, pending unused Console-history verification. After selecting the next version/code, rebuild, test, verify signatures and hashes. Separate repository does not isolate same-package Android signing/upgrade rules.

Manually coordinate code allocation with customer-distribution releases because both distributions intentionally use `com.youriptv.app`. Keep an owner-controlled allocation record covering both release records and Console uploads, while preserving independent source/release histories and no automatic synchronization. Customer code 44 is published and private diagnostic code 47 has been used. Recheck the next release against every installation meant to update; code 45 cannot ordinarily update code 47. Rebuild, retest and re-hash after any code change. Do not modify the customer version here. Compatible signing is still required in either direction.

Freeze source commit/toolchain/commands, run checks, inspect final AAB, verify signature/certificates, record one AAB SHA256 and supplemental QA hashes. Update changelog/notes/privacy/test report. Preserve older deliveries before replacing anything. Customer backup/release/update server stays untouched.

Owner handles Console account/package/code/signing, public privacy, legal review access/actual assets and declarations; internal/required closed testing, TV/device catalog; explicit production-submission authorization. Never claim approval before Google review.

CI: read-only repository permissions, no embedded signing credentials, no customer hooks, no automatic Play publication. Artifacts must exclude credential-bearing QA data. Recheck official requirements at submission.
