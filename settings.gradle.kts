pluginManagement {
  repositories {
    maven { url = uri("http://127.0.0.1:8761"); isAllowInsecureProtocol = true }
    maven { url = uri("http://127.0.0.1:8762"); isAllowInsecureProtocol = true }
    maven { url = uri("http://127.0.0.1:8763"); isAllowInsecureProtocol = true }
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    maven { url = uri("http://127.0.0.1:8761"); isAllowInsecureProtocol = true }
    maven { url = uri("http://127.0.0.1:8762"); isAllowInsecureProtocol = true }
  }
}
rootProject.name = "AERIS AI"
include(":app")
