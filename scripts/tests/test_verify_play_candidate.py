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

    def test_complete_production_deny_policy_passes(self):
        self.assertTrue(all(self.inspect('<network-security-config><base-config cleartextTrafficPermitted="false"/></network-security-config>').values()))

    def test_allowing_or_omitting_cleartext_policy_fails(self):
        for base in ('', '<base-config/>', '<base-config cleartextTrafficPermitted="true"/>'):
            self.assertFalse(all(self.inspect('<network-security-config>' + base + '</network-security-config>').values()))

    def test_domain_exception_and_debug_override_fail(self):
        for override in ('<domain-config cleartextTrafficPermitted="true"><domain>127.0.0.1</domain></domain-config>', '<debug-overrides/>'):
            self.assertFalse(all(self.inspect('<network-security-config><base-config cleartextTrafficPermitted="false"/>' + override + '</network-security-config>').values()))

    def test_unsafe_version_qualified_policy_cannot_hide_behind_safe_default(self):
        safe = ET.fromstring('<network-security-config><base-config cleartextTrafficPermitted="false"/></network-security-config>')
        unsafe = ET.fromstring('<network-security-config><base-config cleartextTrafficPermitted="true"/></network-security-config>')
        result = verifier.inspect_network_security_xml({'default': safe, 'v28': unsafe})
        self.assertFalse(all(result['checks'].values()))

    def test_aapt_binary_boolean_is_decoded_before_policy_validation(self):
        root = verifier.parse_aapt_xml('E: network-security-config (line=2)\n  E: base-config (line=3)\n    A: cleartextTrafficPermitted=(type 0x12)0x00000000\n')
        self.assertTrue(all(verifier.inspect_network_security_xml({'default': root})['checks'].values()))

    def test_manifest_must_explicitly_disable_cleartext_and_reference_policy(self):
        args = SimpleNamespace(application_id='com.youriptv.app', version_code=48, version_name='1.0.9', min_sdk=23, target_sdk=36)
        for attributes, expected in (('android:usesCleartextTraffic="false" android:networkSecurityConfig="@xml/network_security_config"', True), ('', False), ('android:usesCleartextTraffic="true"', False)):
            root = ET.fromstring('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.youriptv.app"><uses-sdk/><application ' + attributes + '/></manifest>')
            result = verifier.inspect_manifest(root, args)
            self.assertEqual(expected, result['checks']['android_cleartext_traffic_disabled'])
            self.assertEqual(expected, result['checks']['network_security_config_present'])


if __name__ == '__main__':
    unittest.main()
