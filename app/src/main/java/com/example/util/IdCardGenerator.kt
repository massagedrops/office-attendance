package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.Employee
import java.io.File
import java.io.FileOutputStream

/**
 * Draws a staff ID card (photo, name, designation, Staff ID, QR code)
 * automatically from an [Employee] record.
 */
object IdCardGenerator {

    // Company branding. Change these to rebrand the card.
    const val COMPANY_NAME = "100 Drops"
    const val TAGLINE = "Care  •  Comfort  •  Quality"
    const val FOOTER = "YOUR COMFORT, OUR PRIORITY"

    // Card size (same ratio as the sample card: 909 x 1600)
    const val CARD_W = 909
    const val CARD_H = 1600

    private val NAVY = Color.parseColor("#0B2E4F")
    private val DARK = Color.parseColor("#0F3A5F")
    private val CYAN = Color.parseColor("#1FD6F5")
    private val BG_TOP = Color.parseColor("#E8F8FF")
    private val BG_BOTTOM = Color.parseColor("#C9EEFB")

    fun generate(context: Context, employee: Employee): Bitmap {
        val bmp = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val w = CARD_W.toFloat()
        val h = CARD_H.toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // ---- outer frame + background ----
        p.color = NAVY
        c.drawRoundRect(RectF(0f, 0f, w, h), 70f, 70f, p)
        p.color = CYAN
        c.drawRoundRect(RectF(18f, 18f, w - 18f, h - 18f), 60f, 60f, p)
        val inner = RectF(30f, 30f, w - 30f, h - 30f)
        p.shader = LinearGradient(0f, 0f, 0f, h, BG_TOP, BG_BOTTOM, Shader.TileMode.CLAMP)
        c.drawRoundRect(inner, 52f, 52f, p)
        p.shader = null

        // clip decorations to the inner card
        c.save()
        val clip = Path().apply { addRoundRect(inner, 52f, 52f, Path.Direction.CW) }
        c.clipPath(clip)
        drawDecorations(c, p, w, h)
        c.restore()

        // ---- logo ----
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_100drops)
        if (logo != null) {
            val boxH = 195f
            val boxW = boxH * logo.width / logo.height
            val dst = RectF(w / 2 - boxW / 2, 70f, w / 2 + boxW / 2, 70f + boxH)
            c.drawBitmap(logo, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }

        // ---- company name + tagline ----
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = NAVY
        }
        text.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        text.textSize = 66f
        c.drawText(COMPANY_NAME, w / 2, 325f, text)
        text.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        text.textSize = 28f
        text.letterSpacing = 0.12f
        c.drawText(TAGLINE, w / 2, 372f, text)
        text.letterSpacing = 0f

        // ---- photo ring ----
        val cx = w / 2
        val cy = 580f
        p.style = Paint.Style.FILL
        p.color = CYAN
        c.drawCircle(cx, cy, 172f, p)
        p.color = NAVY
        c.drawCircle(cx, cy, 160f, p)
        p.color = CYAN
        p.style = Paint.Style.STROKE
        p.strokeWidth = 8f
        c.drawCircle(cx, cy, 148f, p)
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        c.drawCircle(cx, cy, 138f, p)

        val photo = loadPhoto(context, employee.photoUri)
        if (photo != null) {
            drawCircularPhoto(c, photo, cx, cy, 138f)
        } else {
            drawPlaceholderAvatar(c, cx, cy, 138f)
        }

        // ---- name ----
        val name = employee.name.ifBlank { employee.nameAr }
        text.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        text.color = NAVY
        fitText(text, name, 780f, 70f, 36f)
        c.drawText(name, w / 2, 855f, text)

        // ---- designation pill ----
        val designation = employee.designation.ifBlank { "Staff" }
        val pill = RectF(w / 2 - 185f, 893f, w / 2 + 185f, 965f)
        p.color = CYAN
        c.drawRoundRect(RectF(pill.left - 4, pill.top - 4, pill.right + 4, pill.bottom + 4), 40f, 40f, p)
        p.shader = LinearGradient(pill.left, 0f, pill.right, 0f, DARK, NAVY, Shader.TileMode.CLAMP)
        c.drawRoundRect(pill, 36f, 36f, p)
        p.shader = null
        text.color = Color.WHITE
        text.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        fitText(text, designation, 330f, 44f, 24f)
        c.drawText(designation, w / 2, pill.centerY() + text.textSize * 0.35f, text)

        // ---- Staff ID row ----
        drawIdIcon(c, p, 262f, 1008f)
        p.color = NAVY
        p.strokeWidth = 2f
        c.drawLine(355f, 1002f, 355f, 1065f, p)
        text.textAlign = Paint.Align.LEFT
        text.color = DARK
        text.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        text.textSize = 26f
        c.drawText("Staff ID", 380f, 1010f, text)
        text.color = NAVY
        text.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        fitText(text, employee.id, 500f, 56f, 28f)
        c.drawText(employee.id, 380f, 1075f, text)
        text.textAlign = Paint.Align.CENTER

