package org.cosmic.ide.activity.model;

import android.os.Environment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.cosmic.ide.common.util.CoroutineUtil;
import org.cosmic.ide.ui.treeview.TreeNode;
import org.cosmic.ide.ui.treeview.TreeUtil;
import org.cosmic.ide.ui.treeview.model.TreeFile;

import java.io.File;

public class FileViewModel extends ViewModel {

    private final MutableLiveData<File> mRoot =
            new MutableLiveData<>(Environment.getExternalStorageDirectory());
    private final MutableLiveData<TreeNode<TreeFile>> mNode = new MutableLiveData<>();

    public LiveData<TreeNode<TreeFile>> getNodes() {
        return mNode;
    }

    public LiveData<File> getRootFile() {
        return mRoot;
    }

    public void setRootFile(File root) {
        mRoot.setValue(root);
        refreshNode(root);
    }

    public void refreshNode(File root) {
        CoroutineUtil.execute(
                () -> {
                    TreeNode<TreeFile> node = TreeNode.root(TreeUtil.getNodes(root));
                    mNode.postValue(node);
                });
    }
}
