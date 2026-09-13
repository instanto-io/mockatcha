# SpotBugs reports

SpotBugs runs automatically in `process-classes`, after Java compilation and
before tests, and therefore also during `package`, `verify`, `install`, and
`deploy`. A `compile`-only invocation stops before this phase.

Run the default reactor locally without running tests:

```sh
./mvnw -B -ntp --fail-at-end -DskipTests process-classes
```

Use `mvn` if this checkout has no Maven wrapper. Use the same Maven settings,
credentials and profiles as your normal build. Optional/profile-only reactors
must be selected explicitly with their usual `-P` or `-f` arguments.

Each Java module writes its own reports under `target/spotbugs/`:

- `${project.artifactId}-spotbugs.xml`: native SpotBugs findings.
- `${project.artifactId}-spotbugs.sarif`: findings for SARIF viewers.
- `${project.artifactId}-spotbugs.html`: readable HTML report.

A small Ant execution names the plugin-generated HTML report after the artifact.

Findings do not fail the build (the `check` goal is not bound). Analyzer errors
still fail it. POM-only modules and modules without compiled Java classes have
no bytecode to analyze. Reports are build outputs and are removed by `clean`.
To skip analysis explicitly, use `-Dspotbugs.skip=true`.
