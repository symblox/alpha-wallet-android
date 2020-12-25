package com.alphawallet.app.ui.widget.entity;

public class GroupTokensSortedItem extends SortedItem<GroupTokenData> {

    public GroupTokensSortedItem(int viewType, GroupTokenData data, int weight) {
        super(viewType, data, weight);
    }

    @Override
    public int compare(SortedItem other) {
        return viewType - other.viewType;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        return false;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return other.viewType == viewType;
    }
}

