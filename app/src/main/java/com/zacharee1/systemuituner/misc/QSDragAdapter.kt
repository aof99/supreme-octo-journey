package com.zacharee1.systemuituner.misc

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.zacharee1.systemuituner.R
import com.zacharee1.systemuituner.util.writeSecure
import java.util.*
import java.util.regex.Pattern

class QSDragAdapter(private val context: Context) : RecyclerView.Adapter<QSDragAdapter.QSViewHolder>() {
    var tiles = ArrayList<QSTile>()

    var availableTiles = ArrayList<QSTile>()

    private val defaultTiles: ArrayList<QSTile>
        get() {
            val order = defaultTileOrder
            val array = order.split(",")

            return array.mapTo(ArrayList()) { QSTile(it, context) }
        }

    private val defaultTileOrder: String
        get() {
            val pm = context.packageManager

            return try {
                val resources = pm.getResourcesForApplication("com.android.systemui")
                val id = resources.getIdentifier("quick_settings_tiles_default", "string", "com.android.systemui")

                resources.getString(id)
            } catch (e: Exception) {
                ""
            }

        }

    init {
        parseTileList()
    }

    fun parseTileList() {
        var tiles: String? = Settings.Secure.getString(context.contentResolver, "sysui_qs_tiles")

        if (tiles == null || tiles.isEmpty()) {
            tiles = defaultTileOrder
        }

        val tileArray = tiles.split(",")

        val tempTiles = tileArray.map { QSTile(it, context) }

        this.tiles.clear()
        this.tiles.addAll(tempTiles)

        refreshAvailableTiles()
    }

    private fun refreshAvailableTiles() {
        availableTiles.clear()
        for (tile in defaultTiles) {
            val hasTile = tiles.any { it.key == tile.key }

            if (!hasTile) {
                availableTiles.add(tile)
            }
        }
    }

    fun addTile(tile: QSTile) {
        tiles.add(tile)
        notifyDataSetChanged()

        setOrder(tiles)
        refreshAvailableTiles()
    }

    fun removeTile(tile: QSTile) {
        tiles.remove(tile)
        notifyDataSetChanged()

        setOrder(tiles)
        refreshAvailableTiles()
    }

