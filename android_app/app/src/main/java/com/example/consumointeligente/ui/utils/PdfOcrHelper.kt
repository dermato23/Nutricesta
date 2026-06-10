package com.example.consumointeligente.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

object PdfOcrHelper {
    private const val TAG = "PdfOcrHelper"

    /**
     * Procesa un PDF desde una URI, renderiza sus páginas a Bitmap, y extrae el texto usando ML Kit.
     * Retorna el texto combinado de todas las páginas procesadas.
     */
    suspend fun processPdfFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val extractedText = StringBuilder()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        try {
            // Se usa contentResolver para abrir el archivo
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                val pdfRenderer = PdfRenderer(descriptor)
                val pageCount = pdfRenderer.pageCount
                
                for (i in 0 until pageCount) {
                    pdfRenderer.openPage(i).use { page ->
                        // Renderizar a un bitmap (ajusta la escala para mejor resolución si es necesario)
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2,
                            page.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        // Llenar con fondo blanco
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        
                        // Procesar Bitmap con ML Kit
                        val image = InputImage.fromBitmap(bitmap, 0)
                        val result = recognizer.process(image).await()
                        extractedText.append(result.text).append("\n")
                    }
                }
                pdfRenderer.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando el PDF", e)
        }
        
        return@withContext extractedText.toString()
    }
}
