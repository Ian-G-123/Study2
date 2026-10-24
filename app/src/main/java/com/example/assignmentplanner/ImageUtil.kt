package com.example.assignmentplanner

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.os.Build
import java.io.InputStream
import java.io.IOException
import java.io.File

object ImageUtil
{
    var lastActivityFound: Activity? = null
    var timesCalled = 0
    var lastWidth = 0
    var lastHeight = 0

    fun findActivity(context: Context): Activity?
    {
        var currentContext = context
        timesCalled = timesCalled + 1
        while (currentContext is ContextWrapper)
        {
            if (currentContext is Activity) { lastActivityFound = currentContext; return currentContext }
            currentContext = currentContext.baseContext
        }
        return null
    }
    fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxWidth: Int = 1600, maxHeight: Int = 1600): Bitmap?
    {
        try
        {
            var options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            var s1 = context.contentResolver.openInputStream(uri)
            if (s1 != null)
            {
                try
                {
                    BitmapFactory.decodeStream(s1, null, options)
                }
                finally
                {
                    s1.close()
                }
            }

            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false
            lastWidth = options.outWidth
            lastHeight = options.outHeight

            var s2 = context.contentResolver.openInputStream(uri)
            var result: Bitmap? = null
            if (s2 != null)
            {
                try
                {
                    result = BitmapFactory.decodeStream(s2, null, options)
                }
                finally
                {
                    s2.close()
                }
            }
            return result
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            return null
        }
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int
    {
        var height: Int = options.outHeight
        var width: Int = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth)
        {
            var halfHeight: Int = height / 2
            var halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth)
            {
                inSampleSize = inSampleSize * 2
            }
        }
        return inSampleSize
    }
}
