package com.example.meokpli.Main.Feed

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PhotoMeta(
    val uri: Uri,
    val fileName: String,            // 표시 이름 or fallback
    val mime: String,                // contentResolver getType 또는 기본값
    val sizeBytes: Long?,            // 바이트 크기(알 수 없으면 null)
    val latitude: Double?,           // EXIF GPS (없으면 null)
    val longitude: Double?,          // EXIF GPS (없으면 null)
    val dateTimeOriginalIso: String?,// "yyyy-MM-dd'T'HH:mm:ss" (타임존 반영)
    val orientationDegrees: Int?,    // 0/90/180/270 (없으면 null)
    val width: Int?,                 // 가능하면 채움
    val height: Int?                 // 가능하면 채움
)

/** 단일 URI에서 PhotoMeta 추출 */
@RequiresApi(Build.VERSION_CODES.O)
fun extractPhotoMeta(context: Context, uri: Uri): PhotoMeta {
    val cr = context.contentResolver
    val mime = cr.getType(uri) ?: "application/octet-stream"

    val (name, size) = queryDisplayNameAndSize(cr, uri)
    val displayName = name ?: "photo_${System.currentTimeMillis()}"

    // MediaStore에서 폭/높이, 찍은 시각(백업) 가져오기
    val (msWidth, msHeight, msTakenMs) = queryWidthHeightAndTaken(cr, uri)

    // EXIF 로드 (없는 포맷/스트림이면 null)
    val exif = openExif(cr, uri)

    // EXIF GPS
    val (lat, lng) = exifLatLng(exif)

    // EXIF 촬영 시각 → ISO
    val isoTime = exifDateTimeToIso(exif) ?: msTakenMs?.let { millisToIso(it) }

    // EXIF 회전
    val orientationDeg = exifOrientationDegrees(exif)

    // EXIF 폭/높이(없으면 MediaStore 값으로 대체)
    val width = exif?.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)?.takeIf { it > 0 } ?: msWidth
    val height = exif?.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)?.takeIf { it > 0 } ?: msHeight

    return PhotoMeta(
        uri = uri,
        fileName = displayName,
        mime = mime,
        sizeBytes = size,
        latitude = lat,
        longitude = lng,
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
        MediaStore.Images.Media.DATE_TAKEN,   // ms since epoch
        MediaStore.Images.Media.DATE_ADDED    // s since epoch (fallback)
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

private fun openExif(cr: ContentResolver, uri: Uri): ExifInterface? = runCatching {
    cr.openInputStream(uri)?.use { ExifInterface(it) }
}.getOrNull()

private fun exifLatLng(exif: ExifInterface?): Pair<Double?, Double?> {
    if (exif == null) return null to null
    val out = FloatArray(2)
    return if (exif.getLatLong(out)) out[0].toDouble() to out[1].toDouble() else null to null
}

private fun exifOrientationDegrees(exif: ExifInterface?): Int? {
    if (exif == null) return null
    return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED -> 0
        else -> 0
    }
}

/** EXIF 시간 → ISO_LOCAL_DATE_TIME (가능하면 오프셋 적용) */
@RequiresApi(Build.VERSION_CODES.O)
private fun exifDateTimeToIso(exif: ExifInterface?): String? {
    exif ?: return null

    // 우선순위: Original > Digitized > Datetime
    val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
        ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        ?: return null

    // "yyyy:MM:dd HH:mm:ss"
    val base = try {
        val fmt = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US)
        LocalDateTime.parse(raw, fmt)
    } catch (_: Throwable) { return null }

    // EXIF 오프셋(+09:00 등)이 있으면 적용
    val offset =
        exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_DIGITIZED)
            ?: exif.getAttribute(ExifInterface.TAG_OFFSET_TIME)

    return if (offset != null) {
        val odt = OffsetDateTime.of(base, ZoneOffset.of(offset))
        odt.withSecond(odt.second).withNano(0).toLocalDateTime().toString()
    } else {
        // 오프셋 없으면 시스템 타임존 가정
        base.atZone(ZoneId.systemDefault()).toLocalDateTime().withSecond(0).withNano(0).toString()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun millisToIso(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime()
        .withSecond(0).withNano(0).toString()
