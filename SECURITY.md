# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.1.x   | Yes       |
| 1.0.x   | Security fixes only |

## Token injection boundary

`RetrofitKit` injects a bearer token only when the request target matches the
exact `baseUrl` origin: scheme, host, and effective port. Additional trusted
origins must be supplied explicitly through `allowedTokenOrigins`. An allowlist
entry may contain only scheme, host, and port; paths, queries, fragments, and
user information are rejected.

Token injection is HTTPS-only by default. For a cleartext `http://` request,
the interceptor does not invoke the token provider and does not attach an
Authorization header. `allowedTokenOrigins` rejects HTTP entries, and a
`tokenProvider` with an HTTP `baseUrl` fails initialization unless the caller
explicitly sets `allowInsecureTokenOrigins = true`. That switch exists only for
local debugging and sends the bearer token in cleartext; it must remain disabled
in release builds.

Requests to a different host, port, or scheme do not receive the token and do
not invoke the token provider. Callers that set `Authorization` themselves keep
their value.

## Reporting a Vulnerability

Please do **not** open a public issue for security vulnerabilities.

Report privately via:

- GitHub Security Advisory: repository page -> Security -> Report a vulnerability

Include in your report:

- Vulnerability type and impact
- Reproduction steps (PoC preferred)
- Affected versions
- Suggested fix (optional)

## Commitment

- Acknowledge receipt within 48 hours
- Provide a fix timeline after triage
- Credit the reporter in the release notes (unless anonymity is requested)
