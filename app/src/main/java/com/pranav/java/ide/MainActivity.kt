package com.pranav.java.ide

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.common.io.ByteStreams
import com.google.common.io.Files
import com.google.common.base.Charsets
import com.googlecode.d2j.smali.BaksmaliCmd
import com.pranav.lib_android.exception.CompilationFailedException
import com.pranav.lib_android.task.JavaBuilder
import com.pranav.lib_android.task.java.CompileJavaTask
import com.pranav.lib_android.task.java.D8Task
import com.pranav.lib_android.task.java.ExecuteJavaTask
import com.pranav.lib_android.code.disassembler.ClassFileDisassembler
import com.pranav.lib_android.code.formatter.Formatter
import com.pranav.lib_android.util.ZipUtil
import com.pranav.lib_android.util.FileUtil
import com.pranav.lib_android.util.ConcurrentUtil

import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays

import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile

class MainActivity: AppCompatActivity() {

	private lateinit var editor: CodeEditor

	private var d8Time: Long = 0
	private var ecjTime: Long = 0

	private var errorsArePresent = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		setSupportActionBar(findViewById(R.id.toolbar))
		getSupportActionBar().setDisplayHomeAsUpEnabled(false)
		getSupportActionBar().setHomeButtonEnabled(false)

		editor = findViewById(R.id.editor)

		editor.setTypefaceText(Typeface.MONOSPACE)
		editor.setEditorLanguage(JavaLanguage())
		editor.setColorScheme(SchemeDarcula())
		editor.setTextSize(12)

		val file = File(FileUtil.getJavaDir(), "Main.java")

		if (file.exists()) {
			try {
				editor.setText(
            file.readText()
				);
			} catch (e: Exception) {
				dialog("Cannot read file", getString(e), true)
			}
		} else {
			editor.setText(
          "package com.example;\n\nimport java.util.*;\n\n" +
          "public class Main {\n\n" +
          "\tpublic static void main(String[] args) {\n" +
          "\t\tSystem.out.print(\"Hello, World!\");\n" + "\t}\n" +
          "}\n"
			)
		}
		
		val builder = JavaBuilder(getApplicationContext(),
				getClassLoader())

		ConcurrentUtil.executeInBackground {
			if (!File(FileUtil.getClasspathDir(), "android.jar").exists()) {
				ZipUtil.unzipFromAssets(MainActivity.this,
					"android.jar.zip", FileUtil.getClasspathDir())
			}
			val output = File(FileUtil.getClasspathDir(), "core-lambda-stubs.jar")
			if (!output.exists() && 
					 getSharedPreferences("compiler_settings", Context.MODE_PRIVATE)
					.getFloat("javaVersion", 7.0f) >= 8.0f) {
				try {
				  output.writeBytes(getAssets().open("core-lambda-stubs.jar").readBytes())
				} catch (e: Exception) {
					showErr(getString(e))
				}
			}
		}

