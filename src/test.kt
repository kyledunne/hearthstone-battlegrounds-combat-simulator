import kotlin.test.assertEquals
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11
import kotlin.system.exitProcess

class MainClass {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val test1 = test1()
            assertEquals(0, test1, test1.toString())
            println("All tests passed")
        }

        fun test1(): Int {
            Display.setFullscreen(true)
            Display.create()

            GL11.glMatrixMode(GL11.GL_PROJECTION)
            GL11.glLoadIdentity()
            GL11.glOrtho(0.0, Display.getWidth().toDouble(), Display.getHeight().toDouble(), 0.0, 0.0, 1.0)
            GL11.glMatrixMode(GL11.GL_MODELVIEW)
            GL11.glLoadIdentity()
            GL11.glTranslatef(.375f, .375f, 0f)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glClearColor(0f, 0f, 0f, 1f)//BLACK
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glShadeModel(GL11.GL_SMOOTH)
            GL11.glDisable(GL11.GL_LIGHTING)
            GL11.glClearDepth(1.0)

            var p1board = BoardState(BoardMinion(MinionType.foeReaper, 3, 3),
                BoardMinion(MinionType.foeReaper, 3, 3),
                BoardMinion(MinionType.foeReaper, 3, 3),
                BoardMinion(MinionType.foeReaper, 3, 3),
                BoardMinion(MinionType.foeReaper, 3, 3),
                BoardMinion(MinionType.foeReaper, 3, 3),
                BoardMinion(MinionType.foeReaper, 3, 3))
            var p2board = BoardState(BoardMinion(MinionType.foeReaper, 2, 100),
                BoardMinion(MinionType.foeReaper, 3, 3),
                BoardMinion(MinionType.foeReaper, 3, 3),
                BoardMinion(MinionType.foeReaper, 3, 3),
                BoardMinion(MinionType.foeReaper, 3, 3),
                BoardMinion(MinionType.foeReaper, 3, 3),
                BoardMinion(MinionType.foeReaper, 3, 3))
            return(simulate(p1board, p2board, 1, 1, true))
        }

        fun updateState(p1state: BoardState, p2state: BoardState, isP1NextToAttack: Boolean, attackingSlot: Int, defendingSlot: Int) {
            println("attacking slot = $attackingSlot")
            println("defending slot = $defendingSlot")

        }
    }
}