        // ---- QR frame ----
        val qrBox = RectF(w / 2 - 175f, 1108f, w / 2 + 175f, 1458f)
        p.style = Paint.Style.FILL
        p.color = Color.parseColor("#7FE3F8")
        c.drawRoundRect(RectF(qrBox.left - 4, qrBox.top - 4, qrBox.right + 4, qrBox.bottom + 4), 40f, 40f, p)
        p.color = Color.parseColor("#DDF6FD")
        c.drawRoundRect(qrBox, 36f, 36f, p)
        val white = RectF(qrBox.left + 38f, qrBox.top + 38f, qrBox.right - 38f, qrBox.bottom - 38f)
        p.color = Color.WHITE
        c.drawRect(white, p)
        val qr = QrCodeGenerator.generateQrBitmap(employee.qrCode, 600, 600)
        if (qr != null) {
            c.drawBitmap(qr, null, RectF(white.left + 4, white.top + 4, white.right - 4, white.bottom - 4), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        // corner brackets
        p.style = Paint.Style.STROKE
        p.strokeWidth = 8f
        p.color = CYAN
        p.strokeCap = Paint.Cap.ROUND
        val b = 36f
        val o = 14f
        val l = qrBox.left + o; val r = qrBox.right - o
        val t = qrBox.top + o; val bt = qrBox.bottom - o
        c.drawLine(l, t, l + b, t, p); c.drawLine(l, t, l, t + b, p)
        c.drawLine(r, t, r - b, t, p); c.drawLine(r, t, r, t + b, p)
        c.drawLine(l, bt, l + b, bt, p); c.drawLine(l, bt, l, bt - b, p)
        c.drawLine(r, bt, r - b, bt, p); c.drawLine(r, bt, r, bt - b, p)
        p.style = Paint.Style.FILL

        // ---- footer ----
        c.save()
        c.clipPath(clip)
        p.color = NAVY
        val foot = Path().apply {
            moveTo(w, h - 120f)
            lineTo(300f, h - 120f)
            lineTo(215f, h)
            lineTo(w, h)
            close()
        }
        c.drawPath(foot, p)
        c.restore()
        text.textAlign = Paint.Align.RIGHT
        text.color = Color.WHITE
        text.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        text.textSize = 27f
        text.letterSpacing = 0.18f
        c.drawText(FOOTER, w - 62f, h - 52f, text)

        return bmp
    }

    // ---------- helpers ----------

    private fun drawDecorations(c: Canvas, p: Paint, w: Float, h: Float) {
        p.style = Paint.Style.FILL
        // top-left dark shape
        p.color = DARK
        c.drawPath(Path().apply {
            moveTo(30f, 30f); lineTo(300f, 30f); lineTo(120f, 110f); lineTo(120f, 320f); lineTo(30f, 400f); close()
        }, p)
        // left slanted bars
        p.color = NAVY
        c.drawPath(Path().apply {
            moveTo(30f, 740f); lineTo(185f, 580f); lineTo(185f, 640f); lineTo(30f, 780f); close()
        }, p)
        p.color = CYAN
        c.drawRoundRect(RectF(35f, 735f, 160f, 800f), 32f, 32f, p)
        // right bar + square
        p.color = NAVY
        c.drawRoundRect(RectF(735f, 520f, 870f, 610f), 45f, 45f, p)
        p.color = CYAN
        c.drawRect(803f, 427f, 853f, 487f, p)
        // lower-left blocks
        p.color = NAVY
        c.drawPath(Path().apply {
            moveTo(30f, 960f); lineTo(135f, 905f); lineTo(135f, 1120f); lineTo(30f, 1150f); close()
        }, p)
        p.color = CYAN
        c.drawPath(Path().apply {
            moveTo(30f, 1190f); lineTo(98f, 1120f); lineTo(98f, 1240f); lineTo(30f, 1265f); close()
        }, p)
        // lower-right dark corner
        p.color = NAVY
        c.drawPath(Path().apply {
            moveTo(w - 30f, 1230f); lineTo(w - 30f, 1480f); lineTo(670f, 1480f); lineTo(790f, 1390f); lineTo(790f, 1300f); close()
        }, p)
        // faint guilloche circles
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f
        p.color = Color.argb(70, 60, 140, 190)
        listOf(
            Triple(745f, 170f, 135f), Triple(140f, 600f, 110f),
            Triple(765f, 790f, 100f), Triple(110f, 950f, 100f), Triple(740f, 1330f, 110f)
        ).forEach { (x, y, r) ->
            for (i in 0 until 16) {
                c.save()
                c.rotate(i * 22.5f, x, y)
                c.drawOval(RectF(x - r, y - r * 0.55f, x + r, y + r * 0.55f), p)
                c.restore()
            }
        }
        p.style = Paint.Style.FILL
    }

    private fun drawIdIcon(c: Canvas, p: Paint, x: Float, y: Float) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 6f
        p.color = NAVY
        c.drawRoundRect(RectF(x, y, x + 70f, y + 52f), 10f, 10f, p)
        p.style = Paint.Style.FILL
        c.drawCircle(x + 22f, y + 20f, 8f, p)
        c.drawRoundRect(RectF(x + 10f, y + 32f, x + 34f, y + 42f), 5f, 5f, p)
        c.drawRect(x + 42f, y + 17f, x + 60f, y + 22f, p)
        c.drawRect(x + 42f, y + 30f, x + 60f, y + 35f, p)
    }

