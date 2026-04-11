package com.kresshy.weatherstation.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.databinding.FragmentLogManagerBinding;
import com.kresshy.weatherstation.databinding.LogItemBinding;

import dagger.hilt.android.AndroidEntryPoint;

import timber.log.Timber;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragment that provides a management interface for application log files. Allows users to view,
 * selectively share via the Android Share Sheet, or delete stored log files.
 */
@AndroidEntryPoint
public class LogManagerFragment extends Fragment {

    private FragmentLogManagerBinding binding;
    private LogAdapter adapter;
    private final List<File> logFiles = new ArrayList<>();
    private final Set<File> selectedFiles = new HashSet<>();

    /** Required empty public constructor for fragment instantiation. */
    public LogManagerFragment() {}

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLogManagerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new LogAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        binding.btnShare.setOnClickListener(v -> shareSelectedLogs());
        binding.btnDelete.setOnClickListener(v -> confirmDeleteLogs());

        loadLogFiles();
    }

    /** Scans the internal logs directory and updates the UI list. */
    private void loadLogFiles() {
        File logsDir = new File(requireContext().getExternalFilesDir(null), "logs");
        File[] files = logsDir.listFiles((dir, name) -> name.endsWith(".html"));
        logFiles.clear();
        selectedFiles.clear();
        if (files != null) {
            logFiles.addAll(Arrays.asList(files));
            // Sort by newest first
            Collections.sort(
                    logFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        }
        adapter.notifyDataSetChanged();

        if (logFiles.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_logs_found, Toast.LENGTH_SHORT).show();
        }
    }

    /** Prepares and launches the Android Share Sheet for selected log files. */
    private void shareSelectedLogs() {
        if (selectedFiles.isEmpty()) {
            Toast.makeText(
                            requireContext(),
                            "Please select at least one log file",
                            Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("text/html");
        ArrayList<Uri> uris = new ArrayList<>();
        for (File file : selectedFiles) {
            uris.add(
                    FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            file));
        }
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    /** Displays a confirmation dialog before deleting selected files. */
    private void confirmDeleteLogs() {
        if (selectedFiles.isEmpty()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.confirm_delete_logs, selectedFiles.size()))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteSelectedLogs())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Deletes the selected files from storage and refreshes the list. */
    private void deleteSelectedLogs() {
        for (File file : selectedFiles) {
            if (file.delete()) {
                Timber.d("Deleted log file: %s", file.getName());
            }
        }
        loadLogFiles();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** Adapter class for displaying log files in a RecyclerView with selection support. */
    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LogItemBinding itemBinding =
                    LogItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = logFiles.get(position);
            holder.binding.fileName.setText(file.getName());
            holder.binding.checkBox.setChecked(selectedFiles.contains(file));

            holder.itemView.setOnClickListener(
                    v -> {
                        if (selectedFiles.contains(file)) {
                            selectedFiles.remove(file);
                        } else {
                            selectedFiles.add(file);
                        }
                        notifyItemChanged(position);
                    });
        }

        @Override
        public int getItemCount() {
            return logFiles.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final LogItemBinding binding;

            ViewHolder(LogItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
