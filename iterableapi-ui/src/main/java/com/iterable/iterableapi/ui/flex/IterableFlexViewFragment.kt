package com.iterable.iterableapi.ui.flex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.iterable.iterableapi.*
import com.iterable.iterableapi.ui.R
import com.iterable.iterableapi.ui.databinding.FragmentIterableFlexViewBinding

class IterableFlexViewFragment : Fragment() {

    private lateinit var viewModel: IterableFlexViewViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding: FragmentIterableFlexViewBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_iterable_flex_view, container, false)

        viewModel = ViewModelProvider(this).get(IterableFlexViewViewModel::class.java)

        viewModel.flexMessage.observe(viewLifecycleOwner, Observer { newMessage ->
            binding.flexMessage = newMessage
        })

        return binding.root
    }
}