/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.quantumbadger.redreader.common

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.quantumbadger.redreader.receivers.NewMessageChecker
import org.quantumbadger.redreader.receivers.RegularCachePruner

object Alarms {
    private val alarmMap: MutableMap<Alarm?, AlarmManager?> = HashMap<Alarm?, AlarmManager?>()
    private val intentMap: MutableMap<Alarm?, PendingIntent?> = HashMap<Alarm?, PendingIntent?>()

    /**
     * Starts the specified alarm
     */
    fun startAlarm(alarm: Alarm, context: Context) {
        if (!alarmMap.containsKey(alarm)) {
            val alarmIntent = Intent(context, alarm.alarmClass())

            var flags = 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }

            @SuppressLint("UnspecifiedImmutableFlag") val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    0,
                    alarmIntent,
                    flags
                )

            val alarmManager = (context.getSystemService(Context.ALARM_SERVICE)) as AlarmManager
            alarmManager.setInexactRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis(),
                alarm.interval(),
                pendingIntent
            )

            alarmMap.put(alarm, alarmManager)
            intentMap.put(alarm, pendingIntent)
        }
    }

    /**
     * Stops the specified alarm
     *
     * @param alarm alarm to stop
     */
    fun stopAlarm(alarm: Alarm?) {
        if (alarmMap.containsKey(alarm)) {
            alarmMap.get(alarm)!!.cancel(intentMap.get(alarm)!!)
            alarmMap.remove(alarm)
            intentMap.remove(alarm)
        }
    }

    /**
     * Starts all alarms that are supposed to start at device boot
     *
     * @param context
     */
    fun onBoot(context: Context) {
        for (alarm in Alarm.entries) {
            if (alarm.startOnBoot()) {
                startAlarm(alarm, context)
            }
        }
    }

    /*
		An enum to represent an alarm that may be created.
		If you wish to add an alarm, just add it at the top of the enum with the 3 arguments,
		and then call startAlarm() on it.
	 */
    enum class Alarm(
        private val interval: Long,
        alarmClass: Class<out BroadcastReceiver?>,
        startOnBoot: Boolean
    ) {
        MESSAGE_CHECKER(AlarmManager.INTERVAL_HALF_HOUR, NewMessageChecker::class.java, true),
        CACHE_PRUNER(AlarmManager.INTERVAL_HOUR, RegularCachePruner::class.java, true);

        private val alarmClass: Class<out BroadcastReceiver?>?
        private val startOnBoot: Boolean

        init {
            this.alarmClass = alarmClass
            this.startOnBoot = startOnBoot
        }

        private fun interval(): Long {
            return interval
        }

        private fun alarmClass(): Class<out BroadcastReceiver?>? {
            return alarmClass
        }

        private fun startOnBoot(): Boolean {
            return startOnBoot
        }
    }
}
