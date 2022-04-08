package com.pranav.java.ide

import androidx.appcompat.app.AppCompatActivity

import io.github.rosemoe.sora.widget.CodeEditor

import java.io.Console

class ExecuteActivity: AppCompatActivity() {

    private val terminal: CodeEditor
    private var logs: String

    override fun onCreate(savedInstanceState: Bundle) {
      super.onCreate(savedInstanceState)
      
      terminal = findViewById(R.id.terminal)
      setContentView(R.layout.activity_execute)
      
      val console = System.console()
      logs = ""
    }
    
    public fun write(text: String) {
      logs.concat(text)
      terminal.append(text)
    }
}