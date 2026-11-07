data class User(val name: String, val age: Int) {
    var lastSeen: String = ""
}

class Plain(val name: String)

fun main() {
    val a = User("Nishant", 30)
    val b = User("Nishant", 30)
    val c = a
    println("a == b  : ${a == b}")
    println("a === b : ${a === b}")
    println("a === c : ${a === c}")
    println("a.hashCode() == b.hashCode() : ${a.hashCode() == b.hashCode()}")
    println("set size : ${setOf(a, b).size}")
    println("a : $a")

    a.lastSeen = "today"
    println("after a.lastSeen = today, a == b : ${a == b}")

    val p = Plain("Nishant")
    val q = Plain("Nishant")
    println("Plain p == q : ${p == q}")
}
