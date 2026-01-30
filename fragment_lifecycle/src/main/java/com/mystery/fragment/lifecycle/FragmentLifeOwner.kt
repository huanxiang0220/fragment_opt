package com.mystery.fragment.lifecycle

import androidx.lifecycle.LifecycleOwner

interface FragmentLifeOwner : LifecycleOwner {

    fun getFragmentLifecycle(): FragmentLifecycle
}