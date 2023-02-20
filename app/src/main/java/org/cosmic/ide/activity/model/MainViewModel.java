package org.cosmic.ide.activity.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {

    // The files currently opened in the editor
    private MutableLiveData<List<File>> mFiles;

    // The current position of the CodeEditor
    private final MutableLiveData<Integer> currentPosition = new MutableLiveData<>(-1);

    private final MutableLiveData<Boolean> mDrawerState = new MutableLiveData<>(false);

    public LiveData<Boolean> getDrawerState() {
        return mDrawerState;
    }

    public void setDrawerState(boolean isOpen) {
        mDrawerState.setValue(isOpen);
    }

    public LiveData<List<File>> getFiles() {
        if (mFiles == null) {
            mFiles = new MutableLiveData<>(new ArrayList<>());
        }
        return mFiles;
    }

    public void setFiles(@NonNull List<File> files) {
        if (mFiles == null) {
            mFiles = new MutableLiveData<>(new ArrayList<>());
        }
        mFiles.setValue(files);
    }

    public LiveData<Integer> getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int pos) {
        currentPosition.setValue(pos);
    }

    @Nullable
    public File getCurrentFile() {
        List<File> files = getFiles().getValue();
        if (files == null) {
            return null;
        }

        Integer currentPos = currentPosition.getValue();
        if (currentPos == null || currentPos == -1) {
            return null;
        }

        if (files.size() - 1 < currentPos) {
            return null;
        }

        return files.get(currentPos);
    }

    public void clear() {
        mFiles.setValue(new ArrayList<>());
    }

    /**
     * Opens this file to the editor
     *
     * @param file The file to be opened
     * @return whether the operation was successful
     */
    public boolean openFile(File file) {
        setDrawerState(false);

        int index = -1;
        List<File> value = getFiles().getValue();
        if (value != null) {
            for (int i = 0; i < value.size(); i++) {
                index = i;
            }
            index = value.indexOf(file);
        }
        if (index != -1) {
            setCurrentPosition(index);
            return true;
        }
        addFile(file);
        return true;
    }

    public void addFile(File file) {
        List<File> files = getFiles().getValue();
        if (files == null) {
            files = new ArrayList<>();
        }
        if (!files.contains(file)) {
            files.add(file);
            mFiles.setValue(files);
        }
        setCurrentPosition(files.indexOf(file));
    }

    public void removeFile(File file) {
        List<File> files = getFiles().getValue();
        if (files == null) {
            return;
        }
        files.remove(file);
        mFiles.setValue(files);
    }

    // Remove all the files except the given file
    public void removeOthers(File file) {
        List<File> files = getFiles().getValue();
        if (files == null) {
            return;
        }
        files.clear();
        files.add(file);
        setFiles(files);
    }
}
