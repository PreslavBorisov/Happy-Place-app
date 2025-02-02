package eu.example.happyplaces.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import eu.example.happyplaces.R

abstract class SwipeToEditCallback(context: Context) :
ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.RIGHT){

    private val editIcon = ContextCompat.getDrawable(context, R.drawable.ic_edit_icon)
    private val intrinsicsWidth = editIcon!!.intrinsicWidth
    private val intrinsicsHeight = editIcon!!.intrinsicHeight
    private val background = ColorDrawable()
    private val backgroundColor = Color.parseColor("#24AE05")
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }


    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        if(viewHolder.adapterPosition==10) return 0
        return super.getMovementFlags(recyclerView, viewHolder)
    }


    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX ==0f && !isCurrentlyActive

        if(isCanceled){
            clearCanvas(c,itemView.left+dX,itemView.top.toFloat(),itemView.left.toFloat(),itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        background.color = backgroundColor
        background.setBounds(itemView.left + dX.toInt(),itemView.top,itemView.left,itemView.bottom)
        background.draw(c)

        val editIconTop = itemView.top + (itemHeight - intrinsicsHeight) / 2
        val editIconMargin = (itemHeight - intrinsicsHeight)
        val editIconLeft = itemView.left + editIconMargin - intrinsicsWidth
        val editIconRight = itemView.left + editIconMargin
        val editIconBottom = editIconTop + intrinsicsHeight

        editIcon!!.setBounds(editIconLeft, editIconTop, editIconRight, editIconBottom)
        editIcon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c:Canvas?,left:Float,top:Float,right:Float,bottom:Float){
        c?.drawRect(left,top,right,bottom,clearPaint)
    }

}


