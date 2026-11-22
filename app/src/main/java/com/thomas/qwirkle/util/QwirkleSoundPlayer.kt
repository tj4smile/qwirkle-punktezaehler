package com.thomas.qwirkle.util

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager

/**
 * Spielt bei einem Qwirkle den Standard-Benachrichtigungston des Android-Systems.
 * Er folgt der Lautstärke und dem Stumm-Modus des Geräts. Fehler (z. B. kein Ton
 * eingestellt) werden ignoriert, die App läuft dann einfach ohne Ton weiter.
 */
class QwirkleSoundPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var ringtone: Ringtone? = null

    fun play() {
        try {
            ringtone?.stop()
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
            val next = RingtoneManager.getRingtone(appContext, uri) ?: return
            next.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            next.play()
            ringtone = next
        } catch (e: Exception) {
            // ohne Ton weiterspielen
        }
    }

    fun release() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            // ignorieren
        }
        ringtone = null
    }
}
