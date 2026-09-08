# Releasing Mockatcha

Use this guide in `instanto-io/mockatcha`, the release repository. The separate
private working repository is not a publication source. A repository may remain
private while its release setup is prepared; changing its visibility does not
publish Maven artifacts.

## What is published

All artifacts use the `io.instanto` group and the same release version:

| Artifact | Published files |
| --- | --- |
| `mockatcha` | Parent POM |
| `mockatcha-core` | POM, library jar, sources jar, Javadoc jar |
| `mockatcha-bdd` | POM, library jar, sources jar, Javadoc jar |
| `mockatcha-dom` | POM, library jar, sources jar, Javadoc jar |
| `mockatcha-webapp-testkit` | POM, library jar, sources jar, Javadoc jar |
| `teavm-rule-support` | POM, library jar, sources jar, Javadoc jar |

The two example modules still build and test, but are excluded from Central.
The parent is published because consumers need it to resolve the module POMs.
Shared licence, developer and source-control metadata is inherited by each module.

The `release` profile attaches sources and Javadoc during `package`, signs
artifacts and POMs during `verify`, and builds and uploads the Central bundle
during `deploy`. It rejects snapshot project versions and snapshot dependencies.
The publisher creates MD5, SHA-1, SHA-256 and SHA-512 checksum files. OpenPGP
signatures are separate `.asc` files, not signatures embedded by `jarsigner`.

## Before the first release

1. Verify publishing rights to `io.instanto` in the Central Portal. If using DNS
   verification, add the Portal's TXT record to `instanto.io`.
2. Create a Central publishing token. Its username and password are separate from
   your GitHub credentials. Put them in your Maven settings under server ID
   `central`, or supply `CENTRAL_TOKEN_USERNAME` and `CENTRAL_TOKEN_PASSWORD` to
   the environment-based settings template in `.github/maven-central-settings.xml`.
3. Choose an OpenPGP release-signing key and publish its public key as described
   in Sonatype's signing guide. Keep the private key outside the repository.
4. Install Java 21 or later, Maven 3.9.2 or later in the 3.9 series (or use the wrapper), GnuPG and the
   browsers required by the tests. The default runner needs `chrome` on `PATH`.

Use `gpg-agent` locally. For unattended signing, provide the passphrase through
`MAVEN_GPG_PASSPHRASE`. Select the intended key with `-Dgpg.keyname=FINGERPRINT`.
Never put the passphrase in a command, POM or committed settings file.

The upload workflow uses the plugin's Java-based signer with
`-Dgpg.signer=bc`, providing `MAVEN_GPG_KEY`, `MAVEN_GPG_KEY_FINGERPRINT` and
`MAVEN_GPG_PASSPHRASE` as protected environment secrets. Create a `maven-central`
GitHub environment with required reviewers and put these three secrets plus the
two Central token values there. YAML alone does not enable reviewer protection.
Allow the workflow to run from `main`; it checks out the release branch separately.
The existing build workflow publishes GitHub snapshots from main; it does not
publish Central releases.

## Prepare a version

In GitHub Actions, run **Prepare release branch** from `main`. Supply a release
version and a next development version, both as `major.minor.patch` without
suffixes. For example, `0.1.0` and `0.1.1` require main to currently be
`0.1.0-SNAPSHOT` and produce:

- `release/0.1.0`, with all POM versions set to `0.1.0`;
- `main`, with all POM versions set to `0.1.1-SNAPSHOT`.

Both branches start from the same checked snapshot commit. Release preparation
sets the release POM's SCM reference to `release/0.1.0`; main keeps `HEAD`.
The push is atomic and does not force changes. A concurrent main update or an
existing release branch stops preparation. Branch protections must allow the
approved automation identity to make this push; the workflow does not bypass them.
Preparation does not upload artifacts. GitHub may not trigger ordinary push CI
for commits made with `GITHUB_TOKEN`; the upload workflow runs the release tests
itself rather than assuming such a run occurred.

