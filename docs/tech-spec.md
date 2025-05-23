# Technical Specification

## Project Overview

`android-ui-components` is a multi-module Android project providing high-quality custom UI
components. Initially, the library will include only `ShimmerView`, with plans to expand the set of
components in future releases.

**Key Goals:**

* Provide a reusable, well-documented `ShimmerView` component.
* Ensure performance and reliability through benchmarks and automated tests.
* Demonstrate integration and usage via a sample app.
* Automate build, test, and release workflows with CI/CD.

## Module Structure

```plaintext
android-ui-components/
├── library/           # Android library module for UI components
├── sample/            # Demo app showing usage of the library
├── benchmark/         # Benchmark tests for performance measurement
├── docs/              # Project documentation
│   ├── tech-spec.md   # This technical specification
│   └── roadmap.md     # Planned features and milestones
├── .github/           # GitHub workflows and templates
└── README.md          # Project overview and quickstart
```

## library Module

* **Purpose:** Contains the core `ShimmerView` implementation.
* **Language & Plugins:** Kotlin, `com.android.library`, `kotlin-android`.
* **API Versioning:** Semantic versioning (e.g., v1.0.0). Version declared in `gradle.properties`.

### ShimmerView

* **Features:**

    * Configurable shimmer color and speed.
    * Support for XML and programmatic instantiation.
    * Lightweight rendering using `Canvas` and `Paint`.
* **Public API:**

    * `setShimmerColor(@ColorInt color)`
    * `setShimmerDuration(ms: Long)`
    * `startShimmer()` / `stopShimmer()`
* **Resources:**

    * Default style in `res/values/styles.xml`.
    * Example layout in `res/layout/view_shimmer.xml`.

### ProgressGridLayout

A custom `FrameLayout` that displays a grid-based progress reveal effect with an optional animated light wave overlay. Designed for use cases where you want to visually indicate loading or progress in a grid (e.g., image galleries, dashboards).

**Features:**
- Configurable number of rows and columns
- Smooth reveal animation for grid cells
- Optional animated light wave overlay
- Customizable overlay color and animation duration
- Supports XML and programmatic usage

**Public API:**
- `start()`: Starts the progress reveal animation and triggers the light wave effect when finished
- `stop()`: Stops all animations and resets the overlay
- `animationDuration`: Duration of the reveal animation (ms)
- `overlayColor`: Color of the overlay (ARGB)
- `gridRows`, `gridCols`: Number of rows and columns in the grid

**Resources:**
- Example layout in `res/layout/progress_grid_layout.xml`

## sample Module

* **Purpose:** Showcase `ShimmerView` in a standalone demo app.
* **Dependencies:** `implementation project(":library")`
* **Features:**

    * Screen demonstrating various configurations of `ShimmerView`.
    * Screenshot assets in `docs/assets/`.
* **Testing:** Espresso tests in `src/androidTest` validating UI behavior.

## benchmark Module

* **Purpose:** Measure performance characteristics of `ShimmerView`.
* **Dependencies:** `androidx.benchmark:benchmark-macro-junit4`.
* **Tests:**

    * Inflation time benchmark.
    * Drawing/render pass benchmark.
* **Execution:** Run via `./gradlew :benchmark:connectedAndroidTest` or scheduled in CI.

## Testing Strategy

1. **Unit Tests (library/src/test):**

    * Robolectric tests for attribute parsing and state changes.
    * JUnit tests for helper utilities.
2. **UI Tests (sample/src/androidTest):**

    * Espresso tests verifying shimmer animation starts and stops.
3. **Benchmark Tests (benchmark/src/androidTest):**

    * Macrobenchmark rules for cold and warm starts.

## CI/CD Process

* **CI Workflow (`.github/workflows/ci.yml`):**

    1. Checkout code
    2. Setup JDK and Android SDK
    3. Run `./gradlew :library:test :sample:connectedAndroidTest`
    4. Publish code coverage reports
* **Release Workflow (`.github/workflows/release.yml`):**

    1. Trigger on Git tag `v*.*.*`
    2. Build `library` AAR (`./gradlew :library:bundleRelease`)
    3. Publish AAR to GitHub Packages
    4. Generate and deploy Javadoc via GitHub Pages (optional)

## Documentation

* **README.md:** Project overview, installation, usage examples, badges.
* **docs/roadmap.md:** Planned features and timelines.

## Future Roadmap

1. Add new UI components (e.g., `LoadingButton`, `AnimatedProgressBar`).
2. Enhance `ShimmerView` with RTL support and hardware acceleration.
3. Publish to Maven Central.
4. Expand CI with lint checks (Detekt, Ktlint) and codecov integration.

---

*Last updated: May 17, 2025*
