package com.neon2048.app

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.neon2048.app.databinding.DialogHowToPlayBinding

/** "How to Play" bottom sheet: rules, merging demo, tile ladder and the win logic. */
object HowToPlay {
    fun show(context: Context) {
        val dialog = BottomSheetDialog(context)
        val b = DialogHowToPlayBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(b.root)

        UiFx.neonGradient(b.howTitle)
        b.howGotIt.enablePressScale()
        b.howGotIt.setOnClickListener { dialog.dismiss() }

        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (sheet != null) {
                sheet.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        dialog.show()
    }
}
