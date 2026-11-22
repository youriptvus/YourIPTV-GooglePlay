import argparse
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
import warnings
import zipfile
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import prepare_release as release


class ReleasePreparationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.home = Path(self.temp.name)
        self.root = self.home / 'source'
        self.root.mkdir()
        self.config = release.configuration(release.DEFAULT_CONFIG)
        (self.root / 'version.properties').write_text('versionName=1.1.2\nversionCode=53\n')
        self.terms = self.home / 'terms.private.json'
        self.terms.write_text('["violetwidget"]')
        self.notes = self.home / 'notes.txt'
        self.notes.write_text('Fixture release notes')
        self.args = argparse.Namespace(version='1.1.2', version_code=53, highest_play_code=52,
            previous_customer_code=52, play_history_confirmed=True, restricted_terms_file=self.terms,
            output_root=self.home / 'releases', release_notes=self.notes,
            customer_apk_url='https://github.com/youriptvus/YourIPTV/releases/download/v1.1.2/YourIPTV.apk',
            authorize_release=True, sdk=None, play_signing_properties=self.home / 'upload.private.properties',
            customer_signing_properties=self.home / 'customer.private.properties')
        self.args.play_signing_properties.write_text('fixture-only')
        self.args.customer_signing_properties.write_text('fixture-only')
        self.provenance = {'sourceCommit': '1' * 40, 'sourceFiles': 1, 'sourceAuditPassed': True}

    def tearDown(self):
        self.temp.cleanup()

    def plan(self):
        return release.release_plan(self.args, self.config, root=self.root,
                                   provenance=lambda *_: self.provenance)[0]

    def test_plan_has_both_variants_but_creates_no_release_files(self):
        plan = self.plan()
        self.assertEqual(plan['versionCode'], 53)
        self.assertIn('-Pdistribution=play', plan['buildCommands']['play'])
        self.assertIn('bundleRelease', plan['buildCommands']['play'])
        self.assertNotIn('bundleRelease', plan['buildCommands']['customer'])
        self.assertFalse(self.args.output_root.exists())

    def test_android_resource_case_is_preserved_without_extracting(self):
        archive = self.home / 'case-sensitive.apk'
        with zipfile.ZipFile(archive, 'w') as stream:
            stream.writestr('res/a.png', b'first fixture')
            stream.writestr('res/A.png', b'second fixture')
        self.assertTrue(release.artifact_payload_audit(archive, ['violetwidget'])['passed'])

    def test_exact_duplicate_archive_member_is_rejected(self):
        archive = self.home / 'duplicate.apk'
        with warnings.catch_warnings():
            warnings.simplefilter('ignore', UserWarning)
            with zipfile.ZipFile(archive, 'w') as stream:
                stream.writestr('res/a.png', b'first fixture')
                stream.writestr('res/a.png', b'second fixture')
        with self.assertRaises(release.ReleaseError):
            release.artifact_payload_audit(archive, ['violetwidget'])

    def test_published_baseline_is_rejected_before_any_build(self):
        self.args.version = '1.1.1'
        self.args.version_code = 52
        with self.assertRaises(release.ReleaseError): self.plan()
        self.assertFalse(self.args.output_root.exists())

    def test_play_history_and_unused_code_are_required(self):
        self.args.play_history_confirmed = False
        with self.assertRaises(release.ReleaseError): self.plan()
        self.args.play_history_confirmed = True
        self.args.highest_play_code = 53
        with self.assertRaises(release.ReleaseError): self.plan()

    def test_requested_version_cannot_override_source_version(self):
        self.args.version_code = 54
        with self.assertRaises(release.ReleaseError): self.plan()

    def test_canonical_url_does_not_accept_aab_wrong_alias_or_query(self):
        for url in ('https://example.invalid/file.apk',
                    self.args.customer_apk_url.replace('YourIPTV.apk', 'YourIPTV-GitHub-1.1.2.apk'),
                    self.args.customer_apk_url + '?download=1',
                    self.args.customer_apk_url.replace('.apk', '.aab')):
            with self.subTest(url=url), self.assertRaises(release.ReleaseError):
                release.canonical_customer_url(url, '1.1.2')

    def test_source_notes_and_private_config_fail_closed(self):
        self.notes.write_text('violetwidget')
        with self.assertRaises(release.ReleaseError): self.plan()
        self.notes.write_text('Fixture notes')
        self.terms.write_text('[]')
        with self.assertRaises(ValueError): self.plan()

    def test_existing_version_folder_is_immutable(self):
        target = self.args.output_root / '1.1.2'
        target.mkdir(parents=True)
        marker = target / 'owner-file.txt'
        marker.write_bytes(b'preserve')
        with self.assertRaises(release.ReleaseError): self.plan()
        self.assertEqual(marker.read_bytes(), b'preserve')

    def test_metadata_uses_actual_bytes_and_legacy_updater_schema(self):
        plan = self.plan()
        artifact = self.home / 'YourIPTV-GitHub-1.1.2.apk'
        artifact.write_bytes(b'fixture apk bytes')
        record = release.artifact_record(artifact, plan, self.config, 'customer-github', 'fixture-date')
        self.assertEqual(record['sha256'], hashlib.sha256(artifact.read_bytes()).hexdigest())
        self.assertEqual(record['size'], artifact.stat().st_size)
        self.assertEqual(record['artifactType'], 'apk')
        self.assertEqual(record['publicArtifactAlias'], 'YourIPTV.apk')
        update = release.updater_record(record, 52, self.args.customer_apk_url)
        self.assertEqual(set(update), {'versionCode', 'versionName', 'apk', 'size', 'sha256', 'previousVersionCode'})
        self.assertEqual(update['sha256'], record['sha256'])
        self.assertNotIn('aab', update['apk'])

    def test_output_lock_and_staging_failure_preserve_previous_versions(self):
        previous = self.args.output_root / '1.1.1'
        previous.mkdir(parents=True)
        (previous / 'preserve.txt').write_bytes(b'published bytes')
        with self.assertRaises(OSError):
            with release.exclusive_stage(self.args.output_root, '1.1.2') as stage:
                (stage / 'partial.txt').write_bytes(b'partial')
                raise OSError('fixture failure')
        self.assertEqual((previous / 'preserve.txt').read_bytes(), b'published bytes')
        self.assertFalse((self.args.output_root / '1.1.2').exists())
        self.assertEqual([p.name for p in self.args.output_root.iterdir()], ['1.1.1'])

    def test_prepare_requires_owner_authorization(self):
        self.args.authorize_release = False
        with self.assertRaises(release.ReleaseError):
            release.prepare(self.args, self.plan(), self.config, {}, ['violetwidget'], root=self.root)

    def test_failed_second_build_does_not_activate_a_pair(self):
        plan = self.plan()
        calls = []
        def runner(*args, **kwargs):
            calls.append(args)
            if len(calls) == 2: raise release.ReleaseError('fixture build failure')
        tools = {'java': self.home / 'jdk/bin/java'}
        with self.assertRaises(release.ReleaseError):
            release.prepare(self.args, plan, self.config, tools, ['violetwidget'], root=self.root, runner=runner)
        self.assertEqual(len(calls), 2)
        self.assertFalse((self.args.output_root / '1.1.2').exists())

    def test_unsigned_or_failed_verification_does_not_activate_a_pair(self):
        plan = self.plan()
        for relative in ('app/build/play/outputs/bundle/release/app-release.aab',
                         'app/build/play/outputs/apk/release/app-release.apk',
                         'app/build/customer/outputs/apk/release/app-release.apk'):
            path = self.root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b'non-shipping fixture')
        tools = {'java': self.home / 'jdk/bin/java'}
        with mock.patch.object(release, 'source_provenance', return_value=self.provenance):
            with self.assertRaises(release.ReleaseError):
                release.prepare(self.args, plan, self.config, tools, ['violetwidget'], root=self.root,
                    runner=lambda *a, **k: '', verifier=lambda *a, **k: {'passed': False})
        self.assertFalse((self.args.output_root / '1.1.2').exists())
