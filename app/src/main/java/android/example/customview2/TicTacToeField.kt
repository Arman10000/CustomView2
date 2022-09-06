package android.example.customview2

enum class Cell {
    PLAYER_1,
    PLAYER_2,
    EMPTY
}

typealias OnFieldChangedListener = (field: TicTacToeField) -> Unit

class TicTacToeField(
    val rows: Int,
    val columns: Int
) {

    private val cells = Array(rows) { Array(columns) { Cell.EMPTY } }

    val listeners = mutableSetOf<OnFieldChangedListener>()

    fun getCell(row: Int, column: Int): Cell {
        return cells[row][column]
    }

    fun setCell(row: Int, column: Int, cell: Cell) {
        if (cells[row][column] != cell) {
            cells[row][column] = cell
            listeners.forEach { it.invoke(this) }
        }
    }
}