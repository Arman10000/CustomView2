package android.example.customview2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

typealias OnCellActionListener = (row: Int, column: Int, field: TicTacToeField) -> Unit

class TicTacToeView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(
    context,
    attributeSet,
    defStyleAttr,
    defStyleRes
) {

    var ticTacToeField: TicTacToeField? = null
        set(value) {
            field = value
            field?.listeners?.add(listener)
            updateViewSizes()
            requestLayout()//если View после пересоздания может изменить свой размер нада запустить этот метод
            invalidate()//если хотим пересоздать View запускаем это метод
        }

    var actionListener: OnCellActionListener? = null

    private var player1Color by Delegates.notNull<Int>()
    private var player2Color by Delegates.notNull<Int>()
    private var gridColor by Delegates.notNull<Int>()

    private val fieldRect = RectF()
    private var cellSize = 0f
    private var cellPadding = 0f
    private val cellRect = RectF()

    private var currentRow = -1
    private var currentColumn = -1

    private lateinit var player1Paint: Paint
    private lateinit var player2Paint: Paint
    private lateinit var currentCellPaint: Paint
    private lateinit var gridPaint: Paint

    companion object {
        const val PLAYER1_DEFAULT_COLOR = Color.GREEN
        const val PLAYER2_DEFAULT_COLOR = Color.RED
        const val GRID_DEFAULT_COLOR = Color.GRAY

        const val DESIRED_CELL_SIZE = 50f
    }

    init {
        if (attributeSet != null) {
            initAttributes(
                attributeSet,
                defStyleAttr,
                defStyleRes
            )
        } else {
            initDefaultColors()
        }
        initPaints()
        //isInEditMode находимся в редактированом режиме
        if (isInEditMode) {
            ticTacToeField = TicTacToeField(8, 6)
//            ticTacToeField?.setCell(2, 2, Cell.PLAYER_1)
//            ticTacToeField?.setCell(2, 3, Cell.PLAYER_2)
        }
        isFocusable = true
        isClickable = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           defaultFocusHighlightEnabled = false //убираем пульсацию при нажатие
        }
    }

    //View привизалас к экрану
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ticTacToeField?.listeners?.add(listener)
    }

    //View отвезалас от эрана
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ticTacToeField?.listeners?.remove(listener)
    }

    //с перва запускаеца мы предлагаем компоновшику свои размеры и его размеры которые он предложил а он уже решает какие размеры приминить
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val minHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        val desiredCellSizeInPixels = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,//хотим получить в Pixel
            DESIRED_CELL_SIZE,
            resources.displayMetrics
        ).toInt()
        val rows = ticTacToeField?.rows ?: 0
        val columns = ticTacToeField?.columns ?: 0

        val desiredWith = max(minWidth, columns * desiredCellSizeInPixels + paddingLeft + paddingRight)
        val desiredHeight = max(minHeight, rows * desiredCellSizeInPixels + paddingTop + paddingBottom)

        setMeasuredDimension(
            resolveSize(desiredWith, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    //потом компоновшик запускает и отправляет нам выбраные им размеры которые он одобрел
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewSizes()
    }

    //рисуем View должен быть максимально оптимизирован
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
//        canvas.drawLine(50f, 50f, 100f, 100f, player1Paint)
        if (ticTacToeField == null) return
        if (cellSize == 0f) return
        if (fieldRect.width() <= 0) return
        if (fieldRect.height() <= 0) return

        drawGrid(canvas)
        drawCurrentCell(canvas)
        drawCells(canvas)
    }

    //слушаем нажатие на клаве
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when(keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> moveCurrentCell(-1, 0)//нажали стрелку в верх
            KeyEvent.KEYCODE_DPAD_DOWN -> moveCurrentCell(1, 0)//нажали стрелку в низ
            KeyEvent.KEYCODE_DPAD_LEFT -> moveCurrentCell(0 , -1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveCurrentCell(0, 1)
            else -> super.onKeyDown(keyCode, event)
        }
    }

    //переподключаемся между ячейками (при нажатие на клаву)
    private fun moveCurrentCell(rowDiff: Int, columnDiff: Int): Boolean {
        val field = ticTacToeField ?: return false
        if (currentRow == -1 || currentColumn == -1) {
            currentRow = 0
            currentColumn = 0
            invalidate()
            return true
        } else {
            if (currentColumn + columnDiff < 0) return false
            if (currentColumn + columnDiff >= field.columns) return false
            if (currentRow + rowDiff < 0) return false
            if (currentRow + rowDiff >= field.rows) return false

            currentColumn += columnDiff
            currentRow += rowDiff
            invalidate()
            return true
        }
    }

    //Обрабативаем нажатие пальцем
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                //событие нажали возвращаем true значит мы его обработали типер будут поступать следующие события
                updateCurrentCell(event)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                //событие водим палцем по экрану
                updateCurrentCell(event)
            }
            MotionEvent.ACTION_UP -> {
                //событие отпутили
                return performClick()
            }
        }
        return false
    }

    //обрабативаем клик ставим нолик или крестик
    override fun performClick(): Boolean {
        super.performClick()
        val field = ticTacToeField ?: return false
        val row = currentRow
        val column = currentColumn
        if(row >= 0 && column >= 0 && row < field.rows && column < field.columns) {
            actionListener?.invoke(row, column, field)
            return true
        }
        return false
    }

    //заполняем ячейку цветом (серым)
    private fun updateCurrentCell(event: MotionEvent) {
        val field = ticTacToeField ?: return
        val row = getRow(event)
        val column = getColumn(event)
        if (row >= 0 && column >= 0 && row < field.rows && column < field.columns) {
            if (currentRow != row || currentColumn != column) {
                currentRow = row
                currentColumn = column
                invalidate()
            }
        }
    }

    //получаем строку чтобы понять где мы находимся (когда водим палцем по экрану)
    private fun getRow(event: MotionEvent): Int {
        return ((event.y - fieldRect.top) / cellSize).toInt()
    }

    //получаем колону чтобы понять где мы находимся (когда водим палцем по экрану)
    private fun getColumn(event: MotionEvent): Int {
        return ((event.x - fieldRect.left) / cellSize).toInt()
    }

    //за полняем ячейку (серым цветом)
    private fun drawCurrentCell(canvas: Canvas) {
        if (currentRow == -1 || currentColumn == -1) return
        val cell = getCellRect(currentRow, currentColumn)
        canvas.drawRect(
            cell.left - cellPadding,
            cell.top - cellPadding,
            cell.right + cellPadding,
            cell.bottom + cellPadding,
            currentCellPaint
        )
    }

    //рисуем сетку
    private fun drawGrid(canvas: Canvas) {
        val field = ticTacToeField ?: return
        val startX = fieldRect.left
        val stopX = fieldRect.right
        for (i in 0..field.rows) {
            val y = fieldRect.top + cellSize * i
            canvas.drawLine(startX, y, stopX, y, gridPaint)
        }

        val startY = fieldRect.top
        val stopY = fieldRect.bottom
        for (i in 0..field.columns) {
            val x = fieldRect.left + cellSize * i
            canvas.drawLine(x, startY, x, stopY, gridPaint)
        }
    }

    //рисуем в ячейках крестики или нолики или не чего если игрок не нажимал на ячейку
    private fun drawCells(canvas: Canvas) {
        val field = ticTacToeField ?: return
        for (row in 0 until field.rows) {
            for (column in 0 until field.columns) {
                val cell = field.getCell(row, column)
                if (cell == Cell.PLAYER_1) {
                    drawPlayer1(canvas, row, column)
                } else if (cell == Cell.PLAYER_2) {
                    drawPlayer2(canvas, row, column)
                }
            }
        }
    }

    //рисуем крестик (первый игрок)
    private fun drawPlayer1(canvas: Canvas, row: Int, column: Int) {
        val cellRect = getCellRect(row, column)
        canvas.drawLine(cellRect.left, cellRect.top, cellRect.right, cellRect.bottom, player1Paint)
        canvas.drawLine(cellRect.right, cellRect.top, cellRect.left, cellRect.bottom, player1Paint)
    }

    //рисуем нолик (второй игрок)
    private fun drawPlayer2(canvas: Canvas, row: Int, column: Int) {
        val cellRect = getCellRect(row, column)
        canvas.drawCircle(
            cellRect.centerX(),
            cellRect.centerY(),
            cellRect.width() / 2,
            player2Paint
        )
    }

    //получаем обект Rect который хранит растояние до указаной ячейки
    private fun getCellRect(row: Int, column: Int): RectF {
        cellRect.left = fieldRect.left + column * cellSize + cellPadding
        cellRect.top = fieldRect.top + row * cellSize + cellPadding
        cellRect.right = cellRect.left + cellSize - cellPadding * 2
        cellRect.bottom = cellRect.top + cellSize - cellPadding * 2
        return cellRect
    }

    //инцилизируем кисточки
    private fun initPaints() {
        //кисточка первого игрока (крестик)
        player1Paint = Paint(Paint.ANTI_ALIAS_FLAG)//ANTI_ALIAS_FLAG плавная отрисовка
        player1Paint.color = player1Color//цвет кисточки
        player1Paint.style = Paint.Style.STROKE//рисовать только линиии
        player1Paint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics)//получаем из db пиксили устанавливаем ширину строк

        //кисточка второго игрока (нолик)
        player2Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        player2Paint.color = player2Color
        player2Paint.style = Paint.Style.STROKE
        player2Paint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics)

        //кисточка сетки
        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        gridPaint.color = gridColor
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)

        //кисточка фон ячейки (когда хотим её залить)
        currentCellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        currentCellPaint.color = Color.rgb(230,230,230)
        currentCellPaint.style = Paint.Style.FILL//полностю заполнит элемент
    }

    //обнавляем размеры View
    private fun updateViewSizes() {
        val field = this.ticTacToeField ?: return

        val safeWidth = width - paddingLeft - paddingRight//получаем ширину без padding
        val safeHeight = height - paddingTop - paddingBottom

        val sellWidth = safeWidth / field.columns.toFloat()//получаем точный размер ячейки по этому используем Float
        val sellHeight = safeHeight / field.rows.toFloat()

        cellSize = min(sellWidth, sellHeight)//полученый размер будет использоваца для ширины и высоты ячейки
        cellPadding = cellSize * 0.2f//получаем Padding в 2% от 100%

        val fieldWidth = cellSize * field.columns//получаем ширину элемента который планируем на рисовать (сетка)
        val fieldHeight = cellSize * field.rows

        fieldRect.left = paddingLeft + (safeWidth - fieldWidth) / 2//получаем от куда будет рисовать элемент (сетку) с учётом не заполненой безопасной зоны с левой стороны если она есть
        fieldRect.top = paddingTop + (safeHeight - fieldHeight) / 2
        fieldRect.right = fieldRect.left + fieldWidth
        fieldRect.bottom = fieldRect.top + fieldHeight
    }

    //получаем данные переданые в атребуты
    private fun initAttributes(
        attributeSet: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        val typedArray = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.TicTacToeView,
            defStyleAttr,
            defStyleRes
        )
        player1Color = typedArray.getColor(R.styleable.TicTacToeView_player1Color, PLAYER1_DEFAULT_COLOR)
        player2Color = typedArray.getColor(R.styleable.TicTacToeView_player2Color, PLAYER2_DEFAULT_COLOR)
        gridColor = typedArray.getColor(R.styleable.TicTacToeView_gridColor, GRID_DEFAULT_COLOR)
        typedArray.recycle()
    }

    //если атребутов не будет то устанавливаем цвета по умольчанию
    private fun initDefaultColors() {
        player1Color = PLAYER1_DEFAULT_COLOR
        player2Color = PLAYER2_DEFAULT_COLOR
        gridColor = GRID_DEFAULT_COLOR
    }

    //пользователь кликает на View устанавливаеца в ячейку обект Cеll (первый/второй игрок) после запукаеца этот метод
    private val listener: OnFieldChangedListener = {
        invalidate()//заного рисуем View
    }
}