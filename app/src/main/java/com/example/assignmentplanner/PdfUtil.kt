package com.example.assignmentplanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import android.util.Log
import java.io.File
import java.io.IOException

object PdfUtil
{
    var pagesRendered = 0
    var lastUri: Uri? = null

    fun pdfToBitmap(context: Context, uri: Uri): Bitmap?
    {
        lastUri = uri
        try
        {
            var pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null)
            {
                var renderer = PdfRenderer(pfd)
                if (renderer.pageCount > 0)
                {
                    var page = renderer.openPage(0)
                    var bitmap = createBitmap(page.width, page.height)
                    var canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    pagesRendered = pagesRendered + 1
                    return bitmap
                }
                else
                {
                    renderer.close()
                    return null
                }
            }
            else return null
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            return null
        }
    }
}