    private fun fitText(paint: Paint, s: String, maxWidth: Float, startSize: Float, minSize: Float) {
        var size = startSize
        paint.textSize = size
        while (paint.measureText(s) > maxWidth && size > minSize) {
            size -= 2f
            paint.textSize = size
        }
    }

    private fun loadPhoto(context: Context, uriStr: String?): Bitmap? {
        if (uriStr.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriStr)
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            if (uri.scheme == "file") {
                BitmapFactory.decodeFile(uri.path, opts)
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Center-crops the photo into a circle. */
    private fun drawCircularPhoto(c: Canvas, src: Bitmap, cx: Float, cy: Float, radius: Float) {
        val d = (radius * 2).toInt()
        val out = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888)
        val oc = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        oc.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val side = minOf(src.width, src.height)
        // bias crop slightly upward so faces are not cut off
        val left = (src.width - side) / 2
        val top = ((src.height - side) * 0.25f).toInt()
        oc.drawBitmap(src, android.graphics.Rect(left, top, left + side, top + side), RectF(0f, 0f, d.toFloat(), d.toFloat()), paint)
        c.drawBitmap(out, cx - radius, cy - radius, null)
    }

    private fun drawPlaceholderAvatar(c: Canvas, cx: Float, cy: Float, r: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.parseColor("#BFD9E6")
        c.drawCircle(cx, cy, r, p)
        p.color = Color.parseColor("#7FA6BC")
        c.drawCircle(cx, cy - 25f, 48f, p)
        c.save()
        c.clipPath(Path().apply { addCircle(cx, cy, r, Path.Direction.CW) })
        c.drawOval(RectF(cx - 95f, cy + 40f, cx + 95f, cy + 190f), p)
        c.restore()
    }

    // ---------- export / share ----------

    private fun cardsDir(context: Context): File =
        File(context.cacheDir, "reports").apply { mkdirs() }

    fun saveCardPng(context: Context, employee: Employee): File {
        val bmp = generate(context, employee)
        val file = File(cardsDir(context), "ID_${employee.id}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    /** All cards on A4 pages, 6 per page (3 x 2). */
    fun saveAllCardsPdf(context: Context, employees: List<Employee>): File {
        val pageW = 595
        val pageH = 842
        val cardW = 180f
        val cardH = cardW * CARD_H / CARD_W
        val gapX = (pageW - cardW * 3) / 4f
        val gapY = (pageH - cardH * 2) / 3f
        val doc = PdfDocument()
        employees.chunked(6).forEachIndexed { pageIdx, group ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageIdx + 1).create())
            group.forEachIndexed { i, emp ->
                val col = i % 3
                val row = i / 3
                val left = gapX + col * (cardW + gapX)
                val top = gapY + row * (cardH + gapY)
                val bmp = generate(context, emp)
                page.canvas.drawBitmap(
                    bmp, null, RectF(left, top, left + cardW, top + cardH),
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )
                bmp.recycle()
            }
            doc.finishPage(page)
        }
        val file = File(cardsDir(context), "ID_Cards_All.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun shareFile(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Copies a picked gallery image into app storage so it stays available later. */
    fun importPhoto(context: Context, source: Uri, employeeId: String): String? {
        return try {
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val dest = File(dir, "${employeeId.replace(Regex("[^A-Za-z0-9_-]"), "_")}.jpg")
            context.contentResolver.openInputStream(source)?.use { input ->
                val bmp = BitmapFactory.decodeStream(input) ?: return null
                val maxSide = 800
                val scale = minOf(1f, maxSide.toFloat() / maxOf(bmp.width, bmp.height))
                val scaled = if (scale < 1f)
                    Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
                else bmp
                FileOutputStream(dest).use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            }
            Uri.fromFile(dest).toString()
        } catch (_: Exception) {
            null
        }
    }
}
