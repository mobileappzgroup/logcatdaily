package dev.logcatdaily.samples.modules.model

data class Product(val id: String, val name: String, val price: Money)

object DemoProducts {
    val all = listOf(
        Product("notebook", "Notebook", Money(450)),
        Product("pen", "Pen", Money(300)),
    )
}
