# Independent repository separation

| | Customer | Google Play |
|---|---|---|
| Remote | https://github.com/youriptvus/YourIPTV | https://github.com/youriptvus/YourIPTV-GooglePlay |
| Local | D:\APK\Repository\YourIPTV | D:\APK\Repository\YourIPTV-GooglePlay |
| Task access | Read only | Implementation |
| History | Existing unchanged | Fresh independent Git |
| Updates | Customer updater unchanged | Play delivery |
| Package | com.youriptv.app | com.youriptv.app |
| Version |1.0.9/code44 unchanged |1.0.9/proposed45 |

Not a branch/worktree/fork/shared Git directory. Same Android package is intentional: normally no side-by-side install, and compatible certificates/higher code needed for updates. No automatic synchronization.

## Remote setup pending authorized authentication

The independent root repository already has origin configured for the exact new URL above, for fetch and push. Do not add another origin. No usable authenticated youriptvus account was available initially; obsolete comp6062 must not be used. An intended origin is not evidence the server repository exists, is private, or has no history.

A preexisting nested checkout at `YourIPTV-GooglePlay/` inside this source root was found, ignored and preserved without alteration. Its initial-history evidence does not prove who created the remote or its visibility. Do not treat that nested checkout as this project's source/Git root or upload it. An existing remote may already have commits that must be reconciled without force push.

### Authenticate and inspect before creating or pushing

```powershell
gh auth login --hostname github.com --git-protocol https --web
gh auth status --hostname github.com
gh api user --jq .login
```

Require successful authentication as **youriptvus** before continuing. Do not print tokens. If login remains obsolete/invalid, stop the remote steps and finish local preparation. A 404 can also conceal a private repository that the current token cannot access; it is not proof that a repository name is available.

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

When remote is verified PRIVATE/non-fork and main is either absent or a proven ancestor of local main, review tracked source for secrets and inspect `git status`/destination again. Only then perform the already-authorized new-repository push:

```powershell
git push -u origin main
```

No GitHub Release or Play submission. No customer configuration/source changes. Final handoff records independent initial/final commits, actual remote status/visibility, original before/after preservation and any unresolved authentication/history conflict. Do not claim private-remote success until verified.

CLI references: https://cli.github.com/manual/gh_repo_create and https://cli.github.com/manual/gh_auth_status

Future ports are manual selective source review only; record source commit, recheck Play/provider/security/update constraints, apply here and run regressions. No automatic merge/sync. See docs/PLAY_MAINTENANCE.md.
