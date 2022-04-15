package com.pranav.java.ide;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.base.Charsets;
import com.googlecode.d2j.smali.BaksmaliCmd;
import com.pranav.lib_android.exception.CompilationFailedException;
import com.pranav.lib_android.task.JavaBuilder;
import com.pranav.lib_android.task.java.CompileJavaTask;
import com.pranav.lib_android.task.java.D8Task;
import com.pranav.lib_android.task.java.DexTask;
import com.pranav.lib_android.task.java.ExecuteJavaTask;
import com.pranav.lib_android.code.disassembler.ClassFileDisassembler;
import com.pranav.lib_android.code.formatter.Formatter;
import com.pranav.lib_android.util.ZipUtil;
import com.pranav.lib_android.util.FileUtil;
import com.pranav.lib_android.util.ConcurrentUtil;

import io.github.rosemoe.sora.langs.java.JavaLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;

public final class MainActivity extends AppCompatActivity {

	private CodeEditor editor;

	private long d8Time = 0;
	private long ecjTime = 0;

	private boolean errorsArePresent = false;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setSupportActionBar(findViewById(R.id.toolbar));
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		getSupportActionBar().setHomeButtonEnabled(false);

		editor = findViewById(R.id.editor);

		editor.setTypefaceText(Typeface.MONOSPACE);
		editor.setEditorLanguage(new JavaLanguage());
		editor.setColorScheme(new SchemeDarcula());
		editor.setTextSize(12);

		final File file = file(FileUtil.getJavaDir() + "Main.java");

		if (file.exists()) {
			try {
				editor.setText(
				    Files.asCharSource(file, Charsets.UTF_8)
				        .read()
				    );
			} catch (Exception e) {
				dialog("Cannot read file", getString(e), true);
			}
		} else {
			editor.setText(
              "package com.example;\n\nimport java.util.*;\n\n" +
              "public class Main {\n\n" +
              "\tpublic static void main(String[] args) {\n" +
              "\t\tSystem.out.print(\"Hello, World!\");\n" + "\t}\n" +
              "}\n"
					);
		}
		
		final JavaBuilder builder = new JavaBuilder(getApplicationContext(),
				getClassLoader());

		ConcurrentUtil.executeInBackground(() -> {
			if (!file(FileUtil.getClasspathDir() + "android.jar").exists()) {
				ZipUtil.unzipFromAssets(MainActivity.this,
					"android.jar.zip", FileUtil.getClasspathDir());
			}
			File output = file(FileUtil.getClasspathDir() + "/core-lambda-stubs.jar");
			if (!output.exists() && 
					 getSharedPreferences("compiler_settings", Context.MODE_PRIVATE)
				    .getString("javaVersion", "7.0")
			        .equals("8.0")) {
		    try {
					Files.write(
					  ByteStreams.toByteArray(
					    getAssets().open("core-lambda-stubs.jar")
					   ),
					 output);
				} catch (Exception e) {
          showErr(getString(e));
        }
		 }
		});

