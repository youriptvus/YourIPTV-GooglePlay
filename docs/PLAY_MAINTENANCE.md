# Independent maintenance and release

Play and customer projects may diverge. Play source/Git/CI/versions/artifacts/issues belong here. Do not run customer archive/sync helper for Play-only changes. No secrets from customer source are copied into Git/archive.

## Manual port

1. Read official customer change/tests without changing that project; record source hash/URL.
2. Review applicability to public generic sources, default-OFF fresh Settings, Admin/parental security and Play delivery. Never restore customer shortcuts/installer.
3. Apply selected source/test edits here; document omissions/adaptations. No automatic merge/sync/customer branch/worktree.
4. Run Play regressions/lint/artifact checks and safe form-factor QA; review permissions/privacy/dependencies.
5. Commit here. Push only to verified PRIVATE new remote within owner authorization; no automatic release/Play publication.

## Version/release

One versionName/code across devices. Proposed45 must be checked in Console all tracks/App bundle explorer including drafts/retired uploads. Record unused-code evidence; if used, choose next higher unused and rebuild/retest/re-hash. Separate repository does not isolate same-package Android signing/upgrade rules.

Freeze source commit/toolchain/commands, run checks, inspect final AAB, verify signature/certificates, record one AAB SHA256 and supplemental QA hashes. Update changelog/notes/privacy/test report. Preserve older deliveries before replacing anything. Customer backup/release/update server stays untouched.

Owner handles Console account/package/code/signing, public privacy, legal review access/actual assets and declarations; internal/required closed testing, TV/device catalog; explicit production-submission authorization. Never claim approval before Google review.

CI: read-only repository permissions, no embedded signing credentials, no customer hooks, no automatic Play publication. Artifacts must exclude credential-bearing QA data. Recheck official requirements at submission.
