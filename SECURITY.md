# Security Policy

## Supported Versions

| Version | Supported |
|---|---|
| Latest release | ✅ |
| Older releases | ❌ |

We only maintain the latest release. Please update before reporting a vulnerability.

## Reporting a Vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Report security issues privately via [GitHub's private vulnerability reporting](https://github.com/woliveiras/palabrita/security/advisories/new).

Include:

- A description of the vulnerability and its potential impact
- Steps to reproduce or a proof-of-concept
- Affected version(s)
- Any suggested mitigations (optional)

We will acknowledge receipt within **72 hours** and aim to provide a fix or mitigation within **14 days** for critical issues.

## Scope

This project runs entirely on-device. The primary security surface areas are:

- **LLM prompt handling** — prompt injection via crafted game input
- **File I/O** — model files downloaded from external sources
- **Dependencies** — third-party libraries with known CVEs

## Out of Scope

- Issues in third-party dependencies should be reported to the respective upstream maintainers
- Theoretical vulnerabilities without a realistic attack vector
