package com.mystery.fragment.lifecycle

open class FragmentLifecycle {

    private val mNestedLazyFragmentLifecycleCallbacks =
        ArrayList<IFragmentLifecycleCallbacks>()

    fun registerLifecycleCallbacks(callback: IFragmentLifecycleCallbacks) {
        synchronized(mNestedLazyFragmentLifecycleCallbacks) {
            mNestedLazyFragmentLifecycleCallbacks.add(callback)
        }
    }

    fun unregisterLifecycleCallbacks(callback: IFragmentLifecycleCallbacks?) {
        synchronized(mNestedLazyFragmentLifecycleCallbacks) {
            mNestedLazyFragmentLifecycleCallbacks.remove(callback)
        }
    }

    internal fun collectLifecycleCallbacks(): Array<Any>? {
        var callbacks: Array<Any>? = null
        synchronized(mNestedLazyFragmentLifecycleCallbacks) {
            if (mNestedLazyFragmentLifecycleCallbacks.isNotEmpty()) {
                callbacks = mNestedLazyFragmentLifecycleCallbacks.toTypedArray()
            }
        }
        return callbacks
    }

}