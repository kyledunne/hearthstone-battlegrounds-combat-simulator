import org.lwjgl.opengl.Display

class CardSlot(var x: Int, var y: Int, card: BoardMinion) {

}

class DisplayBoard(var p1Slots: Array<CardSlot>, var p2Slots: Array<CardSlot>) {
    init {
        val displayWidth = Display.getWidth()
        val displayHeight = Display.getHeight()
        val totalCardWidth = CARD_WIDTH * 7 + CARD_HORIZONTAL_SPACING * 6
        val x: Int = (displayWidth - totalCardWidth) / 2
        val totalCardHeight = CARD_HEIGHT * 2 + CARD_VERTICAL_SPACING
        val y: Int = (displayHeight - totalCardHeight) / 2
        val y2: Int = y + CARD_HEIGHT + CARD_VERTICAL_SPACING
        var currentX = x
        for (slot in p2Slots) {
            slot.x = currentX
            slot.y = y

            currentX += CARD_WIDTH + CARD_HORIZONTAL_SPACING
        }
        currentX = x
        for (slot in p1Slots) {
            slot.x = currentX
            slot.y = y2

            currentX += CARD_WIDTH + CARD_HORIZONTAL_SPACING
        }
    }
    companion object {
        const val CARD_WIDTH: Int = 150
        const val CARD_HEIGHT: Int = 150
        const val CARD_HORIZONTAL_SPACING = 50
        const val CARD_VERTICAL_SPACING = 200
    }
}