//
plugins {
    id("com.sinch.gradle.dockerhelper")
}

dockerhelper {
    taskGroup = "foobar"
    taskPrefix = "foo"

    imageName = "zupa"
    imagePath = "dev/lupa"
}

println("OK")
