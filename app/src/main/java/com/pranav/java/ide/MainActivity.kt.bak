package com.pranav.java.ide

import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import com.google.common.base.Charsets
import com.pranav.ide.build.exception.CompilationFailedException
import com.pranav.ide.build.task.JavaBuilder
import com.pranav.ide.build.task.java.*
import com.pranav.ide.code.disassembler.ClassFileDisassembler
import com.pranav.ide.code.formatter.Formatter
import com.pranav.ide.util.ZipUtil
import dalvik.system.PathClassLoader
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.Enumeration

class MainActivity: AppCompatActivity() {

	lateinit val CodeEditor editor

	var dxTime: long = 0
	var ecjTime: long = 0

	var errors: Boolean = false

	override protected fun onCreate(savedInstanceState: Bundle) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.main)
		initialize()
		initializeLogic()
		grantChmod(getFilesDir().getParentFile())
	}

	private fun initialize() {
		val builder = JavaBuilder(getApplicationContext(),
				getClassLoader())
		val Toolbar toolbar = findViewById(R.id._toolbar)
		setSupportActionBar(toolbar)
		getSupportActionBar().setDisplayHomeAsUpEnabled(false)
		getSupportActionBar().setHomeButtonEnabled(false)

		editor = findViewById(R.id.editor)
		val btn_disassemble = findViewById(R.id.btn_disassemble)
		val btn_run = findViewById(R.id.btn_run)
		val btn_smali = findViewById(R.id.btn_smali)
		val btn_smali2java = findViewById(R.id.btn_smali2java)

		btn_disassemble.setOnClickListener((v) -> {
			disassemble()
		})
		btn_smali2java.setOnClickListener((v) -> {
			decompile()
		})
		btn_smali.setOnClickListener((v) -> {
			smali()
		})
		btn_run.setOnClickListener((v) -> {
			errors = false
			val latch = CountDownLatch(1)
			Executors.newSingleThreadExecutor().execute(() -> {
				try {
					// code that prepares the files
					FileUtil.deleteFile(FileUtil
							.getBinDir())
					File(FileUtil
							.getBinDir()).mkdirs()
					val mainFile = File(FileUtil.getJavaDir() + "Main.java")
					Files.createParentDirs(mainFile)
					Files.write(editor.getText().toString().replace("System.exit(", "System.err.print(\"Exit code \" + ").getBytes(), mainFile)
				} catch (e: IOException) {
							
				}
				// code that copies android.jar and
				// core-lambda-stubs.jar from
				// assets to temp folder (if not exists)
				if (!File(FileUtil
						.getClasspathDir()
							+ "android.jar").exists()) {
					ZipUtil.unzipFromAssets(getApplicationContext(),
							"android.jar.zip",
							FileUtil.getClasspathDir())
					try (input: InputStream = getAssets()
							.open("core-lambda-stubs.jar")) {
						val output = File(FileUtil.getClasspathDir(), "core-lambda-stubs.jar")
						Files.write(ByteStreams.toByteArray(input), output)
					} catch (e: Exception) {

					}
				}
				// code that runs ecj
				var time = System.currentTimeMillis()
				try {
					val javaTask = CompileJavaTask(
							builder)
					javaTask.doFullTask()
				} catch (e: Throwable) {
					errors = true
					if (e instanceof CompilationFailedException) {
						showErr(e.getMessage())
					} else {
						showErr(Log.getStackTraceString(e))
					}
					latch.countDown()
					return
				}
				ecjTime = System.currentTimeMillis() - time
				// code that packages classes to a JAR
				time = System.currentTimeMillis()
				try {
					DexTask(builder).doFullTask()
				} catch (Exception e) {
					errors = true
					showErr(e.toString())
					latch.countDown()
					return
				}
				dxTime = System.currentTimeMillis() - time
				latch.countDown()
			}
		})
		try {
			latch.await()
		} catch (e: Throwable) {
			dialog("eww", Log.getStackTraceString(e), true)
		}
		// code that loads the final dex
		if (!errors) {
			{
				try {
					val classes: String[] = getClassesFromDex()
					if (classes == null)
						return
					listDialog("Select a class to execute", classes,
							(dialog, pos) -> {
									val task = ExecuteJavaTask(
											builder, classes[pos]
									)
									try {
										task.doFullTask()
									} catch (java.lang.reflect.InvocationTargetException e) {
										dialog("Failed...",
												"Runtime error: "
														+ e.getCause()
																.toString(),
												true)
									} catch (Exception e) {
										dialog("Failed..",
												"Couldn't execute the dex: "
														+ e.toString()
														+ "\n\nSystem logs:\n"
														+ task.getLogs(),
												true)
									}
									dialog("Success! Ecj took: "
											+ String.valueOf(ecjTime)
											+ " " + "ms" + ", Dx took: "
											+ String.valueOf(dxTime),
											task.getLogs(), true)
								}
							})
				} catch (e: Throwable) {
					showErr(Log.getStackTraceString(e))
				}
			}
		}
	}

	private fun showErr(e: String) {
		Snackbar.make(findViewById(R.id.container), "An error occurred",
				Snackbar.LENGTH_INDEFINITE)
				.setAction("Show error", () -> {
					dialog("Failed..", e, true)
				})
					.show()
	}

	private fun initializeLogic() {
		editor.setTypefaceText(Typeface.MONOSPACE)

//		editor.setOverScrollEnabled(true)

		editor.setEditorLanguage(JavaLanguage())

		editor.setColorScheme(SchemeDarcula())

		editor.setTextSize(12)
		
		val file = File(FileUtil.getJavaDir() + "Main.java")
		
		if (file.exists()) {
			try {
				editor.setText(Files.asCharSource(file, Charsets.UTF_8).read())
			} catch (e: Exception) {
				dialog("Cannot read file", Log.getStackTraceString(e), true)
			}
		} else {
			editor.setText("package com.example\n\nimport java.util.*\n\n"
					+ "public class Main {\n\n"
					+ "\tpublic static fun main(String[] args) {\n"
					+ "\t\tSystem.out.print(\"Hello, World!\")\n" + "\t}\n"
					+ "}\n")
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menu.add(0, 0, 0, "Format")
		menu.add(0, 1, 0, "Settings")
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		switch (item.getItemId()) {
			case 0 :
				val formatter = Formatter(
						editor.getText().toString())
				editor.setText(formatter.format())
				break

			case 1 :
				val intent = Intent()
				intent.setClass(getApplicationContext(), SettingActivity.class)
				startActivity(intent)
				break
			default :
				break
		}
		return super.onOptionsItemSelected(item)
	}

	fun smali() {
		try {
			val classes: String[] = getClassesFromDex()
			if (classes == null)
				return
			listDialog("Select a class to extract source", classes,
					(d, pos) -> {
					val claz = classes[0]
					val latch = CountDownLatch(1)
					Executors.newSingleThreadExecutor()
							.execute(() -> {
							String[] str = String[]{"-f",
									"-o",
									FileUtil.getBinDir()
											.concat("smali/"),
									FileUtil.getBinDir()
											.concat("classes.dex")
							}
							com.googlecode.d2j.smali.BaksmaliCmd
								.main(str)
								latch.countDown()
					})

					try {
						latch.await()
					} catch (e: InterruptedException) {
						e.printStackTrace()
					}
					val edi = CodeEditor(MainActivity.this)

					edi.setTypefaceText(Typeface.MONOSPACE)

//					edi.setOverScrollEnabled(true)

					edi.setEditorLanguage(JavaLanguage())

					edi.setColorScheme(SchemeDarcula())

					edi.setTextSize(13)
							
					val smaliFile = File(FileUtil.getBinDir() + "smali/" + claz.replace(".", "/") + ".smali")
							
					try {
					    edi.setText(formatSmali(Files.asCharSource(smaliFile, Charsets.UTF_8).read()))
					} catch (e: IOException) {
						dialog("Cannot read file", Log.getStackTraceString(e), true)
					}

					val dialog = AlertDialog.Builder(
							MainActivity.this).setView(edi).create()
					dialog.setCanceledOnTouchOutside(true)
					dialog.show()
				}

				private fun formatSmali(in: String): String {

					var lines: ArrayList<String> = ArrayList<>(
							Arrays.asList(in.split("\n")))

					var insideMethod = false

					for (var i = 0 i < lines.size() i++) {

						String line = lines.get(i)

						if (line.startsWith(".method")) {
							insideMethod = true
						}

						if (line.startsWith(".end method")) {
							insideMethod = false
						}

						if (insideMethod && !shouldSkip(line)) {
							lines.set(i, line + "\n")
						}
					}

					StringBuilder result = StringBuilder()

					for (var i = 0 i < lines.size() i++) {
						if (i != 0) {
							result.append("\n")
						}

						result.append(lines.get(i))
					}

					return result.toString()
				}

				fun shouldSkip(String smaliLine): Boolean {

					String[] ops = {".line", ":", ".prologue"}

					for (String op : ops) {

						if (smaliLine.trim().startsWith(op)) {
							return true
						}
					}

					return false
				})
		} catch (e: Throwable) {
			dialog("Failed to extract smali source", Log.getStackTraceString(e),
					true)
		}
	}

	public fun decompile() {
		final String[] classes = getClassesFromDex()
		if (classes == null)
			return
		listDialog("Select a class to extract source", classes,
				(dialog, pos) -> {
				val claz = classes[pos].replace(".", "/")
				val latch = CountDownLatch(1)
				Executors.newSingleThreadExecutor()
					.execute(() -> {
						String[] args = {
							// "-jar",
							FileUtil.getBinDir()
									+ "classes/" + claz
									+ ".class",
							"--extraclasspath",
							FileUtil.getClasspathDir()
									+ "android.jar",
							"--outputdir",
							FileUtil.getBinDir()
									+ "cfr/"
						}

						try {
							org.benf.cfr.reader.Main.main(args)
						} catch (e: Exception) {
							dialog("Failed to decompile...",
									Log.getStackTraceString(e),
									true)
						}
						latch.countDown()
					})

				try {
					latch.await()
				} catch (e: InterruptedException) {
					e.printStackTrace()
					dialog("Thread was interrupted while decompiling...",
							e.getMessage(), true)
				}

				val edi = CodeEditor(
						MainActivity.this)

				edi.setTypefaceText(Typeface.MONOSPACE)

//				edi.setOverScrollEnabled(true)

				edi.setEditorLanguage(JavaLanguage())

				edi.setColorScheme(SchemeDarcula())

				edi.setTextSize(12)

				val decompiledClass = File(FileUtil.getBinDir() + "cfr/" + claz + ".java")

				try {
					edi.setText(Files.asCharSource(decompiledClass, Charsets.UTF_8).read())
				} catch (IOException e) {
					dialog("Cannot read file", Log.getStackTraceString(e), true)
				}

				val d = AlertDialog.Builder(
						MainActivity.this).setView(edi).create()
				d.setCanceledOnTouchOutside(true)
				d.show()
		})
	}

	fun disassemble() {
		val classes = getClassesFromDex()
		if (classes == null)
			return
		listDialog("Select a class to disassemble", classes,
				(dialog, pos) -> {
			val claz = classes[pos].replace(".", "/")
			
			val edi = CodeEditor(
					MainActivity.this)

			edi.setTypefaceText(Typeface.MONOSPACE)

//			edi.setOverScrollEnabled(true)

			edi.setEditorLanguage(JavaLanguage())

			edi.setColorScheme(SchemeDarcula())

			edi.setTextSize(12)
			
			try {
				val disassembly = ClassFileDisassembler(FileUtil.getBinDir() + "classes/" + claz + ".class").disassemble()
				
				edi.setText(disassembly)
				
				val d = AlertDialog.Builder(MainActivity.this).setView(edi).create()
				d.setCanceledOnTouchOutside(true)
				d.show()
			} catch (e: Throwable) {
				dialog("Failed to disassemble", Log.getStackTraceString(e), true)
			}
		}
	}

	public fun listDialog(title: String, items: String[],
			listener: DialogInterface.OnClickListener) {
		val dialog = MaterialAlertDialogBuilder(
				MainActivity.this).setTitle(title).setItems(items, listener)
		dialog.create().show()
	}

	public fun dialog(title: String, message: String, copyButton: Boolean) {
		val dialog = MaterialAlertDialogBuilder(
				MainActivity.this).setTitle(title).setMessage(message)
						.setPositiveButton("GOT IT", null)
						.setNegativeButton("CANCEL", null)
		if (copyButton)
			dialog.setNeutralButton("COPY",
					(dialogInterface, i) -> {
						((ClipboardManager) getSystemService(
							getApplicationContext().CLIPBOARD_SERVICE))
									.setPrimaryClip(ClipData
											.newPlainText("clipboard",
													message))
			})
		dialog.create().show()
	}

	public fun grantChmod(file: File) {
		try {
			if (file.isDirectory()) {
				Runtime.getRuntime()
						.exec("chmod 777 " + file.getAbsolutePath())
				for (f : file.listFiles()) {
					grantChmod(f)
				}
			} else {
				Runtime.getRuntime()
						.exec("chmod 777 " + file.getAbsolutePath())
			}
		} catch (e: Throwable) {
			e.printStackTrace()
		}
	}

	public getClassesFromDex(): String[] {
		try {
			val classes = ArrayList<>()
			org.jf.dexlib2.iface.DexFile dexfile = org.jf.dexlib2.DexFileFactory
					.loadDexFile(
							FileUtil.getBinDir()
									.concat("classes.dex"),
							org.jf.dexlib2.Opcodes.forApi(21))
			for (org.jf.dexlib2.iface.ClassDef f : dexfile.getClasses()
					.toArray(org.jf.dexlib2.iface.ClassDef[0])) {
				val name = f.getType().replace("/", ".")
				classes.add(name.substring(1, name.length() - 1))
			}
			return classes.toArray(new String[0])
		} catch (e: Exception) {
			dialog("Failed to get available classes in dex...",
					Log.getStackTraceString(e), true)
			return null
		}
	}
}
