package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static com.alphawallet.app.repository.EthereumNetworkBase.VELAS_MAINNET_ID;

public class SelectActiveNetworkActivity extends SelectNetworkActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        singleItem = false;
    }

    @Override
    protected void setupFilterList() {
        ArrayList<NetworkType> list = new ArrayList<>();
        List<Integer> intList = Utils.intListToArray(selectedChainId);
        List<Integer> activeNetworks = viewModel.getActiveNetworks();

        //Ensure that there's always a network selected in single network mode
        if (singleItem && (intList.size() < 1 || !activeNetworks.contains(intList.get(0)))) {
            intList.clear();
            intList.add(VELAS_MAINNET_ID);
        }

        //if active networks is empty ensure mainnet is displayed
        if (activeNetworks.size() == 0) {
            activeNetworks.add(VELAS_MAINNET_ID);
            intList.add(VELAS_MAINNET_ID);
        }

        list.add(new NetworkType("Velas Networks", NetworkType.Type.Header));
        for (NetworkInfo info : viewModel.getVelasNetworkList()) {
            NetworkItem item = new NetworkItem(info.name, info.chainId, intList.contains(info.chainId));
            if (info.chainId == viewModel.getVelasIdNodeSelected()) {
                item.setSelected(true);
            } else {
                item.setSelected(false);
            }
            list.add(new NetworkType(item, NetworkType.Type.NetworkItem_Single, true));
        }

        list.add(new NetworkType("Other Networks", NetworkType.Type.Header));
        for (NetworkInfo info : viewModel.getNetworkList()) {
            if (!singleItem || activeNetworks.contains(info.chainId)) {
                if (info.chainId != VELAS_MAINNET_ID) {
                    NetworkItem item = new NetworkItem(info.name, info.chainId, intList.contains(info.chainId));
                    list.add(new NetworkType(item, NetworkType.Type.NetworkItem_Multiple));
                }
            }
        }
        adapter = new NetworkItemAdapter(list, singleItem);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void handleSetNetworks() {
        Integer[] filterList = ((NetworkItemAdapter) adapter).getSelectedItems();
        if (filterList.length == 0) {
            filterList = EthereumNetworkRepository.addDefaultNetworks().toArray(new Integer[0]);
        }
        viewModel.setFilterNetworks(filterList);
        sendBroadcast(new Intent(C.RESET_WALLET));
        finish();
    }

    private static class NetworkType {
        private Object item;
        private int type;
        private boolean singleItem = false;

        NetworkType(Object item, int type) {
            this.item = item;
            this.type = type;
        }

        NetworkType(Object item, int type, boolean singleItem) {
            this.item = item;
            this.type = type;
            this.singleItem = singleItem;
        }

        Object getItem() {
            return item;
        }

        int getType() {
            return type;
        }

        interface Type {
            int NetworkItem_Single = 0;
            int NetworkItem_Multiple = 1;
            int Header = 2;
        }
    }

    private class NetworkItemAdapter extends RecyclerView.Adapter<BinderViewHolder> {
        private ArrayList<NetworkType> dataSet;
        private int chainId;
        private final boolean singleItem;

        public Integer[] getSelectedItems() {
            List<Integer> enabledIds = new ArrayList<>();
            for (NetworkType data : dataSet) {
                if ((data.item instanceof NetworkItem) &&
                        ((NetworkItem) data.item).isSelected()) {
                    enabledIds.add(((NetworkItem) data.item).getChainId());
                }
            }
            return enabledIds.toArray(new Integer[0]);
        }

        @Override
        public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            BinderViewHolder holder = null;
            switch (viewType) {
                case NetworkItemViewHolder.VIEW_TYPE_SINGLE: {
                    int buttonTypeId = R.layout.item_simple_radio;
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(buttonTypeId, parent, false);
                    holder = new NetworkItemViewHolder(itemView);
                    break;
                }
                case NetworkItemViewHolder.VIEW_TYPE_MULTIPLE: {
                    int buttonTypeId = R.layout.item_simple_check;
                    View itemView = LayoutInflater.from(parent.getContext())
                            .inflate(buttonTypeId, parent, false);
                    holder = new NetworkItemViewHolder(itemView);
                    break;
                }
                case HeaderViewHolder.VIEW_TYPE:
                    holder = new HeaderViewHolder(R.layout.item_select_network_head, parent);
                    break;

            }
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull BinderViewHolder holder, int position) {
            holder.bind(dataSet.get(position));
        }

        class NetworkItemViewHolder extends BinderViewHolder<NetworkType> {
            public static final int VIEW_TYPE_SINGLE = 60003;
            public static final int VIEW_TYPE_MULTIPLE = 60004;
            ImageView checkbox;
            TextView name;
            View itemLayout;

            NetworkItemViewHolder(View view) {
                super(view);
                checkbox = view.findViewById(R.id.checkbox);
                name = view.findViewById(R.id.name);
                itemLayout = view.findViewById(R.id.layout_list_item);
            }

            @Override
            public void bind(@Nullable NetworkType networkType, @NonNull Bundle addition) {
                NetworkItem item = (NetworkItem) networkType.getItem();
                if (item != null) {
                    name.setText(item.getName());
                    itemLayout.setOnClickListener(v -> clickListener(this, dataSet.indexOf(networkType)));
                    checkbox.setSelected(item.isSelected());
                    checkbox.setAlpha(1.0f);
                }
            }
        }

        class HeaderViewHolder extends BinderViewHolder<NetworkType> {
            public static final int VIEW_TYPE = 6004;
            private final TextView title;

            public HeaderViewHolder(int resId, ViewGroup parent) {
                super(resId, parent);
                title = (TextView) findViewById(R.id.title);
            }

            @Override
            public void bind(@Nullable NetworkType networkType, @NonNull Bundle addition) {
                title.setText(networkType.getItem().toString());
            }
        }

        private NetworkItemAdapter(ArrayList<NetworkType> data, boolean singleItem) {
            this.dataSet = data;
            this.singleItem = singleItem;
        }

        private void clickListener(final NetworkItemViewHolder holder, final int position) {
            NetworkType networkType = dataSet.get(position);
            NetworkItem networkItem = (NetworkItem) networkType.getItem();
            if (networkType.getType() == NetworkType.Type.NetworkItem_Single) {
                for (NetworkType network : dataSet) {
                    if ((network.getItem() instanceof NetworkItem) &&
                            EthereumNetworkRepository.isVelasNetwork(((NetworkItem) network.getItem()).getChainId())) {
                        ((NetworkItem) network.getItem()).setSelected(false);
                    }
                }
                networkItem.setSelected(true);
                notifyDataSetChanged();
            } else if (networkType.getType() == NetworkType.Type.NetworkItem_Multiple) {
                networkItem.setSelected(!networkItem.isSelected());
            }
            holder.checkbox.setSelected(networkItem.isSelected());
        }

        @Override
        public int getItemCount() {
            return dataSet.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (dataSet.get(position).type == NetworkType.Type.Header) {
                return HeaderViewHolder.VIEW_TYPE;
            } else if (dataSet.get(position).type == NetworkType.Type.NetworkItem_Single) {
                return NetworkItemViewHolder.VIEW_TYPE_SINGLE;
            }
            return NetworkItemViewHolder.VIEW_TYPE_MULTIPLE;
        }
    }
}
