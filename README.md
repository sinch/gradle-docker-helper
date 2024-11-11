
### Gradle Docker helper

A helper plugin that makes it easier to set up Docker tasks e.g. for local MySQL DB access.

#### Configuration

```kotlin
open class DockerHelperPluginExtension {
    var taskGroup: String = "docker"
    var taskPrefix: String = "docker"

    val registryAuthority: String? = null // if null, the other registry settings are ignored
    val registryUsername: String? = null
    val registryPassword: String? = null

    var imageName: String? = null 
    var imagePath: String? = null 
    var tag: String = "latest"
    
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
