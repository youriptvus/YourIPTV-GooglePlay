# Independent repository separation

> **Current status, November 17:** [Active Play 1.0.9 / proposed code 48](CURRENT_STATUS.md) is owner-approved local preparation, pending unused Console-history verification. Code 45 artifacts are historical. No `.150` testing/reinstall without a new explicit owner request. The owner attributes the separate playback issue to the server; no encoder/decoder changes are included.

| | Customer | Google Play |
|---|---|---|
| Remote | https://github.com/youriptvus/YourIPTV | https://github.com/youriptvus/YourIPTV-GooglePlay |
| Local | D:\APK\Repository\YourIPTV | D:\APK\Repository\YourIPTV-GooglePlay |
| Task access | Read only | Implementation |
| History | Existing unchanged | Fresh independent Git |
| Updates | Customer updater unchanged | Play delivery |
| Package | com.youriptv.app | com.youriptv.app |
| Recorded baseline/delivery version |1.0.9/code44 source baseline; owner now confirms published |1.0.9/code45 historical artifacts |

Not a branch/worktree/fork/shared Git directory. Same Android package is intentional: normally no side-by-side install, and compatible certificates/higher code needed for updates. No automatic synchronization.

## Current scope: local work only

The owner's latest explicit instruction restricts workspace tools to local work. Do not call GitHub, read remote state, create a repository, push or upload anything. No remote creation, private-visibility verification or push was performed by workspace agents. This is an instruction boundary, not an authentication task for the agent to finish later.

The independent root repository already has origin configured locally for the exact new URL above, for fetch and push. It was configured before the local-only instruction and is retained for owner use. Do not add another origin. An intended origin does not prove the server repository exists, is private, or has no history. Obsolete comp6062 must not be used.

A preexisting nested checkout at `YourIPTV-GooglePlay/` inside this source root was found, ignored and preserved without alteration. Its initial-history evidence does not prove who created the remote or its visibility. Do not treat that nested checkout as this project's source/Git root or upload it. An existing remote may already have commits that must be reconciled without force push.

### Owner-run authentication and inspection

The commands below are documentation for the owner to run manually outside the workspace-agent workflow. They are not authorized workspace-tool actions under the current instruction. Missing authentication does not prevent completion of local builds, tests and release preparation.

```powershell
gh auth login --hostname github.com --git-protocol https --web
gh auth status --hostname github.com
gh api user --jq .login
```

The owner should require successful authentication as **youriptvus** before continuing. Do not print tokens. A 404 can conceal a private repository that the current token cannot access; it is not proof that a repository name is available.

```powershell
cd D:\APK\Repository\YourIPTV-GooglePlay
git remote -v
gh repo view youriptvus/YourIPTV-GooglePlay --json nameWithOwner,visibility,isFork,url
```

If the repository exists, require correct owner/name, PRIVATE and isFork=false before any push. Do not delete/recreate it or overwrite its history. If it cannot be viewed, resolve account/scopes/access first. **Only after its absence has been confirmed under the correct owner account**, create the remote without source/remote options (origin already exists):

```powershell
gh repo create youriptvus/YourIPTV-GooglePlay --private --description 'Independent unified YourIPTV Google Play application'
gh repo view youriptvus/YourIPTV-GooglePlay --json nameWithOwner,visibility,isFork,url
```

### Preserve any existing remote history

After successful private/non-fork verification, inspect remote refs and existing main history before pushing:

```powershell
git ls-remote origin
# If remote main exists, fetch ONLY into this independent root repository:
git fetch --no-tags origin
git log --oneline --decorate origin/main
# Exit0 means local main contains remote main; nonzero needs reconciliation.
git merge-base --is-ancestor origin/main main
```

Do not run the last two commands for an empty remote without main. For unexpected/nonempty or unrelated remote history, preserve all commits and reconcile it deliberately in this independent project before the first push. Do not force push, reset the remote branch, replace the remote repository or assume its initial commit is disposable. Owner review is needed if the purpose/ownership of unexpected remote content remains unclear.

When the owner has verified PRIVATE/non-fork and main is either absent or a proven ancestor of local main, they should review tracked source for secrets and inspect `git status`/destination again. Only the owner may then perform a manual new-repository push:

```powershell
git push -u origin main
```

No GitHub Release or Play submission was performed. No customer configuration/source changes were made. Final handoff records independent commits, local-only scope, remote status left unverified and the original-preservation qualification. Do not claim private-remote success without actual owner-supplied evidence.

CLI references: https://cli.github.com/manual/gh_repo_create and https://cli.github.com/manual/gh_auth_status

Future ports are manual selective source review only; record source commit, recheck Play/provider/security/update constraints, apply here and run regressions. No automatic merge/sync. See docs/PLAY_MAINTENANCE.md.
