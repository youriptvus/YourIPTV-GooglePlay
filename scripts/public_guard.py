"""Fail-closed publication checks with private, externally supplied restricted terms.

Findings contain categories and redacted relative paths, never matching values.
Pattern checks supplement the controlled export policy; they are not proof that
every possible unknown credential or image caption has been identified.
"""
from __future__ import annotations

import argparse
import base64
import html
import io
import json
import os
from pathlib import Path
import re
from typing import Mapping, Sequence
import unicodedata
from urllib.parse import parse_qsl, unquote, urlsplit
import zipfile


_PRIVATE_COMPONENTS = {
    '.git', '.gradle', '.kotlin', '.idea', '.cxx', '.externalnativebuild',
    'node_modules', '__pycache__', 'build', 'artifacts', 'captures',
    'private', 'private-evidence', 'qa_evidence', 'qa_backup', 'test-results',
    'backups', '.secrets', '.credentials',
}
_PRIVATE_NAMES = {'local.properties', 'keystore.properties', 'test-config.local.properties',
                  '.env', 'id_rsa', 'id_ed25519', 'credentials.json', 'service-account.json'}
_PRIVATE_SUFFIXES = {'.jks', '.keystore', '.p12', '.pfx', '.pem', '.key', '.der',
                     '.apk', '.aab', '.apks', '.zip', '.7z', '.rar', '.db', '.sqlite',
                     '.sqlite3', '.log', '.hprof'}
_SECRET_PATTERNS = {
    'private_key': re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH |ENCRYPTED )?PRIVATE KEY-----'),
    'github_token': re.compile(rb'\b(?:gh[pousr]_[A-Za-z0-9_]{30,}|github_pat_[A-Za-z0-9_]{40,})\b'),
    'cloud_access_key': re.compile(rb'\b(?:AKIA|ASIA)[A-Z0-9]{16}\b'),
    'google_api_key': re.compile(rb'\bAIza[0-9A-Za-z_-]{35}\b'),
}
_PLACEHOLDERS = {'u', 'p', 'user', 'username', 'pass', 'password', 'secret', 'test',
                 'demo', 'example', 'placeholder', 'redacted', 'changeme', 'change_me',
                 'replace_me', 'not-a-secret', 'not_a_secret', 'none', 'null', 'fake'}
_WRAPPER = 'gradle/wrapper/gradle-wrapper.jar'


def _terms(values: Sequence[str], *, required: bool = True) -> list[str]:
    if isinstance(values, (str, bytes)) or not isinstance(values, Sequence):
        raise ValueError('Restricted-term configuration must be a JSON list of strings.')
    result, seen = [], set()
    for value in values:
        if not isinstance(value, str) or not value.strip() or len(value) > 256:
            raise ValueError('Restricted-term configuration contains an invalid entry.')
        value = unicodedata.normalize('NFKC', value.strip())
        if value.casefold() not in seen:
            result.append(value)
            seen.add(value.casefold())
    if required and not result:
        raise ValueError('Private restricted-term configuration is required and must not be empty.')
    return result


def load_restricted_terms(path=None, env_name='SYNC_RESTRICTED_TERMS_B64', required=True) -> list[str]:
    """Load an external UTF-8 JSON list, or its base64 JSON environment equivalent.

    File configuration must resolve outside this source tree. Errors deliberately
    omit input values and paths because either can contain confidential material.
    """
    try:
        if path is not None:
            source = Path(path).resolve(strict=True)
            if source.is_relative_to(Path(__file__).resolve().parents[1]):
                raise ValueError('Private configuration must remain outside the source tree.')
            if source.stat().st_size > 65536:
                raise ValueError('Restricted-term configuration is too large.')
            raw = source.read_bytes()
        else:
            value = os.environ.get(env_name, '')
            if not value.strip():
                return _terms([], required=required)
            raw = base64.b64decode(value, validate=True)
        if len(raw) > 65536:
            raise ValueError('Restricted-term configuration is too large.')
        parsed = json.loads(raw.decode('utf-8-sig'))
    except (OSError, UnicodeError, json.JSONDecodeError, ValueError, TypeError):
        raise ValueError('Private restricted-term configuration is missing, unreadable or invalid.') from None
    return _terms(parsed, required=required)


def _normal_path(path: str) -> str | None:
    if not isinstance(path, str) or not path or any(ord(c) < 32 for c in path):
        return None
    path = path.replace('\\', '/')
    if path.startswith('/') or re.match(r'^[A-Za-z]:', path):
        return None
    parts = path.split('/')
    if any(p in {'', '.', '..'} or p[-1:] in {' ', '.'} or re.search(r'[:<>"|?*]', p) for p in parts):
        return None
    if any(re.fullmatch(r'(?:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\..*)?', p, re.I) for p in parts):
        return None
    return '/'.join(parts)


def _redact_path(path: str | None, terms: Sequence[str]) -> str:
    if path is None:
        return '<invalid-path>'
    path = unicodedata.normalize('NFKC', path)
    for term in sorted(terms, key=len, reverse=True):
        path = re.sub(re.escape(term), '<restricted>', path, flags=re.I)
    path = re.sub(r'[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}', '<private>', path)
    path = re.sub(r'\b(?:gh[pousr]_[A-Za-z0-9_]{30,}|github_pat_[A-Za-z0-9_]{40,})\b', '<secret>', path)
    return path


