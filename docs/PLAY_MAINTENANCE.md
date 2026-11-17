# Independent maintenance and release

Play and customer projects may diverge. Play source/Git/CI/versions/artifacts/issues belong here. Do not run customer archive/sync helper for Play-only changes. No secrets from customer source are copied into Git/archive.

## Manual port

1. Read verified local customer change/tests without changing that project; record the local source hash and any owner-provided remote provenance. Current workspace tools must not call GitHub.
2. Review applicability to public generic sources, default-OFF fresh Settings, Admin/parental security and Play delivery. Never restore customer shortcuts/installer.
3. Apply selected source/test edits here; document omissions/adaptations. No automatic merge/sync/customer branch/worktree.
4. Run Play regressions/lint/artifact checks and safe form-factor QA; review permissions/privacy/dependencies.
5. Commit here locally. The owner handles any PRIVATE remote creation, verification and push manually; workspace tools must not call GitHub or publish anything under the current local-only instruction.

## Version/release

One versionName/code across devices. Proposed45 must be checked in Console all tracks/App bundle explorer including drafts/retired uploads. Record unused-code evidence; if used, choose next higher unused and rebuild/retest/re-hash. Separate repository does not isolate same-package Android signing/upgrade rules.

Manually coordinate code allocation with customer-distribution releases because both distributions intentionally use `com.youriptv.app`. Keep an owner-controlled allocation record covering both release records and Console uploads, while preserving independent source/release histories and no automatic synchronization. If any customer release reaches code 45 or higher before the proposed Play candidate is uploaded or rolled out, recheck its upgrade path and select an unused code higher than the installations meant to update. Rebuild, retest and re-hash after any code change. Do not modify the customer version here. Compatible signing is still required in either direction.

Freeze source commit/toolchain/commands, run checks, inspect final AAB, verify signature/certificates, record one AAB SHA256 and supplemental QA hashes. Update changelog/notes/privacy/test report. Preserve older deliveries before replacing anything. Customer backup/release/update server stays untouched.

Owner handles Console account/package/code/signing, public privacy, legal review access/actual assets and declarations; internal/required closed testing, TV/device catalog; explicit production-submission authorization. Never claim approval before Google review.

CI: read-only repository permissions, no embedded signing credentials, no customer hooks, no automatic Play publication. Artifacts must exclude credential-bearing QA data. Recheck official requirements at submission.
