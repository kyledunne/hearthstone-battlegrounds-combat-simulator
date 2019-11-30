import org.lwjgl.opengl.Display

class CardSlot(var x: Int, var y: Int, var card: BoardMinion?) {

}

class DisplayBoard(var p1Board: BoardState, var p2Board: BoardState) {
    var p1Slots: Array<CardSlot> = Array(7) { i ->
        CardSlot(0, 0, p1Board.get(i+1))
    }
    var p2Slots: Array<CardSlot> = Array(7) { i ->
        CardSlot(0, 0, p2Board.get(i+1))
    }
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
    fun display() {
        for (slot in p1Slots) {
            if (slot.card != null) {
                drawCard(slot.card!!, slot.x, slot.y)
            }
        }
        for (slot in p2Slots) {
            if (slot.card != null) {
                drawCard(slot.card!!, slot.x, slot.y)
            }
        }
    }

    fun updateState(p1Board: BoardState, p2Board: BoardState) {
        var i = 0
        var numMinions = p1Board.numMinions()
        while (i < numMinions) {
            p1Slots[i].card = p1Board.get(i+1)
            i += 1
        }
        while (i < 7) {
            p1Slots[i].card = null
            i += 1
        }
        i = 0
        numMinions = p2Board.numMinions()
        while (i < numMinions) {
            p2Slots[i].card = p2Board.get(i+1)
            i += 1
        }
        while (i < 7) {
            p2Slots[i].card = null
            i += 1
        }
    }


    companion object {
        const val CARD_WIDTH: Int = 150
        const val CARD_HEIGHT: Int = 150
        const val CARD_HORIZONTAL_SPACING = 50
        const val CARD_VERTICAL_SPACING = 200

        const val CARD_IMG_FOLDER = "res/img/cards/"
        const val CARD_IMG_ENDING = ".png"

        const val EFFECT_IMG_FOLDER = "res/img/effects/"
        const val EFFECT_IMG_ENDING = ".png"

        const val ICON_SIZE = 64
        const val TRIPLE_SIZE = 100

        val POISONOUS_IMG = Image(EFFECT_IMG_FOLDER + "poisonous" + EFFECT_IMG_ENDING)
        val DIVINE_SHIELD_IMG = Image(EFFECT_IMG_FOLDER + "divineShield" + EFFECT_IMG_ENDING)
        val DEATHRATTLE_IMG = Image(EFFECT_IMG_FOLDER + "deathrattle" + EFFECT_IMG_ENDING)
        val TRIPLE_IMG = Image(EFFECT_IMG_FOLDER + "triple" + EFFECT_IMG_ENDING)
    }
}
