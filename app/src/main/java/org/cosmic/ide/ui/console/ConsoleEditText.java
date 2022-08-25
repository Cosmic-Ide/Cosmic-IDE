package org.cosmic.ide.ui.console;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.os.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import androidx.annotation.*;
import java.util.concurrent.atomic.AtomicBoolean;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.Log;

public class ConsoleEditText extends AppCompatEditText {
	private static final String TAG = "ConsoleEditText";
	private static final int NEW_OUTPUT = 1;
	private static final int NEW_ERR = 2;
	
	
	//length of text
	private int mLength = 0;
	
	//out, in and err stream
	private  PrintStream outputStream;
	private  InputStream inputStream;
	private  PrintStream errorStream;
	
	/**
	* uses for input
	*/
	private IntegerQueue mInputBuffer = new IntegerQueue(IntegerQueue.QUEUE_SIZE);
	
	/**
	* buffer for output
	*/
	private ByteQueue mStdoutBuffer = new ByteQueue(IntegerQueue.QUEUE_SIZE);
	
	/**
	* buffer for output
	*/
	private ByteQueue mStderrBuffer = new ByteQueue(IntegerQueue.QUEUE_SIZE);
	private AtomicBoolean isRunning = new AtomicBoolean(true);
	
	//filter input text, block a part of text
	private TextListener mTextListener = new TextListener();
	private EnterListener mEnterListener = new EnterListener();
	private byte[] mReceiveBuffer;
	private final Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			if (!isRunning.get()) {
				return;
			}
			if (msg.what == NEW_OUTPUT) {
				writeStdoutToScreen();
				} else if (msg.what == NEW_ERR) {
				writeStderrToScreen();
			}
		}
	};
	
	public ConsoleEditText(Context context) {
		super(context);
		init(context);
	}
	
	public ConsoleEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public ConsoleEditText(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}
	
	private void init(Context context) {
		if (!isInEditMode()) {
			/*
			AppSetting pref = new AppSetting(context);
			setTypeface(pref.getConsoleFont());
			setTextSize(pref.getConsoleTextSize());
			*/
		}
		setFilters(new InputFilter[]{mTextListener});
		setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		addTextChangedListener(mEnterListener);
		setMaxLines(2000);
		
		createIOStream();
	}
	
	private void createIOStream() {
		mReceiveBuffer = new byte[4 * 1024];
		mStdoutBuffer = new ByteQueue(4 * 1024);
		mStderrBuffer = new ByteQueue(4 * 1024);
		
		inputStream = new ConsoleInputStream(mInputBuffer);
		outputStream = new PrintStream(new ConsoleOutputStream(mStdoutBuffer, new StdListener() {
			@Override
			public void onUpdate() {
				mHandler.sendMessage(mHandler.obtainMessage(NEW_OUTPUT));
			}
		}));
		errorStream = new PrintStream(new ConsoleErrorStream(mStderrBuffer, new StdListener() {
			@Override
			public void onUpdate() {
				mHandler.sendMessage(mHandler.obtainMessage(NEW_ERR));
			}
		}));
	}
	
	private void writeStdoutToScreen() {
		
		int bytesAvailable = mStdoutBuffer.getBytesAvailable();
		int bytesToRead = Math.min(bytesAvailable, mReceiveBuffer.length);
		try {
			int bytesRead = mStdoutBuffer.read(mReceiveBuffer, 0, bytesToRead);
			//                        mEmulator.append(mReceiveBuffer, 0, bytesRead);
			String out = new String(mReceiveBuffer, 0, bytesRead);
			mLength = mLength + out.length();
			appendStdout(out);
			} catch (InterruptedException e) {
		}
	}
	
	private void writeStderrToScreen() {
		
		int bytesAvailable = mStderrBuffer.getBytesAvailable();
		int bytesToRead = Math.min(bytesAvailable, mReceiveBuffer.length);
		try {
			int bytesRead = mStderrBuffer.read(mReceiveBuffer, 0, bytesToRead);
			//                        mEmulator.append(mReceiveBuffer, 0, bytesRead);
			String out = new String(mReceiveBuffer, 0, bytesRead);
			mLength = mLength + out.length();
			appendStderr(out);
			} catch (InterruptedException e) {
		}
		//
		//        String out = new String(Character.toChars(read));
		//        mLength = mLength + out.length();
		//        appendStdout(out);
	}
	
	@WorkerThread
	public PrintStream getOutputStream() {
		return outputStream;
	}
	
	@WorkerThread
	public InputStream getInputStream() {
		return inputStream;
	}
	
	@WorkerThread
	public PrintStream getErrorStream() {
		return errorStream;
	}
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}
	
	@UiThread
	private void appendStdout(final CharSequence spannableString) {
		mHandler.post(() -> {
			append(spannableString);
		});
	}
	
	@UiThread
	private void appendStderr(final CharSequence str) {
		mHandler.post(() -> {
			SpannableString spannableString = new SpannableString(str);
			spannableString.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, str.length(),
			    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			append(spannableString);
		});
	}
	
	
	public void stop() {
		mInputBuffer.write(-1);
		isRunning.set(false);
	}
	
	public interface StdListener {
		void onUpdate();
	}
	
	private static class ConsoleOutputStream extends OutputStream {
		private ByteQueue mStdoutBuffer;
		private StdListener listener;
		
		private ConsoleOutputStream(ByteQueue mStdoutBuffer, StdListener listener) {
			this.mStdoutBuffer = mStdoutBuffer;
			this.listener = listener;
		}
		
		@Override
		public void write(@NonNull byte[] b, int off, int len) throws IOException {
			try {
				mStdoutBuffer.write(b, off, len);
				listener.onUpdate();
				} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void write(int b) throws IOException {
			write(new byte[]{(byte) b}, 0, 1);
		}
	}
	
	private static class ConsoleErrorStream extends OutputStream {
		private ByteQueue mStderrBuffer;
		private StdListener stdListener;
		
		public ConsoleErrorStream(ByteQueue mStderrBuffer, StdListener stdListener) {
			this.mStderrBuffer = mStderrBuffer;
			this.stdListener = stdListener;
		}
		
		@Override
		public void write(@NonNull byte[] b, int off, int len) throws IOException {
			try {
				mStderrBuffer.write(b, off, len);
				stdListener.onUpdate();
				} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void write(int b) throws IOException {
			write(new byte[]{(byte) b}, 0, 1);
		}
	}
	
	private static class ConsoleInputStream extends InputStream {
		private final Object mLock = new Object();
		@NonNull
		private IntegerQueue mInputBuffer;
		
		public ConsoleInputStream(@NonNull IntegerQueue mInputBuffer) {
			this.mInputBuffer = mInputBuffer;
		}
		
		@Override
		public int read() throws IOException {
			synchronized (mLock) {
				return mInputBuffer.read();
			}
		}
	}
	
	private class EnterListener implements TextWatcher {
		
		private int start;
		private int count;
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			
		}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			this.start = start;
			this.count = count;
		}
		
		@Override
		public void afterTextChanged(Editable s) {
			if (count == 1 && s.charAt(start) == '\n' && start >= mLength) {
				String data = s.toString().substring(mLength);
				for (char c : data.toCharArray()) {
					mInputBuffer.write(c);
				}
				mInputBuffer.write(-1); //flush
				mLength = s.length(); //append to console
			}
		}
	}
	
	private class TextListener implements InputFilter {
		public CharSequence removeStr(CharSequence removeChars, int startPos) {
			if (startPos < mLength) { //this mean output from console
				return removeChars; //can not remove console output
		    }else{
				return "";	
			}
		}
		
		public CharSequence insertStr(CharSequence newChars, int startPos) {
			if (startPos < mLength) { //it mean output from console
				return newChars;
				
				} else { //(startPos >= mLength)
				SpannableString spannableString = new SpannableString(newChars);
				spannableString.setSpan(new ForegroundColorSpan(Color.GREEN), 0,
				spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				return spannableString;
				
			}
		}
		
		public CharSequence updateStr(CharSequence oldChars, int startPos, CharSequence newChars) {
			if (startPos < mLength) {
				return oldChars; //don't edit
				
				} else {//if (startPos >= mLength)
				SpannableString spannableString = new SpannableString(newChars);
				spannableString.setSpan(new ForegroundColorSpan(Color.GREEN), 0,
				spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				return spannableString;
			}
		}
		
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			CharSequence returnStr = source;
			String curStr = dest.subSequence(dstart, dend).toString();
			String newStr = source.toString();
			int length = end - start;
			int dlength = dend - dstart;
			if (dlength > 0 && length == 0) {
				// Case: Remove chars, Simple
				returnStr = TextListener.this.removeStr(dest.subSequence(dstart, dend), dstart);
				} else if (length > 0 && dlength == 0) {
				// Case: Insert chars, Simple
				returnStr = TextListener.this.insertStr(source.subSequence(start, end), dstart);
				} else if (curStr.length() > newStr.length()) {
				// Case: Remove string or replace
				if (curStr.startsWith(newStr)) {
					// Case: Insert chars, by append
					returnStr = TextUtils.concat(curStr.subSequence(0, newStr.length()), TextListener.this.removeStr(curStr.subSequence(newStr.length(), curStr.length()), dstart + curStr.length()));
					} else {
					// Case Replace chars.
					returnStr = TextListener.this.updateStr(curStr, dstart, newStr);
				}
				} else if (curStr.length() < newStr.length()) {
				// Case: Append String or rrepace.
				if (newStr.startsWith(curStr)) {
					// Addend, Insert
					returnStr = TextUtils.concat(curStr, TextListener.this.insertStr(newStr.subSequence(curStr.length(), newStr.length()), dstart + curStr.length()));
					} else {
					returnStr = TextListener.this.updateStr(curStr, dstart, newStr);
				}
			} else {
				// No update
			}
			
			// If the return value is same as the source values, return the source value.
			return returnStr;
		}
	}
}
