import base64
import io
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from public_guard import audit_files, load_restricted_terms


class PublicGuardTests(unittest.TestCase):
    terms = ['violetwidget']

    def categories(self, files):
        return {x['category'] for x in audit_files(files, self.terms)['findings']}

    def test_safe_source_and_example_template_pass(self):
        result = audit_files({'app/src/main/Example.kt': b'package example',
                              'keystore.properties.example': b'storePassword=YOUR_STORE_PASSWORD'}, self.terms)
        self.assertTrue(result['passed'])

    def test_missing_restricted_config_fails_closed(self):
        self.assertFalse(audit_files({'README.md': b'ok'}, [])['passed'])
        with patch.dict(os.environ, {'SYNC_RESTRICTED_TERMS_B64': ''}):
            with self.assertRaises(ValueError): load_restricted_terms()

    def test_external_file_and_encoded_environment_load(self):
        with tempfile.TemporaryDirectory() as folder:
            config = Path(folder)/'terms.json'
            config.write_text(json.dumps(self.terms), encoding='utf-8')
            self.assertEqual(load_restricted_terms(config), self.terms)
        encoded = base64.b64encode(json.dumps(self.terms).encode()).decode()
        with patch.dict(os.environ, {'SYNC_RESTRICTED_TERMS_B64': encoded}):
            self.assertEqual(load_restricted_terms(), self.terms)

    def test_invalid_or_empty_config_has_no_values_in_error(self):
        for raw in ('', 'not-encoded', base64.b64encode(b'[]').decode(), base64.b64encode(b'{}').decode()):
            with self.subTest(raw_length=len(raw)), patch.dict(os.environ, {'SYNC_RESTRICTED_TERMS_B64': raw}):
                with self.assertRaises(ValueError) as caught: load_restricted_terms()
                self.assertNotIn('not-encoded', str(caught.exception))

    def test_restricted_content_case_and_encoded_forms_are_rejected(self):
        for payload in (b'VioletWidget', base64.b64encode(b'violetwidget'), 'violetwidget'.encode('utf-16-le')):
            self.assertIn('restricted_content', self.categories({'README.md': payload}))

    def test_restricted_filename_is_redacted(self):
        finding = audit_files({'docs/violetwidget-notes.md': b'x'}, self.terms)['findings']
        self.assertNotIn('violetwidget', json.dumps(finding))
        self.assertIn('<restricted>', finding[0]['path'])

    def test_private_and_generated_paths_fail(self):
        for path in ('release.jks', 'keystore.properties', 'local.properties', '.env.local',
                     'app/build/results.xml', 'artifacts/source.zip', 'private-evidence/report.md', 'user.sqlite'):
            with self.subTest(path=path):
                self.assertIn('private_or_generated_path', self.categories({path: b'x'}))

    def test_traversal_absolute_reserved_and_case_collision_fail(self):
        for path in ('../README.md', '/README.md', 'C:\\keys\\file', 'a/../b', 'a//b', 'a/NUL.txt', 'a/file.'):
            self.assertIn('invalid_path', self.categories({path: b'x'}))
        self.assertIn('case_collision', self.categories({'README.md': b'x', 'readme.md': b'x'}))

    def test_secret_detection_does_not_echo_the_secret(self):
        secret = ('gh'+'p_'+'a'*40).encode()
        report = audit_files({'config.txt': secret}, self.terms)
        self.assertIn('github_token', {x['category'] for x in report['findings']})
        self.assertNotIn(secret.decode(), json.dumps(report))

    def test_real_url_credentials_fail_but_fixture_placeholders_do_not(self):
        risky = ('https://source.example-host.org/get.php?username=reader&password='+'actual-value-927').encode()
        self.assertIn('literal_source_credential', self.categories({'config.txt': risky}))
        self.assertTrue(audit_files({'app/src/test/Fixture.kt': b'https://source.test/get.php?username=u&password=synthetic-value'}, self.terms)['passed'])
        self.assertTrue(audit_files({'README.md': b'https://provider.org/get.php?username=USER&password=PASS'}, self.terms)['passed'])

    def test_wrapper_archive_is_checked_without_extracting(self):
        def archive(name, content):
            stream = io.BytesIO()
            with zipfile.ZipFile(stream, 'w') as result: result.writestr(name, content)
            return stream.getvalue()
        path = 'gradle/wrapper/gradle-wrapper.jar'
        self.assertTrue(audit_files({path: archive('org/example/Wrapper.class', b'code')}, self.terms)['passed'])
        self.assertIn('private_wrapper_member', self.categories({path: archive('release.jks', b'key')}))
        self.assertIn('unsafe_wrapper_content', self.categories({path: archive('data.txt', b'violetwidget')}))


if __name__ == '__main__': unittest.main()
