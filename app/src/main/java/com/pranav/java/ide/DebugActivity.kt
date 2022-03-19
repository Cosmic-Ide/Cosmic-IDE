package com.pranav.java.ide

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log

class DebugActivity: AppCompatActivity() {

    val exceptionTypes: String[] = {
        "StringIndexOutOfBoundsException",
        "IndexOutOfBoundsException",
        "ArithmeticException",
        "NumberFormatException",
        "ActivityNotFoundException"
    }

    val exceptionMessages: String[] = {
        "Invalid string operation\n",
        "Invalid list operation\n",
        "Invalid arithmetical operation\n",
        "Invalid toNumber block operation\n",
        "Invalid intent operation"
    }


    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)

        val intent = getIntent()
        var errorMessage = ""
        var madeErrorMessage = ""

        if (intent != null) {
            errorMessage = intent.getStringExtra("error")

            val split: String[] = errorMessage.split("\n")
            try {
                for (int j = 0 j < exceptionTypes.length j++) {
                    if (split[0].contains(exceptionTypes[j])) {
                        madeErrorMessage = exceptionMessages[j]

                        int addIndex = split[0].indexOf(exceptionTypes[j]) + exceptionTypes[j].length()

                        madeErrorMessage += split[0].substring(addIndex, split[0].length())
                        madeErrorMessage += "\n\nDetailed error message:\n" + errorMessage
                        break
                    }
                }

                if (madeErrorMessage.isEmpty()) {
                    madeErrorMessage = errorMessage
                }
            } catch (e: Exception) {
                madeErrorMessage = madeErrorMessage + "\n\nError while getting error: " + Log.getStackTraceString(e)
            }
        }

        AlertDialog.Builder(this)
               .setTitle("An error occurred")
               .setMessage(madeErrorMessage)
               .setPositiveButton("End Application", (dialog, which) -> finish())
               .create()
               .show()
    }
}