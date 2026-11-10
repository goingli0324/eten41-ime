// Maven Central 用 repo1.maven.org：開發環境解析不到 repo.maven.apache.org（mavenCentral() 的預設網址），
// 兩者是同一個官方倉庫
pluginManagement {
    repositories {
        google()
        maven("https://repo1.maven.org/maven2/")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://repo1.maven.org/maven2/")
    }
}
rootProject.name = "Eten41"
include(":app")
