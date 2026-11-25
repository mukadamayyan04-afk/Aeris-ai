pluginManagement {
  repositories {
    maven {
      url = uri("http://127.0.0.1:8765/google")
      isAllowInsecureProtocol = true
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    maven { url = uri("http://127.0.0.1:8765/central"); isAllowInsecureProtocol = true }
    maven { url = uri("http://127.0.0.1:8765/plugins"); isAllowInsecureProtocol = true }
  }
}

// JDK is provisioned by the build environment; no toolchain download resolver is required.

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    maven { url = uri("http://127.0.0.1:8765/google"); isAllowInsecureProtocol = true }
    maven { url = uri("http://127.0.0.1:8765/central"); isAllowInsecureProtocol = true }
  }
}

rootProject.name = "AERIS AI"

include(":app")