def _private_path(path: str) -> bool:
    parts = path.casefold().split('/')
    name = parts[-1]
    if any(part in _PRIVATE_COMPONENTS for part in parts):
        return True
    if name in _PRIVATE_NAMES or name.startswith('.env.') and name != '.env.example':
        return True
    if name.endswith('.local.properties') or name.endswith('.local.json'):
        return True
    if any(name.endswith(suffix) or suffix + '.' in name for suffix in _PRIVATE_SUFFIXES):
        return True
    return name.endswith('.jar') and path != _WRAPPER


def _placeholder(value: str) -> bool:
    value = unquote(value).strip('"\'<> ').casefold()
    return (not value or value in _PLACEHOLDERS or value.startswith(('your_', 'example_', 'replace_', 'dummy_', '${', '{', '%'))
            or all(c in '*._-' for c in value) or 'getenv(' in value or 'getproperty(' in value)


def _literal_url_credential(text: str) -> bool:
    for match in re.finditer(r'https?://[^\s"\'<>]+', html.unescape(text), re.I):
        try:
            url = urlsplit(match.group())
            host = (url.hostname or '').casefold()
            if host in {'localhost', '127.0.0.1', '::1', 'example.com', 'example.org', 'example.net'} or host.endswith(('.test', '.invalid', '.example')):
                continue
            values = [value for key, value in parse_qsl(url.query) if key.casefold() in {'password', 'passwd', 'access_token', 'api_key', 'apikey'}]
            if url.password is not None:
                values.append(url.password)
            if any(not _placeholder(value) and '$' not in value and len(value) >= 6 for value in values):
                return True
        except ValueError:
            continue
    return False


def _restricted_content(data: bytes, terms: Sequence[str]) -> bool:
    text = unicodedata.normalize('NFKC', html.unescape(data.decode('utf-8', errors='ignore'))).casefold()
    for term in terms:
        if term.casefold() in text:
            return True
        for form in {term, term.casefold(), term.upper(), term.title()}:
            if base64.b64encode(form.encode('utf-8')) in data or form.encode('utf-16-le') in data:
                return True
    return False


def audit_files(files: Mapping[str, bytes], restricted_terms: Sequence[str]) -> dict:
    """Check supplied export bytes. The caller must resolve symlinks safely first."""
    findings, seen = [], set()
    try:
        terms = _terms(restricted_terms)
    except ValueError:
        return {'passed': False, 'files_checked': 0,
                'findings': [{'category': 'restricted_configuration_required', 'path': '<configuration>'}]}

    def finding(category, path):
        row = {'category': category, 'path': _redact_path(path, terms)}
        if row not in findings:
            findings.append(row)

    for original, data in files.items():
        path = _normal_path(original)
        if path is None:
            finding('invalid_path', None)
            continue
        folded = unicodedata.normalize('NFKC', path).casefold()
        if folded in seen:
            finding('case_collision', path)
        seen.add(folded)
        if _private_path(path):
            finding('private_or_generated_path', path)
        if any(t.casefold() in folded for t in terms):
            finding('restricted_path', path)
        if not isinstance(data, bytes):
            finding('invalid_file_data', path)
            continue
        if _restricted_content(data, terms):
            finding('restricted_content', path)
        for category, pattern in _SECRET_PATTERNS.items():
            if pattern.search(data):
                finding(category, path)
        text = data.decode('utf-8', errors='ignore')
        if _literal_url_credential(text):
            finding('literal_source_credential', path)
        if path == _WRAPPER:
            try:
                with zipfile.ZipFile(io.BytesIO(data)) as archive:
                    infos = archive.infolist()
                    if len(infos) > 5000 or sum(i.file_size for i in infos) > 32 * 1024 * 1024:
                        finding('unsafe_wrapper_archive', path)
                        continue
                    for item in infos:
                        if item.is_dir():
                            continue
                        member = _normal_path(item.filename)
                        if member is None or _private_path(member) or (item.external_attr >> 16) & 0o170000 == 0o120000:
                            finding('private_wrapper_member', path)
                        payload = archive.read(item)
                        if _restricted_content(payload, terms) or any(p.search(payload) for p in _SECRET_PATTERNS.values()):
                            finding('unsafe_wrapper_content', path)
            except (zipfile.BadZipFile, OSError, RuntimeError, ValueError):
                finding('invalid_wrapper_archive', path)
    return {'passed': not findings, 'files_checked': len(files), 'findings': findings}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--restricted-terms-file', type=Path)
    parser.add_argument('files', nargs='+', type=Path, help='Files relative to the current directory; no directory recursion.')
    args = parser.parse_args(argv)
    try:
        terms = load_restricted_terms(args.restricted_terms_file)
        root = Path.cwd().resolve()
        material = {}
        for candidate in args.files:
            if candidate.is_symlink():
                raise ValueError('Symlink inputs are not permitted.')
            resolved = candidate.resolve(strict=True)
            if not resolved.is_relative_to(root) or not resolved.is_file():
                raise ValueError('Inputs must be regular files under the current directory.')
            material[resolved.relative_to(root).as_posix()] = resolved.read_bytes()
        result = audit_files(material, terms)
    except (OSError, ValueError):
        result = {'passed': False, 'findings': [{'category': 'invalid_guard_input', 'path': '<input>'}]}
    print(json.dumps(result, indent=2))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
