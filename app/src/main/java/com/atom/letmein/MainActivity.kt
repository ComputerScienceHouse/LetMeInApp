package com.atom.letmein

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun showDialogStatus() {
        val builder = AlertDialog.Builder(this)
        // Get the layout inflator
        val inflator = layoutInflater


        builder.setView(inflator.inflate(R.layout.dialog_req_status, null))
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
        val dialog = builder.create()
        dialog.show()
    }

    /** button events **/
    fun reqLWell(view: View) {
        showDialogStatus()
    }

    fun reqFirstElevator(view: View) {
        showDialogStatus()
    }

    fun reqAElevator(view: View) {
        showDialogStatus()
    }

    fun reqNorthStairwell(view: View) {
        showDialogStatus()
    }

    fun reqSouthStairwell(view: View) {
        showDialogStatus()
    }
}