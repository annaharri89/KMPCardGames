package presentation.solitaire.geometry

/**
 * Stage-space rectangle (top-left origin, positive Y downward), with no UI-engine types.
 */
data class AxisAlignedRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
) {
    fun containsPoint(stageX: Double, stageY: Double): Boolean =
        stageX >= x && stageX <= x + width && stageY >= y && stageY <= y + height
}
