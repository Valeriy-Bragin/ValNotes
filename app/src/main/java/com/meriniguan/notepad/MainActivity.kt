package com.meriniguan.notepad

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import com.google.android.material.navigation.NavigationView
import com.meriniguan.notepad.databinding.ActivityMainBinding
import com.meriniguan.notepad.utils.DrawerItemClickListener
import com.meriniguan.notepad.utils.OnNavigateBackObservable
import dagger.hilt.android.AndroidEntryPoint
import java.util.regex.Pattern


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController

    private val topLevelDestinations = setOf(
        getNotesDestination(), getArchiveDestination(), getTrashDestination()
    )

    private var currentDestination: NavDestination? = null

    private val destinationListener = NavController.OnDestinationChangedListener { _, destination, arguments ->
        currentDestination = destination
    }

    private var currentFragment: Fragment? = null

    private val fragmentCallbacks: FragmentLifecycleCallbacks = object : FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(
            fm: FragmentManager,
            f: Fragment,
            v: View,
            savedInstanceState: Bundle?
        ) {
            super.onFragmentViewCreated(fm, f, v, savedInstanceState)
            currentFragment = f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
        createReminderNotificationChannel()

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_main)
        navController.addOnDestinationChangedListener(destinationListener)

        appBarConfiguration = AppBarConfiguration(
            topLevelDestinations, drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
            val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
            if (currentFragment is DrawerItemClickListener) {
                when (it.itemId) {
                    R.id.notesFragment -> {
                        currentFragment.onNormalNotesSelected()
                    }
                    R.id.archiveFragment -> {
                        currentFragment.onArchivedNotesSelected()
                    }
                    R.id.trashFragment -> {
                        currentFragment.onTrashedNotesSelected()
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, false)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else if (currentDestination.isTopLevelDestination()) {
            finish()
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        navController.removeOnDestinationChangedListener(destinationListener)
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
    }

    private fun NavDestination?.isTopLevelDestination(): Boolean {
        return if (this != null) topLevelDestinations.contains(this.id) else false
    }

    private fun notifyOnBackPressed() {
        val fragment = currentFragment
        if (fragment is OnNavigateBackObservable) {
            fragment.onBackPressed()
        }
    }

    private fun prepareTitle(label: CharSequence?, arguments: Bundle?): String {

        // code for this method has been copied from Google sources :)

        if (label == null) return ""
        val title = StringBuffer()
        val fillInPattern = Pattern.compile("\\{(.+?)\\}")
        val matcher = fillInPattern.matcher(label)
        while (matcher.find()) {
            val argName = matcher.group(1)
            if (arguments != null && arguments.containsKey(argName)) {
                matcher.appendReplacement(title, "")
                title.append(arguments[argName].toString())
            } else {
                throw IllegalArgumentException(
                    "Could not find $argName in $arguments to fill label $label"
                )
            }
        }
        matcher.appendTail(title)
        return title.toString()
    }

    private fun createReminderNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "reminderChannelName"
            val description = "channel for reminder alarm manager"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("reminderChannel", name, importance)
            channel.description = description
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotesDestination(): Int = R.id.notesFragment

    private fun getArchiveDestination(): Int = R.id.archiveFragment

    private fun getTrashDestination(): Int = R.id.trashFragment
}

const val NOTE_UPDATED_RESULT_OK = Activity.RESULT_FIRST_USER
const val EMPTY_NOTE_DISCARDED_RESULT_OK = Activity.RESULT_FIRST_USER + 1
