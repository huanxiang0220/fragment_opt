package com.zhitongcaijin.ztc.fragment.life

import androidx.lifecycle.LifecycleOwner

interface FragmentLifeOwner : LifecycleOwner {

    fun getFragmentLifecycle(): FragmentLifecycle
}