package com.kashsuks.seekerdisplay.data.media

import android.service.notification.NotificationListenerService

/**
 * Does nothing itself. Android only lets an app read other apps' media sessions if it holds
 * notification access, which is granted by enabling this service in system settings.
 */
class MediaListenerService : NotificationListenerService()
