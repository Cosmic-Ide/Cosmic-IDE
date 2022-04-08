package com.pranav.java.ide;

import androidx.appcompat.app.AppCompatActivity;

import io.github.rosemoe.sora.widget.CodeEditor;

import java.io.Console;

public class ExecuteActivity extends AppCompatActivity {

    private CodeEditor terminal;
    private String logs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      
      terminal = findViewById(R.id.terminal);
      setContentView(R.layout.activity_execute);
      
      Console console = System.console();
      logs = "";
    }
    
    public void write(String text) {
      logs.concat(text);
      terminal.append(text);
    }
}