    fun setOrder(tiles: ArrayList<QSDragAdapter.QSTile>) {
        val keys = tiles.map { it.key }

        val tileString = TextUtils.join(",", keys)

        context.writeSecure("sysui_qs_tiles", tileString)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QSViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.qs_tile_layout, parent, false)
        return QSViewHolder(view)
    }

    override fun onBindViewHolder(holder: QSViewHolder, position: Int) {
        holder.setTitle(tiles[holder.adapterPosition].title)
        holder.setIcon(tiles[holder.adapterPosition].icon)
        holder.setCloseListener(View.OnClickListener {
            AlertDialog.Builder(context)
                    .setTitle(R.string.removing_tile)
                    .setMessage(String.format(holder.context.resources.getString(R.string.remove_tile), tiles[holder.adapterPosition].title))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        removeTile(tiles[holder.adapterPosition])
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
        })
    }

    override fun getItemCount(): Int {
        return tiles.size
    }

    class QSViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val context: Context
            get() = itemView.context

        init {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                itemView.setOnLongClickListener {
                    showClose()
                    true
                }
                itemView.setOnClickListener {
                    hideClose()
                }
            }
        }

        fun setTitle(title: String) {
            val textView = itemView.findViewById<TextView>(R.id.textView)
            textView.text = title
        }

        fun setIcon(icon: Drawable) {
            val imageView = itemView.findViewById<ImageView>(R.id.imageView)
            imageView.setImageDrawable(icon)
        }

        fun setCloseListener(listener: View.OnClickListener) {
            itemView.findViewById<View>(R.id.close_button).setOnClickListener(listener)
        }

        private fun showClose() {
            val closeButton = itemView.findViewById<ImageView>(R.id.close_button)

            closeButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(OvershootInterpolator())
                    .start()
        }

        private fun hideClose() {
            val closeButtom = itemView.findViewById<ImageView>(R.id.close_button)

            closeButtom.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setInterpolator(AnticipateInterpolator())
                    .start()
        }
    }

    class QSTile(var key: String, context: Context) {
        private var parser: TileParser = TileParser(key, context)

        var title = parser.title
        var icon = parser.icon
    }

    class TileParser(var key: String, private val mContext: Context) {
        lateinit var icon: Drawable
        lateinit var title: String

        init {
            parseKey()
        }

        private fun parseKey() {
            when {
                key.toLowerCase().contains("intent(") -> parseIntent()
                key.toLowerCase().contains("custom(") -> parseCustom()
                else -> parseStandard()
            }
        }

        private fun parseIntent() {
            val drawable = mContext.resources.getDrawable(R.drawable.ic_android_black_24dp, null)
            drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)

            icon = drawable

            val p = Pattern.compile("\\((.*?)\\)")
            val m = p.matcher(key)

            var title = ""

            while (!m.hitEnd()) {
                if (m.find()) title = m.group()
            }

            this.title = title.replace("(", "").replace(")", "")
        }

        private fun parseCustom() {
            val p = Pattern.compile("\\((.*?)\\)")
            val m = p.matcher(key)

            var name = ""

            while (!m.hitEnd()) {
                if (m.find()) name = m.group()
            }

            name = name.replace("(", "").replace(")", "")

            val packageName = name.split("/")[0]
            val component = name.split("/")[1]

            icon = try {
                mContext.packageManager.getServiceInfo(ComponentName(packageName, "$packageName$component"), 0).loadIcon(mContext.packageManager)
            } catch (e: Exception) {
                e.printStackTrace()
                mContext.resources.getDrawable(R.drawable.ic_android_black_24dp, null)
            }
            icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)

            title = try {
                mContext.packageManager.getServiceInfo(ComponentName(packageName, "$packageName$component"), 0).loadLabel(mContext.packageManager).toString()
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val split = component.split(".")
                    split[split.size - 1]
                } catch (ex: Exception) {
                    packageName
                }
            }

        }

        private fun parseStandard() {
            title = capitalize(key.toLowerCase())

            val iconRes = when (key.toLowerCase()) {
                "wifi" -> R.drawable.ic_signal_wifi_4_bar_black_24dp
                "bluetooth", "bt" -> R.drawable.ic_bluetooth_black_24dp
                "color_inversion", "inversion" -> R.drawable.ic_invert_colors_black_24dp
                "cell" -> R.drawable.ic_signal_cellular_4_bar_black_24dp
                "do_not_disturb", "dnd" -> R.drawable.ic_do_not_disturb_on_black_24dp
                "airplane" -> R.drawable.ic_airplanemode_active_black_24dp
                "cast" -> R.drawable.ic_cast_black_24dp
                "location" -> R.drawable.ic_location_on_black_24dp
                "rotation" -> R.drawable.ic_screen_rotation_black_24dp
                "flashlight" -> R.drawable.ic_highlight_black_24dp
                "hotspot" -> R.drawable.ic_wifi_tethering_black_24dp
                "battery" -> R.drawable.ic_battery_full_black_24dp
                "sound" -> R.drawable.ic_volume_up_black_24dp
                "sync" -> R.drawable.ic_sync_black_24dp
                "nfc" -> R.drawable.ic_nfc_black_24dp
                "data" -> R.drawable.ic_data_usage_black_24dp
                else -> R.drawable.ic_android_black_24dp
            }

            val drawable = mContext.resources.getDrawable(iconRes, null).current.mutate()
            drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)

            icon = drawable
        }

        private fun capitalize(string: String): String {
            val builder = StringBuilder()

            val words = string.split(" ")
            words
                    .filter { it.isNotEmpty() }
                    .forEach { builder.append("${it[0].toUpperCase()}${it.substring(1, it.length)}") }

            return builder.toString()
        }
    }
}
