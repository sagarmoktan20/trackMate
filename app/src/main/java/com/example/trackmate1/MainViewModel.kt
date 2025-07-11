package com.example.trackmate1

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {

    val visiblePermissionDialogQueue = mutableStateListOf<String>()


    fun dismissDialog() {
        visiblePermissionDialogQueue.removeAt(0)
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ) {
        android.util.Log.d("QUEUE_DEBUG", "onPermissionResult called for $permission, isGranted: $isGranted, current queue: $visiblePermissionDialogQueue")
        
        if (isGranted) {
            // If the permission is granted, remove it from the queue if it was there
            val wasRemoved = visiblePermissionDialogQueue.remove(permission)
            android.util.Log.d("QUEUE_DEBUG", "Permission $permission granted, removed from queue: $wasRemoved, new queue: $visiblePermissionDialogQueue")
        } else {
            // Only add to the queue if it's not already there and not granted
            if (!visiblePermissionDialogQueue.contains(permission)) {
                visiblePermissionDialogQueue.add(permission)
                android.util.Log.d("QUEUE_DEBUG", "Permission $permission denied, added to queue, new queue: $visiblePermissionDialogQueue")
            } else {
                android.util.Log.d("QUEUE_DEBUG", "Permission $permission already in queue, not adding again")
            }
        }
    }
}