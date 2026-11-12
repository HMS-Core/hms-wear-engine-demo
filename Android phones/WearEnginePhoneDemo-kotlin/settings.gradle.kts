pluginManagement {
    repositories {
        mavenLocal()
        // 配置HMS Core SDK的Maven仓地址
        // HMS Coreのリポジトリ
        // Configure the Maven repository address for the HMS Core SDK
        maven { setUrl("https://developer.huawei.com/repo/") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        // 配置HMS Core SDK的Maven仓地址
        // HMS Coreのリポジトリ
        // Configure the Maven repository address for the HMS Core SDK
        maven { setUrl("https://developer.huawei.com/repo/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "WearEngineDemo"
include(":app")
