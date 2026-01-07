package com.iterable.inbox_customization

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.iterable.inbox_customization.customizations.*
import com.iterable.inbox_customization.tabs.onSimpleInboxClicked

class MainFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        view.findViewById<Button>(R.id.simpleInboxButton).setOnClickListener { onSimpleInboxClicked() }
        view.findViewById<Button>(R.id.customCellButton).setOnClickListener { onInboxWithCustomCellClicked() }
        view.findViewById<Button>(R.id.dateFormatButton).setOnClickListener { onChangeDateFormatClicked() }
        view.findViewById<Button>(R.id.sortDateButton).setOnClickListener { onSortByDateAscendingClicked() }
        view.findViewById<Button>(R.id.sortTitleButton).setOnClickListener { onSortByTitleAscendingClicked() }
        view.findViewById<Button>(R.id.filterMessageTypeButton).setOnClickListener { onFilterByMessageTypeClicked() }
        view.findViewById<Button>(R.id.filterMessageTitleButton).setOnClickListener { onFilterByMessageTitleClicked() }
        view.findViewById<Button>(R.id.cellTypesButton).setOnClickListener { onInboxWithMultipleCellTypesClicked() }
        view.findViewById<Button>(R.id.additionalFieldsButton).setOnClickListener { onInboxWithAdditionalFieldsClicked() }
        view.findViewById<Button>(R.id.configurationButton).setOnClickListener {
            findNavController().navigate(R.id.configurationFragment)
        }

        // Handle window insets for safe areas (camera cutouts, status bar, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                top = insets.top,
                bottom = insets.bottom,
                left = insets.left,
                right = insets.right
            )
            windowInsets
        }

        return view
    }
}