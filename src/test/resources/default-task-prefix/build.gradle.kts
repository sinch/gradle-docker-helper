//
plugins {
    id("com.sinch.gradle.dockerhelper")
}

dockerhelper {
    imageName = "zupa"
    imagePath = "dev/lupa"
}

println("OK")
