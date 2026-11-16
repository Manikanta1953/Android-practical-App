package com.manikanta.trekaltitudelogger.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.manikanta.trekaltitudelogger.data.db.WaypointEntity
import java.util.Locale
import kotlin.math.max

class ElevationProfileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val waypoints = mutableListOf<WaypointEntity>()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#1B5E20") // Forest Green
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#E0E0E0")
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        color = Color.parseColor("#666666")
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2E7D32")
    }

    private val pointInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        color = Color.GRAY
        textAlign = Paint.Align.CENTER
    }

    private val path = Path()
    private val fillPath = Path()

    fun setWaypoints(newWaypoints: List<WaypointEntity>) {
        waypoints.clear()
        waypoints.addAll(newWaypoints)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        if (waypoints.size < 2) {
            val msg = if (waypoints.isEmpty()) "Log waypoints to view elevation profile" else "Log at least 2 waypoints for profile"
            canvas.drawText(msg, width / 2f, height / 2f, emptyTextPaint)
            return
        }

        val paddingLeft = 100f
        val paddingRight = 40f
        val paddingTop = 50f
        val paddingBottom = 60f

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom

        var minAlt = Double.MAX_VALUE
        var maxAlt = Double.MIN_VALUE

        for (wp in waypoints) {
            if (wp.altitude < minAlt) minAlt = wp.altitude
            if (wp.altitude > maxAlt) maxAlt = wp.altitude
        }

        val altRange = max(maxAlt - minAlt, 10.0)
        val drawMinAlt = minAlt - (altRange * 0.1)
        val drawMaxAlt = maxAlt + (altRange * 0.1)
        val drawAltRange = drawMaxAlt - drawMinAlt

        val gridLines = 3
        for (i in 0..gridLines) {
            val y = paddingTop + (contentHeight / gridLines) * i
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)

            val gridAlt = drawMaxAlt - (drawAltRange / gridLines) * i
            canvas.drawText(String.format(Locale.US, "%.0fm", gridAlt), 10f, y + 10f, textPaint)
        }

        path.reset()
        fillPath.reset()

        fillPath.moveTo(paddingLeft, paddingTop + contentHeight)

        val points = mutableListOf<PointF>()
        for (i in waypoints.indices) {
            val x = paddingLeft + (contentWidth / (waypoints.size - 1)) * i
            val normAlt = (waypoints[i].altitude - drawMinAlt) / drawAltRange
            val y = paddingTop + contentHeight - (normAlt * contentHeight).toFloat()

            points.add(PointF(x, y))

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(points.last().x, paddingTop + contentHeight)
        fillPath.close()

        val shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + contentHeight,
            Color.parseColor("#6681C784"), Color.parseColor("#0581C784"),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = shader

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        for (i in points.indices) {
            val pt = points[i]
            canvas.drawCircle(pt.x, pt.y, 10f, pointPaint)
            canvas.drawCircle(pt.x, pt.y, 5f, pointInnerPaint)
        }
    }
}
