package com.mystery.fragment.lifecycle

internal class FragmentLifecycleRegistry : FragmentLifecycle() {

    companion object {

        @JvmStatic
        fun create(): FragmentLifecycleRegistry {
            return FragmentLifecycleRegistry()
        }
    }

    fun onFragmentResume() {
        val callbacks = collectLifecycleCallbacks()
        callbacks?.forEach {
            (it as IFragmentLifecycleCallbacks).onFragmentResume()
        }
    }

    fun onFragmentPause() {
        val callbacks = collectLifecycleCallbacks()
        callbacks?.forEach {
            (it as IFragmentLifecycleCallbacks).onFragmentPause()
        }
    }
}