		findViewById(R.id.btn_disassemble).setOnClickListener(v -> disassemble());
		findViewById(R.id.btn_smali2java).setOnClickListener(v -> decompile());
		findViewById(R.id.btn_smali).setOnClickListener(v -> smali());
		findViewById(R.id.btn_run).setOnClickListener(view -> {
			try {
				// Delete previous build files
				FileUtil.deleteFile(FileUtil.getBinDir());
				file(FileUtil.getBinDir()).mkdirs();
				final File mainFile = file(
						FileUtil.getJavaDir() + "Main.java");
				Files.createParentDirs(mainFile);
				// a simple workaround to prevent calls to system.exit
				Files.write(editor.getText().toString()
						.replace("System.exit(",
								"System.err.print(\"Exit code \" + ")
						.getBytes(), mainFile);
			} catch (final IOException e) {
        dialog("Cannot save program", getString(e), true);
			}

			// code that runs ecj
			long time = System.currentTimeMillis();
			errorsArePresent = true;
			try {
				CompileJavaTask javaTask = new CompileJavaTask(builder);
				javaTask.doFullTask();
				errorsArePresent = false;
			} catch (CompilationFailedException e) {
				showErr(e.getMessage());
			} catch (Throwable e) {
				showErr(getString(e));
			}
			if (errorsArePresent) {
			  return;
			}

			ecjTime = System.currentTimeMillis() - time;
			time = System.currentTimeMillis();
			
			// run d8
			try {
        new D8Task().doFullTask();
			} catch (Exception e) {
				errorsArePresent = true;
				showErr(e.toString());
				return;
			}
			d8Time = System.currentTimeMillis() - time;
			// code that loads the final dex
			try {
				final String[] classes = getClassesFromDex();
				if (classes == null) {
					return;
				}
				listDialog("Select a class to execute", classes,
						(dialog, pos) -> {
							ExecuteJavaTask task = new ExecuteJavaTask(
							  	builder, classes[pos]);
					  	try {
								task.doFullTask();
							} catch (java.lang.reflect.InvocationTargetException e) {
								dialog("Failed...",
										"Runtime error: " +
										    e.getMessage() +
										    "\n\n" +
												getString(e),
									true);
							} catch (Exception e) {
								dialog("Failed..",
										"Couldn't execute the dex: "
												+ e.toString()
												+ "\n\nSystem logs:\n"
												+ task.getLogs(),
										true);
							}
							StringBuilder s = new StringBuilder();
							s.append("Success! ECJ took: ");
							s.append(String.valueOf(ecjTime));
							s.append("ms, ");
							s.append("D8");
							s.append(" took: ");
							s.append(String.valueOf(d8Time));
							s.append("ms");
							dialog(s.toString(), task.getLogs(), true);
					});
			} catch (Throwable e) {
				showErr(getString(e));
			}
		});
	}

	public void showErr(final String e) {
		Snackbar.make((LinearLayout) findViewById(R.id.container), "An error occurred",
				Snackbar.LENGTH_INDEFINITE)
				.setAction("Show error", (view) -> dialog("Failed...", e, true))
			    .show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Format");
		menu.add(0, 1, 0, "Settings");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 0 :
				Formatter formatter = new Formatter(
						editor.getText().toString());
				editor.setText(formatter.format());
				break;

			case 1 :
				Intent intent = getIntent();
				intent.setClass(this, SettingActivity.class);
				startActivity(intent);
			default :
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void smali() {
		try {
			final String[] classes = getClassesFromDex();
			if (classes == null)
				return;
			listDialog("Select a class to extract source", classes,
					(d, pos) -> {
						final String claz = classes[pos];
						final String[] args = new String[] {
						  "-f",
						  "-o",
						  FileUtil.getBinDir()
						      .concat("smali/"),
						  FileUtil.getBinDir()
						      .concat("classes.dex")
						};
						ConcurrentUtil.execute(() -> BaksmaliCmd.main(args));

						CodeEditor edi = new CodeEditor(MainActivity.this);
						edi.setTypefaceText(Typeface.MONOSPACE);
						edi.setEditorLanguage(new JavaLanguage());
						edi.setColorScheme(new SchemeDarcula());
						edi.setTextSize(13);
							
						File smaliFile = file(FileUtil.getBinDir() + "smali/" + claz.replace(".", "/") + ".smali");
							
						try {
						    edi.setText(formatSmali(Files.asCharSource(smaliFile, Charsets.UTF_8).read()));
						} catch (IOException e) {
							dialog("Cannot read file", getString(e), true);
						}

						final AlertDialog dialog = new AlertDialog.Builder(
								MainActivity.this).setView(edi).create();
						dialog.setCanceledOnTouchOutside(true);
						dialog.show();
			});
		} catch (Throwable e) {
			dialog("Failed to extract smali source", getString(e),
					true);
		}
	}

	public void decompile() {
		final String[] classes = getClassesFromDex();
		if (classes == null)
			return;
		listDialog("Select a class to extract source", classes,
				(dialog, pos) -> {
					final String claz = classes[pos].replace(".", "/");
          String[] args = {
							FileUtil.getBinDir() +
                  "classes/" +
                  claz + // full class name
                  ".class",
							"--extraclasspath",
							FileUtil.getClasspathDir() + "android.jar",
							"--outputdir",
							FileUtil.getBinDir() + "cfr/"
						};

					ConcurrentUtil.execute(() -> {
						try {
							org.benf.cfr.reader.Main.main(args);
						} catch (Exception e) {
							dialog("Failed to decompile...",
									getString(e), true);
						}
					});

					final CodeEditor edi = new CodeEditor(MainActivity.this);
					edi.setTypefaceText(Typeface.MONOSPACE);
					edi.setEditorLanguage(new JavaLanguage());
					edi.setColorScheme(new SchemeDarcula());
					edi.setTextSize(12);

					File decompiledFile = file(
							FileUtil.getBinDir() + "cfr/" + claz + ".java");

					try {
						edi.setText(Files
								.asCharSource(decompiledFile, Charsets.UTF_8)
								.read());
					} catch (IOException e) {
						dialog("Cannot read file", getString(e),
								true);
					}

					final AlertDialog d = new AlertDialog.Builder(
							MainActivity.this).setView(edi).create();
					d.setCanceledOnTouchOutside(true);
					d.show();
		});
	}

	public void disassemble() {
		final String[] classes = getClassesFromDex();
		if (classes == null)
			return;
		listDialog("Select a class to disassemble", classes, (dialog, pos) -> {
			final String claz = classes[pos].replace(".", "/");

			final CodeEditor edi = new CodeEditor(MainActivity.this);
			edi.setTypefaceText(Typeface.MONOSPACE);
			edi.setEditorLanguage(new JavaLanguage());
			edi.setColorScheme(new SchemeDarcula());
			edi.setTextSize(12);

			try {
				final String disassembled = new ClassFileDisassembler(
						FileUtil.getBinDir() + "classes/" + claz + ".class")
								.disassemble();

				edi.setText(disassembled);

				AlertDialog d = new AlertDialog.Builder(MainActivity.this)
						.setView(edi).create();
				d.setCanceledOnTouchOutside(true);
				d.show();
			} catch (Throwable e) {
				dialog("Failed to disassemble", getString(e),
						true);
			}
		});
	}

	private String formatSmali(String in) {

		ArrayList<String> lines = new ArrayList<>(
				Arrays.asList(in.split("\n"))
		);

		boolean insideMethod = false;

		for (int i = 0; i < lines.size(); i++) {

			String line = lines.get(i);

			if (line.startsWith(".method"))
				insideMethod = true;

			if (line.startsWith(".end method"))
				insideMethod = false;

			if (insideMethod && !shouldSkip(line))
				lines.set(i, line + "\n");
		}

		StringBuilder result = new StringBuilder();

		for (int i = 0; i < lines.size(); i++) {
			if (i != 0)
				result.append("\n");

			result.append(lines.get(i));
		}

		return result.toString();
	}

	private boolean shouldSkip(String smaliLine) {

		String[] ops = {".line", ":", ".prologue"};

		for (String op : ops) {
			if (smaliLine.trim().startsWith(op))
				return true;
		}
		return false;
	}

	public void listDialog(String title, String[] items,
			DialogInterface.OnClickListener listener) {
		new MaterialAlertDialogBuilder(MainActivity.this)
	  	  .setTitle(title)
		    .setItems(items, listener)
	      .create()
	      .show();
	}

	public void dialog(String title, final String message, boolean copyButton) {
		final MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(
				MainActivity.this).setTitle(title).setMessage(message)
						.setPositiveButton("GOT IT", null)
						.setNegativeButton("CANCEL", null);
		if (copyButton)
			dialog.setNeutralButton("COPY", (dialogInterface, i) -> {
				((ClipboardManager) getSystemService(
						getApplicationContext().CLIPBOARD_SERVICE))
								.setPrimaryClip(ClipData
										.newPlainText("clipboard", message));
			});
		dialog.create().show();
	}

// for granting execute permissions to file/folder (currently unused)
  public void grantChmod(File file) {
	  File[] files = file.listFiles();
		try {
		  for (File f : files) {
		    if (f.isDirectory()) {
          grantChmod(f);
        } else {
          f.setExecutable(true, true);
        }
		  }
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public String[] getClassesFromDex() {
		try {
			final ArrayList<String> classes = new ArrayList<>();
			DexFile dexfile = DexFileFactory.loadDexFile(FileUtil.getBinDir().concat("classes.dex"),
							Opcodes.forApi(26)
			);
			for (ClassDef f : dexfile.getClasses()
					.toArray(new ClassDef[0])
					) {
				String name = f.getType().replace("/", "."); // convert class name to standard form
				classes.add(name.substring(1, name.length() - 1));
			}
			return classes.toArray(new String[0]);
		} catch (Exception e) {
			dialog("Failed to get available classes in dex...",
					getString(e), true);
			return null;
		}
	}
	
	public File file(final String path) {
	  return new File(path);
	}
	
	private String getString(final Throwable e) {
	  return Log.getStackTraceString(e);
	}
}
