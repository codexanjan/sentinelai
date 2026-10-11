# Repository privacy

Personal support addresses and owner Firebase client configuration have been replaced by empty configuration templates. Commit author personal names/email addresses and matching historical file contents are removed from rewritten branches and release tags.

Firebase browser/Android API keys and OAuth client IDs are public client configuration, not administrator credentials. The live website and released APK still require their client configuration to authenticate; GitHub source templates do not include it. This cleanup does not revoke or rotate authentication settings.

Fill configuration in a private build/deployment copy. Never commit personal support addresses, service-account files, provider secrets, signing keys/passwords, CLI credentials or actual vault data. Local vault and demo modes work with the empty templates.

Existing clones and GitHub cached views may retain old history. Use a fresh clone after this history rewrite; do not merge an old checkout back into the cleaned repository.
