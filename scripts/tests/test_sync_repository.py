import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import sync_repository as sync
import ci_sync


class ControlledExportTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.destination = self.root / 'customer'
        self.destination.mkdir()
        self.git('init', '--initial-branch=main')
        self.put('app/src/main/Old.kt', b'old\n')
        self.put('README.md', b'original\n')
        self.put('.github/workflows/customer.yml', b'name: Customer checks\n')
        self.commit()
        self.policy = {'files': ['README.md', 'distribution.properties'], 'trees': ['app/src/'],
                       'bootstrapBaseline': self.git('rev-parse', 'HEAD').strip()}
        self.payload = {'app/src/main/New.kt': (b'shared\n', 0o100644),
                        'README.md': (b'customer\n', 0o100644),
                        'distribution.properties': (b'distribution=customer\n', 0o100644)}

    def tearDown(self):
        self.temp.cleanup()

    def git(self, *args):
        return subprocess.check_output(['git', '-c', 'core.hooksPath=', '-c', 'user.name=Fixture',
               '-c', 'user.email=fixture@example.invalid', '-C', str(self.destination), *args], stderr=subprocess.PIPE).decode()

    def put(self, name, data):
        path = self.destination / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)

    def commit(self):
        self.git('add', '--all')
        self.git('commit', '-m', 'fixture')

    def plan(self, commit='1' * 40):
        return sync.plan_export(self.destination, commit, self.payload, self.policy)

    def established(self):
        sync.apply_export(self.destination, self.payload, self.plan())
        self.commit()

    def test_bootstrap_preserves_workflows_and_handles_add_modify_delete(self):
        plan = self.plan()
        self.assertTrue(plan['bootstrap'])
        self.assertEqual(plan['deletions'], ['app/src/main/Old.kt'])
        sync.apply_export(self.destination, self.payload, plan)
        self.assertFalse((self.destination / 'app/src/main/Old.kt').exists())
        self.assertEqual((self.destination / 'app/src/main/New.kt').read_bytes(), b'shared\n')
        self.assertEqual((self.destination / '.github/workflows/customer.yml').read_bytes(), b'name: Customer checks\n')

    def test_source_only_commit_does_not_create_empty_sync(self):
        self.established()
        plan = self.plan(commit='2' * 40)
        self.assertFalse(plan['changed'])
        before = (self.destination / sync.MANIFEST).read_bytes()
        sync.apply_export(self.destination, self.payload, plan)
        self.assertEqual(before, (self.destination / sync.MANIFEST).read_bytes())

    def test_followup_removes_only_owned_source(self):
        self.established()
        del self.payload['app/src/main/New.kt']
        self.payload['app/src/main/Next.kt'] = (b'next\n', 0o100644)
        plan = self.plan()
        self.assertEqual(plan['deletions'], ['app/src/main/New.kt'])
        sync.apply_export(self.destination, self.payload, plan)
        self.assertTrue((self.destination / '.github/workflows/customer.yml').exists())

    def test_dirty_destination_is_not_overwritten(self):
        self.put('README.md', b'owner work\n')
        with self.assertRaisesRegex(ValueError, 'local changes'):
            self.plan()

    def test_clean_but_independently_committed_drift_stops(self):
        self.established()
        self.put('README.md', b'independent change\n')
        self.commit()
        with self.assertRaisesRegex(ValueError, 'drift'):
            self.plan()

    def test_new_unmanaged_collision_is_not_overwritten(self):
        self.established()
        self.put('app/src/main/Extra.kt', b'owner addition\n')
        self.commit()
        self.payload['app/src/main/Extra.kt'] = (b'master addition\n', 0o100644)
        with self.assertRaisesRegex(ValueError, 'unmanaged'):
            self.plan()

    def test_unreviewed_initial_head_is_rejected(self):
        self.policy['bootstrapBaseline'] = '0' * 40
        with self.assertRaisesRegex(ValueError, 'baseline'):
            self.plan()

    def test_forged_manifest_cannot_claim_workflow(self):
        self.established()
        path = self.destination / sync.MANIFEST
        manifest = json.loads(path.read_text())
        manifest['files']['.github/workflows/customer.yml'] = {'sha256': '0' * 64, 'mode': 0o100644}
        path.write_text(json.dumps(manifest))
        self.commit()
        with self.assertRaisesRegex(ValueError, 'unmanaged'):
            self.plan()

    def test_partial_write_failure_restores_original_bytes(self):
        plan = self.plan()
        original = Path.replace
        calls = [0]
        def fail_once(path, target):
            calls[0] += 1
            if calls[0] == 2:
                raise OSError('fixture interruption')
            return original(path, target)
        with mock.patch.object(Path, 'replace', fail_once), self.assertRaises(OSError):
            sync.apply_export(self.destination, self.payload, plan)
        self.assertEqual((self.destination / 'README.md').read_bytes(), b'original\n')
        self.assertTrue((self.destination / 'app/src/main/Old.kt').exists())
        self.assertFalse((self.destination / sync.MANIFEST).exists())

    def test_ignored_file_created_after_plan_is_not_overwritten(self):
        self.put('.gitignore', b'app/src/main/New.kt\n')
        self.commit()
        self.policy['bootstrapBaseline'] = self.git('rev-parse', 'HEAD').strip()
        plan = self.plan()
        self.put('app/src/main/New.kt', b'private work\n')
        with self.assertRaisesRegex(ValueError, 'changed after'):
            sync.apply_export(self.destination, self.payload, plan)

    def test_platform_paths_and_case_collisions_are_rejected(self):
        for path in ('../outside', 'app/src/../../.git/config', 'app/src/CON.kt', 'C:/outside',
                     'app/src/aux.txt', 'app/src/bad?.kt', 'app/src/a\n.kt', '.GIT/config'):
            with self.subTest(path=path), self.assertRaises(ValueError):
                sync.contained(self.destination, path)
        self.payload['readme.md'] = (b'collision', 0o100644)
        with self.assertRaises(ValueError):
            self.plan()

    def test_symbolic_link_cannot_redirect_export(self):
        outside = self.root / 'outside'
        outside.mkdir()
        try:
            (self.destination / 'app/src/linked').symlink_to(outside, target_is_directory=True)
        except OSError:
            self.skipTest('Host account cannot create symbolic links')
        with self.assertRaisesRegex(ValueError, 'Symlinks'):
            sync.contained(self.destination, 'app/src/linked/key.txt')


class RunnerBoundaryTests(unittest.TestCase):
    def test_local_invocation_has_no_write_authority(self):
        with mock.patch.dict(os.environ, {}, clear=True), self.assertRaises(ValueError):
            ci_sync.require_hosted_workflow()

    def test_pull_requests_have_no_write_authority(self):
        env = {'GITHUB_ACTIONS': 'true', 'RUNNER_ENVIRONMENT': 'github-hosted',
               'GITHUB_REPOSITORY': sync.SOURCE, 'GITHUB_EVENT_NAME': 'pull_request',
               'GITHUB_REF': 'refs/heads/main',
               'GITHUB_WORKFLOW_REF': sync.SOURCE + '/.github/workflows/sync-youriptv.yml@refs/heads/main'}
        with mock.patch.dict(os.environ, env, clear=True), self.assertRaises(ValueError):
            ci_sync.require_hosted_workflow()
