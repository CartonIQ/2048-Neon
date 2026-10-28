package com.neon2048.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neon2048.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val vm: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = Prefs.get(requireContext())

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(300).start()

        // Restore current selections before attaching listeners.
        binding.themeGroup.check(
            when (prefs.getInt(Prefs.KEY_NIGHT, Prefs.NIGHT_SYSTEM)) {
                Prefs.NIGHT_LIGHT -> R.id.radio_light
                Prefs.NIGHT_DARK -> R.id.radio_dark
                else -> R.id.radio_system
            }
        )
        binding.tileGroup.check(
            when (TileTheme.fromIndex(prefs.getInt(Prefs.KEY_TILE_THEME, 0))) {
                TileTheme.NEON -> R.id.radio_neon
                TileTheme.CLASSIC -> R.id.radio_classic
                TileTheme.PASTEL -> R.id.radio_pastel
            }
        )
        binding.switchSound.isChecked = prefs.getBoolean(Prefs.KEY_SOUND, true)
        binding.switchHaptics.isChecked = prefs.getBoolean(Prefs.KEY_HAPTICS, true)

        binding.themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.radio_light -> Prefs.NIGHT_LIGHT
                R.id.radio_dark -> Prefs.NIGHT_DARK
                else -> Prefs.NIGHT_SYSTEM
            }
            prefs.edit().putInt(Prefs.KEY_NIGHT, value).apply()
            AppCompatDelegate.setDefaultNightMode(Prefs.nightMode(requireContext()))
        }

        binding.tileGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.radio_classic -> TileTheme.CLASSIC
                R.id.radio_pastel -> TileTheme.PASTEL
                else -> TileTheme.NEON
            }
            prefs.edit().putInt(Prefs.KEY_TILE_THEME, theme.ordinal).apply()
        }

        binding.switchSound.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(Prefs.KEY_SOUND, checked).apply()
            if (checked) Feedback.merged(binding.root, 64)
        }
        binding.switchHaptics.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(Prefs.KEY_HAPTICS, checked).apply()
            if (checked) Feedback.click(binding.root)
        }

        UiFx.neonGradient(binding.settingsTitle)
        binding.rowHowTo.enablePressScale()
        binding.rowHowTo.setOnClickListener { HowToPlay.show(requireContext()) }
        binding.rowRate.enablePressScale()
        binding.rowPrivacy.enablePressScale()
        binding.rowReset.enablePressScale()
        binding.rowRate.setOnClickListener { openStore() }
        binding.rowPrivacy.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.privacy_policy)
                .setMessage(R.string.privacy_text)
                .setPositiveButton(R.string.close, null)
                .show()
        }
        binding.rowReset.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reset_title)
                .setMessage(R.string.reset_message)
                .setPositiveButton(R.string.reset_confirm) { _, _ ->
                    vm.resetStats()
                    Toast.makeText(requireContext(), R.string.reset_done, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun openStore() {
        val pkg = requireContext().packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                    )
                )
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(requireContext(), R.string.store_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
