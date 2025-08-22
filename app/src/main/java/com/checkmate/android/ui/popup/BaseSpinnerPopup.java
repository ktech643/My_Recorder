package com.checkmate.android.ui.popup;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.checkmate.android.R;
import com.lxj.xpopup.core.BottomPopupView;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseSpinnerPopup extends BottomPopupView {
    protected List<SpinnerOption> options;
    protected OnItemSelectedListener listener;
    protected String currentSelection;
    protected boolean isMultiSelect = false;
    protected Set<String> selectedItems = new HashSet<>();

    public BaseSpinnerPopup(@NonNull Context context) {
        super(context);
        options = new ArrayList<>();
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.popup_spinner;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        // Remove any default margins or padding
        getPopupContentView().setPadding(0, 0, 0, 0);
        
        // Setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        SpinnerAdapter adapter = new SpinnerAdapter();
        recyclerView.setAdapter(adapter);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }

    public void setCurrentSelection(String selection) {
        this.currentSelection = selection;
        if (isMultiSelect) {
            selectedItems.clear();
            if (selection != null && !selection.isEmpty()) {
                selectedItems.add(selection);
            }
        }
    }

    public void setMultiSelect(boolean multiSelect) {
        this.isMultiSelect = multiSelect;
    }

    public void setSelectedItems(Set<String> items) {
        if (isMultiSelect) {
            this.selectedItems = new HashSet<>(items);
        }
    }

    public Set<String> getSelectedItems() {
        return new HashSet<>(selectedItems);
    }

    protected class SpinnerAdapter extends RecyclerView.Adapter<SpinnerAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_spinner, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SpinnerOption option = options.get(position);
            holder.title.setText(option.title);
            if (option.iconResId != 0) {
                holder.icon.setImageResource(option.iconResId);
                holder.icon.setVisibility(View.VISIBLE);
            } else {
                holder.icon.setVisibility(View.GONE);
            }

            boolean isSelected;
            if (isMultiSelect) {
                isSelected = selectedItems.contains(option.id);
            } else {
                isSelected = option.id.equals(currentSelection);
            }
            
            holder.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            holder.title.setTextColor(getContext().getResources().getColor(isSelected ? R.color.blue : android.R.color.black));

            holder.itemView.setOnClickListener(v -> {
                if (isMultiSelect) {
                    // Toggle selection for multi-select
                    if (selectedItems.contains(option.id)) {
                        selectedItems.remove(option.id);
                    } else {
                        selectedItems.add(option.id);
                    }
                    notifyItemChanged(position);
                } else {
                    // Single selection - call listener and dismiss
                    if (listener != null) {
                        listener.onItemSelected(option.id);
                    }
                    dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView title;
            ImageView check;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.icon);
                title = itemView.findViewById(R.id.title);
                check = itemView.findViewById(R.id.check);
            }
        }
    }

    public static class SpinnerOption {
        public String id;
        public String title;
        public int iconResId;

        public SpinnerOption(String id, String title, int iconResId) {
            this.id = id;
            this.title = title;
            this.iconResId = iconResId;
        }
    }

    public interface OnItemSelectedListener {
        void onItemSelected(String id);
        default void onItemsSelected(Set<String> ids) {
            // Default implementation for backward compatibility
            if (ids.size() == 1) {
                onItemSelected(ids.iterator().next());
            }
        }
    }
}
