package com.pranav.java.ide;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.base.Charsets;
import com.pranav.lib_android.exception.CompilationFailedException;
import com.pranav.lib_android.task.JavaBuilder;
import com.pranav.lib_android.task.java.*;
import com.pranav.lib_android.code.disassembler.ClassFileDisassembler;
import com.pranav.lib_android.code.formatter.Formatter;
import com.pranav.lib_android.util.*;
import dalvik.system.PathClassLoader;
import io.github.rosemoe.sora.langs.java.JavaLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

	private CodeEditor editor;

	private long dxTime = 0;
	private long ecjTime = 0;

	private boolean errors = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initialize();
		initializeLogic();
		grantChmod(getFilesDir().getParentFile());
	}

	private void initialize() {
		final JavaBuilder builder = new JavaBuilder(getApplicationContext(),
				getClassLoader());
		final Toolbar toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		getSupportActionBar().setHomeButtonEnabled(false);

		editor = findViewById(R.id.editor);
		final MaterialButton btn_disassemble = findViewById(
				R.id.btn_disassemble);
		final MaterialButton btn_run = findViewById(R.id.btn_run);
		final MaterialButton btn_smali = findViewById(R.id.btn_smali);
		final MaterialButton btn_smali2java = findViewById(R.id.btn_smali2java);

		btn_disassemble.setOnClickListener((v) -> {
			disassemble();
		});
		btn_smali2java.setOnClickListener((v) -> {
			decompile();
		});
		btn_smali.setOnClickListener((v) -> {
			smali();
		});
		btn_run.setOnClickListener((view) -> {
			errors = false;
			final CountDownLatch latch = new CountDownLatch(1);
			Executors.newSingleThreadExecutor().execute(() -> {
				try {
					// code that prepares the files
					FileUtil.deleteFile(FileUtil.getBinDir());
					new File(FileUtil.getBinDir()).mkdirs();
					final File mainFile = new File(
							FileUtil.getJavaDir() + "Main.java");
					Files.createParentDirs(mainFile);
					Files.write(editor.getText().toString()
							.replace("System.exit(",
									"System.err.print(\"Exit code \" + ")
							.getBytes(), mainFile);
				} catch (IOException e) {

				}
				// code that copies android.jar and
				// core-lambda-stubs.jar from
				// assets to temp folder (if not exists)
				if (!new File(FileUtil.getClasspathDir() + "android.jar")
						.exists()) {
					ZipUtil.unzipFromAssets(getApplicationContext(),
							"android.jar.zip", FileUtil.getClasspathDir());
					try (InputStream input = getAssets()
							.open("core-lambda-stubs.jar")) {
						File output = new File(FileUtil.getClasspathDir(),
								"core-lambda-stubs.jar");
						Files.write(ByteStreams.toByteArray(input), output);
					} catch (Exception e) {

					}
				}
				// code that runs ecj
				long time = System.currentTimeMillis();
				try {
					CompileJavaTask javaTask = new CompileJavaTask(builder);
					javaTask.doFullTask();
				} catch (Throwable e) {
					errors = true;
					if (e instanceof CompilationFailedException) {
						showErr(e.getMessage());
					} else {
						showErr(Log.getStackTraceString(e));
					}
					latch.countDown();
					return;
				}
				ecjTime = System.currentTimeMillis() - time;
				// code that packages classes to a JAR
				time = System.currentTimeMillis();
				try {
					new DexTask(builder).doFullTask();
				} catch (Exception e) {
					errors = true;
					showErr(e.toString());
					latch.countDown();
					return;
				}
				dxTime = System.currentTimeMillis() - time;
				latch.countDown();
			});
			try {
				latch.await();
			} catch (Throwable e) {
				dialog("eww", Log.getStackTraceString(e), true);
			}
			// code that loads the final dex
			if (!errors) {
				{
					try {
						final String[] classes = getClassesFromDex();
						if (classes == null)
							return;
						listDialog("Select a class to execute", classes,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int pos) {
										ExecuteJavaTask task = new ExecuteJavaTask(
												builder, classes[pos]);
										try {
											task.doFullTask();
										} catch (java.lang.reflect.InvocationTargetException e) {
											dialog("Failed...",
													"Runtime error: "
															+ e.getCause()
																	.toString(),
													true);
										} catch (Exception e) {
											dialog("Failed..",
													"Couldn't execute the dex: "
															+ e.toString()
															+ "\n\nSystem logs:\n"
															+ task.getLogs(),
													true);
										}
										dialog("Success! Ecj took: "
												+ String.valueOf(ecjTime) + " "
												+ "ms" + ", Dx took: "
												+ String.valueOf(dxTime),
												task.getLogs(), true);
									}
								});
					} catch (Throwable e) {
						final String stack = Log.getStackTraceString(e);
						showErr(stack);
					}
				}
			}
		});
	}

	private void showErr(final String e) {
		Snackbar.make(findViewById(R.id.container), "An error occurred",
				Snackbar.LENGTH_INDEFINITE)
				.setAction("Show error", new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						dialog("Failed..", e, true);
					}
				}).show();
	}

	private void initializeLogic() {
		editor.setTypefaceText(Typeface.MONOSPACE);

		// editor.setOverScrollEnabled(true);

		editor.setEditorLanguage(new JavaLanguage());

		editor.setColorScheme(new SchemeDarcula());

		editor.setTextSize(12);

		final File file = new File(FileUtil.getJavaDir() + "Main.java");

		if (file.exists()) {
			try {
				editor.setText(Files.asCharSource(file, Charsets.UTF_8).read());
			} catch (Exception e) {
				dialog("Cannot read file", Log.getStackTraceString(e), true);
			}
		} else {
			editor.setText("package com.example;\n\nimport java.util.*;\n\n"
					+ "public class Main {\n\n"
					+ "\tpublic static void main(String[] args) {\n"
					+ "\t\tSystem.out.print(\"Hello, World!\");\n" + "\t}\n"
					+ "}\n");
		}
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
				final Intent intent = new Intent();
				intent.setClass(getApplicationContext(), SettingActivity.class);
				startActivity(intent);
				break;
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
						final String claz = classes[0];
						final CountDownLatch latch = new CountDownLatch(1);
						Executors.newSingleThreadExecutor()
								.execute(() -> {
									String[] str = new String[]{"-f",
											"-o",
											FileUtil.getBinDir()
													.concat("smali/"),
											FileUtil.getBinDir()
													.concat("classes.dex")};
									com.googlecode.d2j.smali.BaksmaliCmd
											.main(str);
									latch.countDown();
						});

						try {
							latch.await();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						CodeEditor edi = new CodeEditor(MainActivity.this);

						edi.setTypefaceText(Typeface.MONOSPACE);

//						edi.setOverScrollEnabled(true);

						edi.setEditorLanguage(new JavaLanguage());

						edi.setColorScheme(new SchemeDarcula());

						edi.setTextSize(13);
							
						File smaliFile = new File(FileUtil.getBinDir() + "smali/" + claz.replace(".", "/") + ".smali");
							
						try {
						    edi.setText(formatSmali(Files.asCharSource(smaliFile, Charsets.UTF_8).read()));
						} catch (IOException e) {
							dialog("Cannot read file", Log.getStackTraceString(e), true);
						}

						final AlertDialog dialog = new AlertDialog.Builder(
								MainActivity.this).setView(edi).create();
						dialog.setCanceledOnTouchOutside(true);
						dialog.show();
			});
		} catch (Throwable e) {
			dialog("Failed to extract smali source", Log.getStackTraceString(e),
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
					final CountDownLatch latch = new CountDownLatch(1);
					Executors.newSingleThreadExecutor().execute(() -> {

						String[] args = {
								// "-jar",
								FileUtil.getBinDir() + "classes/" + claz
										+ ".class",
								"--extraclasspath",
								FileUtil.getClasspathDir() + "android.jar",
								"--outputdir", FileUtil.getBinDir() + "cfr/"};

						try {
							org.benf.cfr.reader.Main.main(args);
						} catch (final Exception e) {
							dialog("Failed to decompile...",
									Log.getStackTraceString(e), true);
						}
						latch.countDown();
					});

					try {
						latch.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
						dialog("Thread was interrupted while decompiling...",
								e.getMessage(), true);
					}

					final CodeEditor edi = new CodeEditor(MainActivity.this);

					edi.setTypefaceText(Typeface.MONOSPACE);

					// edi.setOverScrollEnabled(true);

					edi.setEditorLanguage(new JavaLanguage());

					edi.setColorScheme(new SchemeDarcula());

					edi.setTextSize(12);

					File decompiledClass = new File(
							FileUtil.getBinDir() + "cfr/" + claz + ".java");

					try {
						edi.setText(Files
								.asCharSource(decompiledClass, Charsets.UTF_8)
								.read());
					} catch (IOException e) {
						dialog("Cannot read file", Log.getStackTraceString(e),
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

			// edi.setOverScrollEnabled(true);

			edi.setEditorLanguage(new JavaLanguage());

			edi.setColorScheme(new SchemeDarcula());

			edi.setTextSize(12);

			try {
				final String disassembly = new ClassFileDisassembler(
						FileUtil.getBinDir() + "classes/" + claz + ".class")
								.disassemble();

				edi.setText(disassembly);

				final AlertDialog d = new AlertDialog.Builder(MainActivity.this)
						.setView(edi).create();
				d.setCanceledOnTouchOutside(true);
				d.show();
			} catch (Throwable e) {
				dialog("Failed to disassemble", Log.getStackTraceString(e),
						true);
			}
		});
	}

	private String formatSmali(String in) {

		ArrayList<String> lines = new ArrayList<>(
				Arrays.asList(in.split("\n")));

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
		final MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(
				MainActivity.this).setTitle(title).setItems(items, listener);
		dialog.create().show();
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

	public void grantChmod(File file) {
		try {
			if (file.isDirectory()) {
				Runtime.getRuntime()
						.exec("chmod 777 " + file.getAbsolutePath());
				for (File f : file.listFiles()) {
					grantChmod(f);
				}
			} else {
				Runtime.getRuntime()
						.exec("chmod 777 " + file.getAbsolutePath());
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public String[] getClassesFromDex() {
		try {
			final ArrayList<String> classes = new ArrayList<>();
			org.jf.dexlib2.iface.DexFile dexfile = org.jf.dexlib2.DexFileFactory
					.loadDexFile(FileUtil.getBinDir().concat("classes.dex"),
							org.jf.dexlib2.Opcodes.forApi(21));
			for (org.jf.dexlib2.iface.ClassDef f : dexfile.getClasses()
					.toArray(new org.jf.dexlib2.iface.ClassDef[0])) {
				final String name = f.getType().replace("/", ".");
				classes.add(name.substring(1, name.length() - 1));
			}
			return classes.toArray(new String[0]);
		} catch (Exception e) {
			dialog("Failed to get available classes in dex...",
					Log.getStackTraceString(e), true);
			return null;
		}
	}
}