		findViewById(R.id.btn_disassemble).setOnClickListener { _ -> disassemble()}
		findViewById(R.id.btn_smali2java).setOnClickListener { _ -> decompile()}
		findViewById(R.id.btn_smali).setOnClickListener { _ -> smali()}
		findViewById(R.id.btn_run).setOnClickListener { _ ->
			try {
				// Delete previous build files
				FileUtil.deleteFile(FileUtil.getBinDir())
				File(FileUtil.getBinDir()).mkdirs()
				val mainFile = File(
						FileUtil.getJavaDir() + "Main.java");
				Files.createParentDirs(mainFile);
				// a simple workaround to prevent calls to system.exit
				mainFile.writeText(editor.getText().toString()
            .replace("System.exit(",
                "System.err.print(\"Exit code \" + ")
        )
			} catch (e: IOException) {
        dialog("Cannot save program", getString(e), true)
			}

			// code that runs ecj
			Long time = System.currentTimeMillis()
			errorsArePresent = true
			try {
				val javaTask = CompileJavaTask(builder)
				javaTask.doFullTask()
				errorsArePresent = false
			} catch (e: CompilationFailedException) {
				showErr(e.getMessage())
			} catch (e: Throwable) {
				showErr(getString(e))
			}
			if (errorsArePresent) {
			  return
			}

			ecjTime = System.currentTimeMillis() - time
			time = System.currentTimeMillis()
			
			// run d8
			try {
				D8Task().doFullTask()
			} catch (e: Throwable) {
				errorsArePresent = true
				showErr(e.toString())
				return
			}
			d8Time = System.currentTimeMillis() - time
			// code that loads the final dex
			try {
				val classes = getClassesFromDex()
				if (classes == null) {
					return
				}
				listDialog("Select a class to execute", classes,
						{ _, pos ->
							val task = ExecuteJavaTask(
								builder, classes[pos])
						try {
							task.doFullTask();
						} catch (e: java.lang.reflect.InvocationTargetException) {
							dialog("Failed...",
									"Runtime error: " +
									e.getMessage() +
									"\n\n" +
									getString(e),
									true)
						} catch (e: Exception) {
								dialog("Failed..",
										"Couldn't execute the dex: "
												+ e.toString()
												+ "\n\nSystem logs:\n"
												+ task.getLogs(),
										true)
							}
							val s = StringBuilder()
							s.append("Success! ECJ took: ")
							s.append(String.valueOf(ecjTime))
							s.append("ms, ")
							s.append("D8")
							s.append(" took: ")
							s.append(String.valueOf(d8Time))
							s.append("ms")
							dialog(s.toString(), task.getLogs(), true)
					})
			} catch (e: Throwable) {
				showErr(getString(e))
			}
		}
	}

	fun showErr(e: String) {
		Snackbar.make(findViewById(R.id.container) as LinearLayout, "An error occurred",
				Snackbar.LENGTH_INDEFINITE)
				.setAction("Show error", { _ -> dialog("Failed...", e, true)})
				.show()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menu.add(0, 0, 0, "Format")
		menu.add(0, 1, 0, "Settings")
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.getItemId()) {
			0 -> {
				val formatter = Formatter(
						editor.getText().toString())
				ConcurrentUtil.execute {
				  editor.setText(formatter.format())
				}
			}

			1 -> {
				val intent = getIntent();
				intent.setClass(this@MainActivity, SettingActivity::class.java)
				startActivity(intent)
			}
		}
		return super.onOptionsItemSelected(item)
	}

	fun smali() {
		try {
			val classes = getClassesFromDex()
			if (classes == null)
				return
			listDialog("Select a class to extract source", classes,
					{ _, pos ->
						val claz = classes[pos]
						val args = listOf(
						  "-f",
						  "-o",
						  FileUtil.getBinDir()
						      .concat("smali/"),
						  FileUtil.getBinDir()
						      .concat("classes.dex")
						)
						ConcurrentUtil.execute {
						  BaksmaliCmd.main(args)
						}

						val edi = CodeEditor(this@MainActivity)
						edi.setTypefaceText(Typeface.MONOSPACE)
						edi.setEditorLanguage(JavaLanguage())
						edi.setColorScheme(SchemeDarcula())
						edi.setTextSize(13)
							
						val smaliFile = File(FileUtil.getBinDir() + "smali/" + claz.replace(".", "/") + ".smali")
							
						try {
						    edi.setText(formatSmali(smaliFile.readText()))
						} catch (e: IOException) {
							dialog("Cannot read file", getString(e), true)
						}

						val dialog = AlertDialog.Builder(
								this).setView(edi).create()
						dialog.setCanceledOnTouchOutside(true)
						dialog.show()
			})
		} catch (e: Throwable) {
			dialog("Failed to extract smali source", getString(e),
					true)
		}
	}

	fun decompile() {
		val classes = getClassesFromDex()
		if (classes == null)
			return
		listDialog("Select a class to extract source", classes,
				{ _, pos ->
					val claz = classes[pos].replace(".", "/")
          val args = listOf(
							FileUtil.getBinDir() +
                  "classes/" +
                  claz + // full class name
                  ".class",
							"--extraclasspath",
							FileUtil.getClasspathDir() + "android.jar",
							"--outputdir",
							FileUtil.getBinDir() + "cfr/"
						)

					ConcurrentUtil.execute {
						try {
							org.benf.cfr.reader.Main.main(args)
						} catch (e: Exception) {
							dialog("Failed to decompile...",
									getString(e), true)
						}
					}

					val edi = CodeEditor(this)
					edi.setTypefaceText(Typeface.MONOSPACE)
					edi.setEditorLanguage(JavaLanguage())
					edi.setColorScheme(SchemeDarcula())
					edi.setTextSize(12)

					val decompiledFile = File(
							FileUtil.getBinDir() + "cfr/" + claz + ".java")

					try {
						edi.setText(decompiledFile.readText())
					} catch (e: IOException) {
						dialog("Cannot read file", getString(e),
								true)
					}

					val d = AlertDialog.Builder(
							this).setView(edi).create()
					d.setCanceledOnTouchOutside(true)
					d.show();
		})
	}

	fun disassemble() {
		val classes = getClassesFromDex();
		if (classes == null)
			return
		listDialog("Select a class to disassemble", classes, { _, pos ->
			val claz = classes[pos].replace(".", "/")

			val edi = CodeEditor(this)
			edi.setTypefaceText(Typeface.MONOSPACE)
			edi.setEditorLanguage(JavaLanguage())
			edi.setColorScheme(SchemeDarcula())
			edi.setTextSize(12)

			try {
				val disassembled = ClassFileDisassembler(
						FileUtil.getBinDir() + "classes/" + claz + ".class")
								.disassemble()

				edi.setText(disassembled)

				val d = AlertDialog.Builder(MainActivity.this)
						.setView(edi).create()
				d.setCanceledOnTouchOutside(true)
				d.show()
			} catch (e: Throwable) {
				dialog("Failed to disassemble", getString(e),
						true)
			}
		})
	}

	private fun formatSmali(in: String): String {

		val lines = ArrayList<String>(
				Arrays.asList(in.split("\n"))
		)

		var insideMethod = false

		for (i: Int = 0; i < lines.size(); i++) {

			val line = lines.get(i)

			if (line.startsWith(".method"))
				insideMethod = true

			if (line.startsWith(".end method"))
				insideMethod = false

			if (insideMethod && !shouldSkip(line))
				lines.set(i, line + "\n")
		}

		val result = StringBuilder()

		for (i: Int = 0; i < lines.size(); i++) {
			if (i != 0)
				result.append("\n")

			result.append(lines.get(i));
		}

		return result.toString()
	}

	private fun shouldSkip(smaliLine: String): Boolean {

		val ops = {".line", ":", ".prologue"}

		for (op in ops) {
			if (smaliLine.trim().startsWith(op))
				return true
		}
		return false
	}

	fun listDialog(title: String, items: Array<String>,
			listener: DialogInterface.OnClickListener) {
		MaterialAlertDialogBuilder(MainActivity.this)
        .setTitle(title)
        .setItems(items, listener)
        .create()
        .show()
	}

	fun dialog(title: String, message: String, copyButton: Boolean) {
		val dialog = MaterialAlertDialogBuilder(
				this).setTitle(title)
            .setMessage(message)
						.setPositiveButton("GOT IT", null)
						.setNegativeButton("CANCEL", null)
		if (copyButton)
			dialog.setNeutralButton("COPY", { _, _ ->
				(getSystemService(
						getApplicationContext().CLIPBOARD_SERVICE) as ClipboardManager)
								.setPrimaryClip(ClipData
										.newPlainText("clipboard", message));
			})
		dialog.create().show()
	}

	fun getClassesFromDex(): Array<String> {
		try {
			val classes = ArrayList<String>();
			val dexfile = DexFileFactory.loadDexFile(FileUtil.getBinDir().concat("classes.dex"),
							Opcodes.forApi(26)
			);
			for (f in dexfile.getClasses()
					.toTypedArray()
			) {
				val name = f.getType().replace("/", "."); // convert class name to standard form
				classes.add(name.substring(1, name.length() - 1))
			}
			return classes.toTypedArray()
		} catch (e: Exception) {
			dialog("Failed to get available classes in dex...",
					getString(e), true)
			return null
		}
	}
	
	private fun getString(e: Throwable): String {
	  return Log.getStackTraceString(e)
	}
}
