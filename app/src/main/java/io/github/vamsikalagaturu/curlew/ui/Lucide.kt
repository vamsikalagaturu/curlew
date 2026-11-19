package io.github.vamsikalagaturu.curlew.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// Path data from lucide-static 1.62.0 (ISC licence, Lucide Icons and Contributors), the icon set jackdaw and
// ferret use. Icons tint the black stroke at draw time, so callers colour them like any other ImageVector.
object Lucide {
    val Phone by icon(PHONE)
    val PhoneOff by icon(
        "M10.1 13.9a14 14 0 0 0 3.732 2.668 1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2 18 18 0 0 1-12.728-5.272",
        "M22 2 2 22",
        "M4.76 13.582A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233 14 14 0 0 0 .244.473",
    )
    val PhoneMissed by icon("m16 2 6 6", "m22 2-6 6", PHONE)
    val PhoneCall by icon("M13 2a9 9 0 0 1 9 9", "M13 6a5 5 0 0 1 5 5", PHONE)
    val PhoneForwarded by icon("M14 6h8", "m18 2 4 4-4 4", PHONE)
    val ArrowUpRight by icon("M7 7h10v10", "M7 17 17 7")
    val ArrowDownLeft by icon("M17 7 7 17", "M17 17H7V7")
    val Ban by icon(circle(12f, 12f, 10f), "M4.929 4.929 19.07 19.071")
    val UserPlus by icon("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", circle(9f, 7f, 4f), "M19 8v6", "M22 11h-6")
    val MicOff by icon(
        "M12 19v3", "M15 9.34V5a3 3 0 0 0-5.68-1.33", "M16.95 16.95A7 7 0 0 1 5 12v-2",
        "M18.89 13.23A7 7 0 0 0 19 12v-2", "m2 2 20 20", "M9 9v3a3 3 0 0 0 5.12 2.12",
    )
    val Volume2 by icon(
        "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z",
        "M16 9a5 5 0 0 1 0 6",
        "M19.364 18.364a9 9 0 0 0 0-12.728",
    )
    val Pause by icon(rect(14f, 3f, 5f, 18f, 1f), rect(5f, 3f, 5f, 18f, 1f))
    val Merge by icon("m8 6 4-4 4 4", "M12 2v10.3a4 4 0 0 1-1.172 2.872L4 22", "m20 22-5-5")
    val ArrowLeftRight by icon("M8 3 4 7l4 4", "M4 7h16", "m16 21 4-4-4-4", "M20 17H4")
    val Bluetooth by icon("m7 7 10 10-5 5V2l5 5L7 17")
    val Headphones by icon(
        "M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3",
    )
    val BookUser by icon(
        "M15 13a3 3 0 1 0-6 0",
        "M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H19a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6.5a1 1 0 0 1 0-5H20",
        circle(12f, 8f, 2f),
    )
    val Settings by icon(
        "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915",
        circle(12f, 12f, 3f),
    )
    val Delete by icon(
        "M10 5a2 2 0 0 0-1.344.519l-6.328 5.74a1 1 0 0 0 0 1.481l6.328 5.741A2 2 0 0 0 10 19h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2z",
        "m12 9 6 6",
        "m18 9-6 6",
    )
    val Contrast by icon(circle(12f, 12f, 10f), "M12 18a6 6 0 0 0 0-12v12z")
    val Vibrate by icon("m2 8 2 2-2 2 2 2-2 2", "m22 8-2 2 2 2-2 2 2 2", rect(8f, 5f, 8f, 14f, 1f))
    val CardSim by icon(
        "M12 14v4",
        "M14.172 2a2 2 0 0 1 1.414.586l3.828 3.828A2 2 0 0 1 20 7.828V20a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z",
        "M8 14h8",
        rect(8f, 10f, 8f, 8f, 2f),
    )
    val Info by icon(circle(12f, 12f, 10f), "M12 16v-4", "M12 8h.01")
    val Camera by icon(
        "M13.997 4a2 2 0 0 1 1.76 1.05l.486.9A2 2 0 0 0 18.003 7H20a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h1.997a2 2 0 0 0 1.759-1.048l.489-.904A2 2 0 0 1 10.004 4z",
        circle(12f, 13f, 3f),
    )
    val Cloud by icon("M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z")
    val Mail by icon("m22 7-8.991 5.727a2 2 0 0 1-2.009 0L2 7", rect(2f, 4f, 20f, 16f, 2f))
    val Code by icon("m16 18 6-6-6-6", "m8 6-6 6 6 6")
    val Download by icon("M12 15V3", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m7 10 5 5 5-5")
    val History by icon("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5", "M12 7v5l4 2")
    val Search by icon("m21 21-4.34-4.34", circle(11f, 11f, 8f))
    val X by icon("M18 6 6 18", "m6 6 12 12")
    val Check by icon("M20 6 9 17l-5-5")
    val Plus by icon("M5 12h14", "M12 5v14")
    val Pencil by icon(
        "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z",
        "m15 5 4 4",
    )
    val Share by icon(
        circle(18f, 5f, 3f), circle(6f, 12f, 3f), circle(18f, 19f, 3f),
        "M8.59 13.51 15.42 17.49", "M15.41 6.51 8.59 10.49",
    )
    val Trash by icon(
        "M10 11v6", "M14 11v6", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M3 6h18",
        "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2",
    )
    val User by icon("M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", circle(12f, 7f, 4f))
    val ChevronDown by icon("m6 9 6 6 6-6")
    val ChevronLeft by icon("m15 18-6-6 6-6")
    val Moon by icon(
        "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401",
    )
    val Music by icon("M21 16V3a1 1 0 0 0-1.28-.96l-10 2.917a1 1 0 0 0-.72.96V19", circle(18f, 16f, 3f), circle(6f, 19f, 3f))
    val RefreshCw by icon(
        "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8", "M21 3v5h-5",
        "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16", "M8 16H3v5",
    )
    val AppWindow by icon(rect(2f, 4f, 20f, 16f, 2f), "M10 4v4", "M2 8h20", "M6 4v4")
    val Image by icon(rect(3f, 3f, 18f, 18f, 2f), circle(9f, 9f, 2f), "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21")

    val Star by icon(STAR)
    val StarFilled by icon(STAR, filled = true)
    val MessageSquare by icon(
        "M22 17a2 2 0 0 1-2 2H6.828a2 2 0 0 0-1.414.586l-2.202 2.202A.71.71 0 0 1 2 21.286V5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2z",
    )
    val BellOff by icon(
        "M10.268 21a2 2 0 0 0 3.464 0",
        "M17 17H4a1 1 0 0 1-.74-1.673C4.59 13.956 6 12.499 6 8a6 6 0 0 1 .258-1.742",
        "m2 2 20 20",
        "M8.668 3.01A6 6 0 0 1 18 8c0 2.687.77 4.653 1.707 6.05",
    )
    val UserX by icon("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", circle(9f, 7f, 4f), "M17 8l5 5", "M22 8l-5 5")
    val Hash by icon("M4 9h16", "M4 15h16", "M10 3 8 21", "M16 3l-2 18")

    // Not in Lucide: a phone keypad drawn the way Lucide's grip icons draw their dots.
    val Keypad by icon(
        circle(6f, 4f, 1f), circle(12f, 4f, 1f), circle(18f, 4f, 1f),
        circle(6f, 10f, 1f), circle(12f, 10f, 1f), circle(18f, 10f, 1f),
        circle(6f, 16f, 1f), circle(12f, 16f, 1f), circle(18f, 16f, 1f),
        circle(12f, 21f, 1f),
    )
}

private const val PHONE =
    "M13.832 16.568a1 1 0 0 0 1.213-.303l.355-.465A2 2 0 0 1 17 15h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2A18 18 0 0 1 2 4a2 2 0 0 1 2-2h3a2 2 0 0 1 2 2v3a2 2 0 0 1-.8 1.6l-.468.351a1 1 0 0 0-.292 1.233 14 14 0 0 0 6.392 6.384"

private const val STAR =
    "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z"

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) =
    "M${x + rx} ${y}h${w - 2 * rx}a$rx $rx 0 0 1 $rx ${rx}v${h - 2 * rx}a$rx $rx 0 0 1 ${-rx} ${rx}" +
        "h${-(w - 2 * rx)}a$rx $rx 0 0 1 ${-rx} ${-rx}v${-(h - 2 * rx)}a$rx $rx 0 0 1 $rx ${-rx}z"

private fun icon(vararg paths: String, filled: Boolean = false) = lazy {
    ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        paths.forEach {
            addPath(
                pathData = PathParser().parsePathString(it).toNodes(),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
}
