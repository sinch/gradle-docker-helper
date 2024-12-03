
### Gradle Docker helper

A helper plugin that makes it easier to set up Docker tasks e.g. for local MySQL DB access.

#### Configuration

```kotlin
open class DockerHelperPluginExtension {
    var taskGroup: String = "docker"
    var taskPrefix: String = "docker"

    var registryAuthority: String? = null // if null, the other registry settings are ignored
    var registryUsername: String? = null
    var registryPassword: String? = null

    var imageName: String? = null
    var imagePath: String? = null
    var tag: String = "latest"

    var waitSeconds: Int = DEFAULT_WAIT_SECONDS // 60

    var envVars: Map<String, String> = mapOf()

    var portBindings: List<String> = listOf()
}

// usage via e.g.:

dockerhelper {
    taskGroup = "infra"
    taskPrefix = "mysql"
    
    // etc. etc.
}
```
