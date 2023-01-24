package com.iterable.iterableapi.ui.flex;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import com.iterable.iterableapi.ui.R;

public class IterableFlexViewFragment extends Fragment {

    var flexMessages =

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.iterable_flex_view_fragment, container, false);
    }
}
