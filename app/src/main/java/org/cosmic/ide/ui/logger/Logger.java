package org.cosmic.ide.ui.logger;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Logger {

    private LogAdapter adapter;
    private List<Log> data = new ArrayList<>();

    private RecyclerView mRecyclerView;

    public void attach(RecyclerView view) {
        mRecyclerView = view;
        init();
    }

    private void init() {
        adapter = new LogAdapter(data);
        var layoutManager = new LinearLayoutManager(mRecyclerView.getContext());
        layoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(adapter);
    }

    public void message(String message) {
        message(null, message);
    }

    public void message(String tag, String message) {
        mRecyclerView.post(
                () -> {
                    data.add(new Log(tag, message));
                    adapter.notifyItemInserted(data.size());
                    mRecyclerView.smoothScrollToPosition(data.size() - 1);
                });
    }
}