Review the release branch and its documentation before uploading. Corrections
belong on that branch and must keep its release version. A released branch should
be protected against further changes; use a new release version for later fixes.

For manual preparation, the equivalent Maven version commands are below. Run
them on a new release branch, then update main separately to the next snapshot.

Work from a clean checkout. Choose an unused non-snapshot version; `0.1.0` below
is an example, not an announcement that it has been published.

```sh
./mvnw org.codehaus.mojo:versions-maven-plugin:2.19.1:set \
  -DnewVersion=0.1.0 -DprocessAllModules=true -DgenerateBackupPoms=false
./mvnw org.codehaus.mojo:versions-maven-plugin:2.19.1:set-scm-tag \
  -DnewTag=release/0.1.0 -DgenerateBackupPoms=false
./mvnw -Prelease validate
```

Review the changed POMs, update documented dependency versions and release notes,
and confirm `scm/tag` names the release branch.
Do not edit only the root version: every child's parent reference must match.

## Check a release locally

```sh
./mvnw -Prelease clean verify -Dgpg.keyname=FINGERPRINT
./mvnw -pl mockatcha-dom -am -Dmockatcha.test.browser=browser-firefox test
```

Both commands must pass before release. `verify` leaves signed POMs and jars in
the modules' target directories without invoking the publisher. Inspect the
library, sources and Javadoc jars before upload. The example modules also build
locally, but the publisher excludes them.

Use `verify`, not `deploy`, for a local-only check. The pinned publishing plugin
does not provide a verified bundle-only path in this setup. In particular, do not
treat `skipPublishing` as a checked way to produce a complete local bundle.

For a packaging-only rehearsal without a signing key, run
`./mvnw -Prelease package -DskipTests`. This produces jars but no signatures and
does not constitute a verified or publishable release. Do not skip tests or
signing for an actual release.

## Upload and approve

Run **Upload release branch to Central** from `main`, supplying the prepared
release version. It checks out `release/<version>`, checks the POM version and
SCM reference, and runs the Chrome and Firefox tests plus release packaging.
The upload job waits for the configured `maven-central` environment approval,
then checks out the exact tested commit, rebuilds, signs and uploads it.
It does not build the next snapshot on main. No release tag is required.

To upload locally instead, use a clean checkout of the reviewed release branch:

```sh
./mvnw --settings .github/maven-central-settings.xml \
  -Prelease clean deploy -Dgpg.keyname=FINGERPRINT
```

This uploads the signed bundle and waits for Central validation. It does **not**
publish automatically: review the deployment in the Portal and explicitly choose
Publish. An upload can expose the source jars when publication is approved, even
if the GitHub repository is private. Review their contents before approval.

The publisher's bundle is `target/central-publishing/central-bundle.zip` at the
reactor root. Check that it contains the six artifacts listed above, matching
release versions, signatures and checksums, and no examples. Test a consumer
using only those artifacts and Maven Central, without private package repositories.

After publication, verify a fresh consumer can resolve the released coordinates
without GitHub credentials. Published versions cannot be overwritten; corrections
need a new version. Main was already advanced to the selected next snapshot by
release preparation. If an upload fails, fix or retry that release branch;
do not advance main a second time.

## Requirements checked

Names, descriptions, project URL and Apache-2.0 licence metadata were already
present. The release setup adds developer and SCM metadata, attachment executions,
signing and the Central publishing extension. Namespace ownership, account
permissions, signing-key availability and final Portal acceptance are external
checks; adding the profile alone does not establish them.

See [Central's requirements](https://central.sonatype.org/publish/requirements/),
[namespace verification](https://central.sonatype.org/register/namespace/),
[OpenPGP signing](https://central.sonatype.org/publish/requirements/gpg/) and
[the Maven publishing guide](https://central.sonatype.org/publish/publish-portal-maven/).
