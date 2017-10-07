package com.tommasoberlose.anotherwidget.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.appwidget.AppWidgetManager
import android.content.*
import android.preference.PreferenceManager
import android.provider.CalendarContract
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.util.Util
import com.tommasoberlose.anotherwidget.ui.widget.TheWidget
import com.tommasoberlose.anotherwidget.util.UpdatesReceiver
import com.tommasoberlose.anotherwidget.util.WeatherReceiver
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.the_widget.*
import java.util.*
import java.util.concurrent.TimeUnit
import android.content.Intent
import android.content.BroadcastReceiver




class MainActivity : AppCompatActivity() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateUI()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        receiver

        // TODO Util.showIntro(this)

        action_support.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                Util.openURI(this@MainActivity, "https://paypal.me/tommasoberlose")
            }
        })

        action_share.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                Util.share(this@MainActivity)
            }
        })

        action_project.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                Util.openURI(this@MainActivity, "https://github.com/tommasoberlose/another-widget")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        registerReceiver(receiver, filter);
        updateUI()
    }

    override fun onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                   grantResults: IntArray) {
        when (requestCode) {
            Constants.CALENDAR_REQUEST_CODE -> if (permissions.size != 1 || grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                UpdatesReceiver().removeUpdates(this)
            } else {
                UpdatesReceiver().setUpdates(this)
            }
            Constants.LOCATION_REQUEST_CODE -> if (permissions.size != 1 || grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                WeatherReceiver().removeUpdates(this)
            } else {
                WeatherReceiver().setUpdates(this)
            }
        }
    }

    fun updateUI() {
        no_calendar_permission_container.visibility= View.GONE
        no_location_permission_container.visibility= View.GONE
        all_set_container.visibility = View.GONE
        updateSettings()

        if (!Util.checkGrantedPermission(this, Manifest.permission.READ_CALENDAR)) {
            no_calendar_permission_container.visibility = View.VISIBLE
            request_calendar.setOnClickListener(object: View.OnClickListener {
                override fun onClick(view: View?) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_CALENDAR), Constants.CALENDAR_REQUEST_CODE)
                }
            })
        } else {
            if (!Util.checkGrantedPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                no_location_permission_container.visibility = View.VISIBLE
                request_location.setOnClickListener(object: View.OnClickListener {
                    override fun onClick(view: View?) {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), Constants.LOCATION_REQUEST_CODE)
                    }
                })
            } else {
                all_set_container.visibility = View.VISIBLE
            }
        }
        updateAppWidget()
        Util.updateWidget(this)
    }


    internal fun updateAppWidget() {
        widget_bg.setImageDrawable(Util.getCurrentWallpaper(this))
        updateCalendarView()
        updateLocationView()
    }

    fun updateCalendarView() {
        val now = Calendar.getInstance()
        val calendarLayout = Util.checkGrantedPermission(this, Manifest.permission.READ_CALENDAR)

        empty_layout.visibility = View.VISIBLE
        calendar_layout.visibility = View.GONE
        empty_date.text = String.format("%s%s", Constants.dateFormat.format(now.time)[0].toUpperCase(), Constants.dateFormat.format(now.time).substring(1))

        if (calendarLayout) {
            val eventList = Util.getNextEvent(this)

            if (eventList.isNotEmpty()) {
                val difference = eventList[0].startDate - now.timeInMillis

                if (difference > 1000 * 60) {
                    var time = ""
                    val hour = TimeUnit.MILLISECONDS.toHours(difference)
                    if (hour > 0) {
                        time = hour.toString() + getString(R.string.h_code)
                    }
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(difference - hour * 3600 * 1000)
                    if (minutes > 0) {
                        time += " " + minutes + getString(R.string.min_code)
                    }

                    next_event.text = String.format("%s %s %s", eventList[0].title, getString(R.string.in_code), time)
                } else {
                    next_event.text = String.format("%s", eventList[0].title)
                }
                next_event_date.text = String.format("%s - %s", Constants.hourFormat.format(eventList[0].startDate), Constants.hourFormat.format(eventList[0].endDate))

                empty_layout.visibility = View.GONE
                calendar_layout.visibility = View.VISIBLE
            }
        }
    }

    fun updateLocationView() {
        val locationLayout = Util.checkGrantedPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        if (locationLayout && SP.contains(Constants.PREF_WEATHER_TEMP) && SP.contains(Constants.PREF_WEATHER_ICON)) {
            weather.visibility = View.VISIBLE
            calendar_weather.visibility = View.VISIBLE
            val currentTemp = String.format(Locale.getDefault(), "%.0f °%s", SP.getFloat(Constants.PREF_WEATHER_TEMP, 0f), SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F"))


            weather_icon.visibility = View.VISIBLE
            empty_weather_icon.visibility = View.VISIBLE
            val icon: String = SP.getString(Constants.PREF_WEATHER_ICON, "")
            if (icon.equals("")) {
                weather_icon.visibility = View.GONE
                empty_weather_icon.visibility = View.GONE
            } else {
                weather_icon.setImageResource(Util.getWeatherIconResource(icon))
                empty_weather_icon.setImageResource(Util.getWeatherIconResource(icon))
            }

            temp.text = currentTemp
            calendar_temp.text = currentTemp
        } else {
            weather.visibility = View.GONE
            calendar_weather.visibility = View.GONE
        }
    }

    fun updateSettings() {
        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        temp_unit.text = if (SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F").equals("F")) getString(R.string.fahrenheit) else getString(R.string.celsius)
        action_change_unit.setOnClickListener(object: View.OnClickListener {
            @SuppressLint("ApplySharedPref")
            override fun onClick(p0: View?) {
                SP.edit().putString(Constants.PREF_WEATHER_TEMP_UNIT, if (SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F").equals("F")) "C" else "F").commit()
                Util.getWeather(this@MainActivity)
                updateSettings()
            }
        })
    }

}