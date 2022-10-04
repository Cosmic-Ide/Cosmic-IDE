package javax.swing

interface Icon {
    fun getIconHeight(): Int

    fun getIconWidth(): Int

    fun paintIcon(component: Component, graphics: Graphics)
}
