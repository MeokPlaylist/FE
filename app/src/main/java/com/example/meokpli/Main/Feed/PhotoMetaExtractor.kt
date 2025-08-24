package com.example.meokpli.Main.Feed

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.GpsDirectory
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PhotoMeta(
    val uri: Uri,
    val fileName: String,
    val mime: String,
    val sizeBytes: Long?,
    val latitude: Double?,
    val longitude: Double?,
    val dateTimeOriginalIso: String?,
    val orientationDegrees: Int?,
    val width: Int?,
    val height: Int?
)

@RequiresApi(Build.VERSION_CODES.O)
fun extractPhotoMeta(context: Context, uri: Uri): PhotoMeta {
    val cr = context.contentResolver
    val mime = cr.getType(uri) ?: "application/octet-stream"

    val (name, size) = queryDisplayNameAndSize(cr, uri)
    val displayName = name ?: "photo_${System.currentTimeMillis()}"

    val (msWidth, msHeight, msTakenMs) = queryWidthHeightAndTaken(cr, uri)

    // 실제 파일로 복사해서 다룸
    val realPath = resolveRealPath(context, uri)
    val file = if (realPath != null) File(realPath) else copyUriToFile(context, uri)

    // 1) ExifInterface
    val exif = ExifInterface(file.absolutePath)

    // 우선 ExifInterface에서 위도/경도 직접 시도
    var lat: Double? = null
    var lon: Double? = null
    runCatching {
        val out = FloatArray(2)
        if (exif.getLatLong(out)) {
            lat = out[0].toDouble()
            lon = out[1].toDouble()
        }
    }

    // 2) ExifInterface 실패 시 metadata-extractor로 보강
    if (lat == null || lon == null) {
        runCatching {
            val metadata = ImageMetadataReader.readMetadata(file)
            val gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)
            val geoLoc = gpsDir?.geoLocation
            if (geoLoc != null && !geoLoc.latitude.isNaN() && !geoLoc.longitude.isNaN()) {
                lat = geoLoc.latitude
                lon = geoLoc.longitude
            }
        }
    }

    // 촬영 시각
    val isoTime = exifDateTimeToIso(exif) ?: msTakenMs?.let { millisToIso(it) }

    val orientationDeg = exifOrientationDegrees(exif)

    val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).takeIf { it > 0 } ?: msWidth
    val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).takeIf { it > 0 } ?: msHeight

    return PhotoMeta(
        uri = uri,
        fileName = displayName,
        mime = mime,
        sizeBytes = size,
        latitude = lat,
        longitude = lon,
        dateTimeOriginalIso = isoTime,
        orientationDegrees = orientationDeg,
        width = width,
        height = height
    )
}

/* ---------- helpers ---------- */

private fun queryDisplayNameAndSize(cr: ContentResolver, uri: Uri): Pair<String?, Long?> {
    val cols = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
    return cr.query(uri, cols, null, null, null)?.use { c ->
        val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
        if (c.moveToFirst()) {
            val n = if (nameIdx >= 0) c.getString(nameIdx) else null
            val s = if (sizeIdx >= 0) c.getLong(sizeIdx) else null
            n to s
        } else null to null
    } ?: (null to null)
}

private data class MsInfo(val width: Int?, val height: Int?, val dateTakenMs: Long?)
private fun queryWidthHeightAndTaken(cr: ContentResolver, uri: Uri): MsInfo {
    val cols = arrayOf(
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED
    )
    return cr.query(uri, cols, null, null, null)?.use { c ->
        val wIdx = c.getColumnIndex(MediaStore.Images.Media.WIDTH)
        val hIdx = c.getColumnIndex(MediaStore.Images.Media.HEIGHT)
        val tIdx = c.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
        val aIdx = c.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
        if (c.moveToFirst()) {
            val w = if (wIdx >= 0) c.getInt(wIdx) else null
            val h = if (hIdx >= 0) c.getInt(hIdx) else null
            val taken = if (tIdx >= 0) c.getLong(tIdx) else null
            val added = if (aIdx >= 0) c.getLong(aIdx) else null
            MsInfo(w, h, taken ?: added?.let { it * 1000L })
        } else MsInfo(null, null, null)
    } ?: MsInfo(null, null, null)
}

private fun copyUriToFile(context: Context, uri: Uri): File {
    // 위치 메타데이터가 지워지지 않은 "원본"을 요구
    val src = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try { MediaStore.setRequireOriginal(uri) } catch (_: Throwable) { uri }
    } else uri

    val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(src)!!.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    return file
}

private fun exifOrientationDegrees(exif: ExifInterface?): Int? {
    if (exif == null) return null
    return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun exifDateTimeToIso(exif: ExifInterface?): String? {
    exif ?: return null
    val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
        ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        ?: return null

    val base = try {
        val fmt = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US)
        LocalDateTime.parse(raw, fmt)
    } catch (_: Throwable) { return null }

    val offset = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
        ?: exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED)
        ?: exif.getAttribute(ExifInterface.TAG_OFFSET_TIME)

    return if (offset != null) {
        val odt = OffsetDateTime.of(base, ZoneOffset.of(offset))
        odt.withSecond(0).withNano(0).toLocalDateTime().toString()
    } else {
        base.atZone(ZoneId.systemDefault()).toLocalDateTime().withSecond(0).withNano(0).toString()
    }
}

private fun resolveRealPath(context: Context, uri: Uri): String? {
    val proj = arrayOf(MediaStore.Images.Media.DATA)
    context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
        val col = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        if (cursor.moveToFirst()) {
            return cursor.getString(col) // 실제 파일 경로
        }
    }
    return null
}


@RequiresApi(Build.VERSION_CODES.O)
private fun millisToIso(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime()
        .withSecond(0).withNano(0).toString()
