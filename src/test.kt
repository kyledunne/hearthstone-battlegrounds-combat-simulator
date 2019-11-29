import kotlin.test.assertEquals

class MainClass {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val test1 = test1()
            assertEquals(0, test1(), test1.toString())
            println("All tests passed")
        }

        fun test1(): Int {
            var p1board = BoardState(BoardMinion(3, 3, 1), BoardMinion(3, 3, 1), BoardMinion(3, 3, 1), BoardMinion(3, 3, 1), BoardMinion(3, 3, 1), BoardMinion(3, 3, 1), BoardMinion(3, 3, 1))
            var p2board = BoardState(BoardMinion(3, 3, 1), BoardMinion(3, 3, 1), BoardMinion(3, 3, 1), BoardMinion(3, 3, 1), BoardMinion(3, 3, 1), BoardMinion(3, 3, 1), BoardMinion(3, 3, 1))
            return(simulate(p1board, p2board, 1, 1, true))
        }
    }
}