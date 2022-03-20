package io.github.huiibuh.extensions

import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name


fun Path.hasParent() = this.parent != null && this.parent.name.isNotEmpty()
fun Path.replacePart(replaceString: String): Path {
    val absReplace = Path.of(replaceString).toAbsolutePath().normalize().absolutePathString()
    val absPath = this.toAbsolutePath().normalize().absolutePathString()
    return Path.of(absPath.replace(absReplace, ""))
}

fun Path.countParents(): Int {
    var current = this.normalize()
    var parents = 0
    while (current.hasParent()) {
        current = current.parent
        parents += 1
    }
    return parents
}

fun Path.parentName() = this.parent.name
fun Path.grandParentName() = this.parent.parent.name
fun Path.grandGrandParentName() = this.parent.parent.parent.name
