"""Host-only regression checks for compiled transport-policy inspection."""
import importlib.util
from pathlib import Path
from types import SimpleNamespace
import unittest
import xml.etree.ElementTree as ET

script = Path(__file__).resolve().parents[1] / 'verify_play_candidate.py'
spec = importlib.util.spec_from_file_location('verify_play_candidate', script)
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)


class CompiledTransportPolicyTest(unittest.TestCase):
    def inspect(self, xml):
        return verifier.inspect_network_security_xml({'default': ET.fromstring(xml)})['checks']

    def test_complete_explicit_user_source_policy_passes(self):
        self.assertTrue(all(self.inspect('<network-security-config><base-config cleartextTrafficPermitted="true"/></network-security-config>').values()))

    def test_blocking_or_omitting_user_source_policy_fails(self):
        for base in ('', '<base-config/>', '<base-config cleartextTrafficPermitted="false"/>'):
            self.assertFalse(all(self.inspect('<network-security-config>' + base + '</network-security-config>').values()))

    def test_domain_exception_and_debug_override_fail(self):
        for override in ('<domain-config cleartextTrafficPermitted="false"><domain>127.0.0.1</domain></domain-config>', '<debug-overrides/>'):
            self.assertFalse(all(self.inspect('<network-security-config><base-config cleartextTrafficPermitted="true"/>' + override + '</network-security-config>').values()))

    def test_version_qualified_block_cannot_hide_behind_compatible_default(self):
        safe = ET.fromstring('<network-security-config><base-config cleartextTrafficPermitted="true"/></network-security-config>')
        unsafe = ET.fromstring('<network-security-config><base-config cleartextTrafficPermitted="false"/></network-security-config>')
        result = verifier.inspect_network_security_xml({'default': safe, 'v28': unsafe})
        self.assertFalse(all(result['checks'].values()))

    def test_aapt_binary_boolean_is_decoded_before_policy_validation(self):
        root = verifier.parse_aapt_xml('E: network-security-config (line=2)\n  E: base-config (line=3)\n    A: cleartextTrafficPermitted=(type 0x12)0xffffffff\n')
        self.assertTrue(all(verifier.inspect_network_security_xml({'default': root})['checks'].values()))

    def test_manifest_must_explicitly_enable_user_sources_and_reference_policy(self):
        args = SimpleNamespace(application_id='com.youriptv.app', version_code=48, version_name='1.0.9', min_sdk=23, target_sdk=36)
        for attributes, expected in (('android:usesCleartextTraffic="true" android:networkSecurityConfig="@xml/network_security_config"', True), ('', False), ('android:usesCleartextTraffic="false"', False)):
            root = ET.fromstring('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.youriptv.app"><uses-sdk/><application ' + attributes + '/></manifest>')
            result = verifier.inspect_manifest(root, args)
            self.assertEqual(expected, result['checks']['user_provided_http_enabled'])
            self.assertEqual(expected, result['checks']['network_security_config_present'])


class SupportedSdkBackupPolicyTest(unittest.TestCase):
    def protected_documents(self):
        domains = verifier.BACKUP_DOMAINS | verifier.DEVICE_BACKUP_DOMAINS
        excludes = ''.join('<exclude domain="' + domain + '" path="."/>' for domain in domains)
        return {'legacy24': ET.fromstring('<full-backup-content>' + excludes + '</full-backup-content>'),
                'modern': ET.fromstring('<data-extraction-rules><cloud-backup>' + excludes +
                                       '</cloud-backup><device-transfer>' + excludes +
                                       '</device-transfer></data-extraction-rules>')}

    def test_pruned_api23_variant_is_allowed_only_when_api23_is_unsupported(self):
        documents = self.protected_documents()
        self.assertTrue(all(verifier.inspect_backup_xml(documents, minimum_sdk=24)['checks'].values()))
        self.assertFalse(all(verifier.inspect_backup_xml(documents, minimum_sdk=23)['checks'].values()))

    def test_api24_device_credentials_and_api31_transfer_cannot_be_reincluded(self):
        for document, parent, domain in [('legacy24', None, 'device_database'),
                                          ('modern', 'device-transfer', 'sharedpref')]:
            documents = self.protected_documents()
            node = documents[document] if parent is None else documents[document].find(parent)
            node.remove(next(entry for entry in node.findall('exclude') if entry.get('domain') == domain))
            self.assertFalse(all(verifier.inspect_backup_xml(documents, minimum_sdk=24)['checks'].values()))


if __name__ == '__main__':
    unittest.main()
