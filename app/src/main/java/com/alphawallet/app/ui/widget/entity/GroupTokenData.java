package com.alphawallet.app.ui.widget.entity;

import androidx.recyclerview.widget.SortedList;

public class GroupTokenData {
    public String walletAddress;
    public String groupName;
    private int chainId;
    private int sectionColor;

    public int getChainId() {
        return chainId;
    }

    public int getSectionColor() {
        return sectionColor;
    }

    public void setSectionColor(int sectionColor) {
        this.sectionColor = sectionColor;
    }

    public interface GroupTokenDataListener {
        void notifyItemRangeChanged(int position, int count);
        void notifyItemRangeInserted(int position, int count);
        void notifyItemRangeRemoved(int position, int count);
        void notifyItemMoved(int fromPosition, int toPosition);
    }

    private GroupTokenDataListener listener;
    protected final SortedList<SortedItem> items = new SortedList<>(SortedItem.class, new SortedList.Callback<SortedItem>() {
        @Override
        public int compare(SortedItem o1, SortedItem o2) {
            return o1.compare(o2);
        }

        @Override
        public void onChanged(int position, int count) {
            listener.notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(SortedItem oldItem, SortedItem newItem) {
            return oldItem.areContentsTheSame(newItem);
        }

        @Override
        public boolean areItemsTheSame(SortedItem item1, SortedItem item2) {
            return item1.areItemsTheSame(item2);
        }

        @Override
        public void onInserted(int position, int count) {
            listener.notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            listener.notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            listener.notifyItemMoved(fromPosition, toPosition);
        }
    });

    public GroupTokenData(String walletAddress, String groupName, int chainId, GroupTokenDataListener listener) {
        this.walletAddress = walletAddress;
        this.groupName = groupName;
        this.listener = listener;
        this.chainId = chainId;
    }

    public SortedList<SortedItem> getItems() {
        return items;
    }

    public void clear() {
        items.beginBatchedUpdates();
        items.clear();
        items.endBatchedUpdates();
    }
}
