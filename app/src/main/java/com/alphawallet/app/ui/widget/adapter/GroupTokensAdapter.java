package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.entity.GroupTokenData;
import com.alphawallet.app.ui.widget.entity.GroupTokensSortedItem;
import com.alphawallet.app.ui.widget.entity.ManageTokensData;
import com.alphawallet.app.ui.widget.entity.ManageTokensSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TokenSortedItem;
import com.alphawallet.app.ui.widget.entity.TotalBalanceSortedItem;
import com.alphawallet.app.ui.widget.entity.WarningData;
import com.alphawallet.app.ui.widget.entity.WarningSortedItem;
import com.alphawallet.app.ui.widget.holder.AssetInstanceScriptHolder;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.GroupTokensHolder;
import com.alphawallet.app.ui.widget.holder.ManageTokensHolder;
import com.alphawallet.app.ui.widget.holder.TokenGridHolder;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.ui.widget.holder.TotalBalanceHolder;
import com.alphawallet.app.ui.widget.holder.WarningHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

public class GroupTokensAdapter extends RecyclerView.Adapter<BinderViewHolder> {
    private static final String TAG = "TKNADAPTER";
    public static final int FILTER_ALL = 0;
    public static final int FILTER_CURRENCY = 1;
    public static final int FILTER_ASSETS = 2;
    public static final int FILTER_COLLECTIBLES = 3;
    private static final BigDecimal CUTOFF_VALUE = BigDecimal.valueOf(99999999999L);
    private final Realm realm;

    private int filterType;
    protected final AssetDefinitionService assetService;
    protected final TokensService tokensService;
    private ContractLocator scrollToken; // designates a token that should be scrolled to

    private Context context;
    private String walletAddress;
    private boolean debugView = false;

    private boolean gridFlag;

    //view types
    public static final int VIEWTYPE_BASE_ETH_SYMBLOX_LABEL = 2099;
    public static final int VIEWTYPE_BASE_ETH_MAINNET_LABEL = 20199;
    public static final int VIEWTYPE_BASE_ETH_BINANCE_LABEL = 20299;
    public static final int VIEWTYPE_BASE_ETH_HECO_LABEL = 20399;
    public static final int VIEWTYPE_BASE_ETH_OTHER_LABEL = 20999;

    protected final OnTokenClickListener onTokenClickListener;

    protected final SortedList<SortedItem> groupItems = new SortedList<>(SortedItem.class, new SortedList.Callback<SortedItem>() {
        @Override
        public int compare(SortedItem o1, SortedItem o2) {
            //Note: ViewTypes are numbered in order of appearance
            if (isManagedLabel(o1) && isManagedLabel(o2)) {
                return o1.compare(o2);
            } else if (isManagedLabel(o1)) {
                return -1;
            } else if (isManagedLabel(o2)) {
                return 1;
            }

            return o1.compare(o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
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
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }

        private boolean isManagedLabel(SortedItem item) {
            return (item.viewType == ManageTokensHolder.VIEW_TYPE);
        }
    });

    /*
    protected final SortedList<SortedItem> items = new SortedList<>(SortedItem.class, new SortedList.Callback<SortedItem>() {
        @Override
        public int compare(SortedItem o1, SortedItem o2) {
            //Note: ViewTypes are numbered in order of appearance
            if (isManagedLabel(o1) && isManagedLabel(o2)) {
                return o1.compare(o2);
            } else if (isManagedLabel(o1)) {
                return -1;
            } else if (isManagedLabel(o2)) {
                return 1;
            }

            return o1.compare(o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
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
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }

        private boolean isManagedLabel(SortedItem item) {
            return (item.viewType == ManageTokensHolder.VIEW_TYPE);
        }
    });
*/
    protected TotalBalanceSortedItem total = new TotalBalanceSortedItem(null);

    public GroupTokensAdapter(OnTokenClickListener onTokenClickListener, AssetDefinitionService aService, TokensService tService, Context context) {
        this.onTokenClickListener = onTokenClickListener;
        this.assetService = aService;
        this.tokensService = tService;
        this.context = context;
        this.realm = tokensService.getTickerRealmInstance();
    }

    protected GroupTokensAdapter(OnTokenClickListener onTokenClickListener, AssetDefinitionService aService) {
        this.onTokenClickListener = onTokenClickListener;
        this.assetService = aService;
        this.tokensService = null;
        this.realm = null;
    }

    private void addGroupToken(int viewType, String name, int chainId, String address, int color, int weight) {
        if (walletAddress != null && !walletAddress.isEmpty()) {
            GroupTokenData groupTokenData = new GroupTokenData(address, name, chainId, groupTokenDataListener);
            groupTokenData.setSectionColor(color);
            groupItems.add(new GroupTokensSortedItem(viewType, groupTokenData, weight));
        }
    }

    private GroupTokenData.GroupTokenDataListener groupTokenDataListener = new GroupTokenData.GroupTokenDataListener() {
        @Override
        public void notifyItemRangeChanged(int position, int count) {
            GroupTokensAdapter.this.notifyItemRangeChanged(position, count);
        }

        @Override
        public void notifyItemRangeInserted(int position, int count) {
            GroupTokensAdapter.this.notifyItemRangeInserted(position, count);
        }

        @Override
        public void notifyItemRangeRemoved(int position, int count) {
            GroupTokensAdapter.this.notifyItemRangeRemoved(position, count);
        }

        @Override
        public void notifyItemMoved(int fromPosition, int toPosition) {
            GroupTokensAdapter.this.notifyItemMoved(fromPosition, toPosition);
        }
    };

    @Override
    public long getItemId(int position) {
        Object obj = getItemInGroupItems(position);
        if (obj instanceof TokenSortedItem) {
            TokenCardMeta tcm = ((TokenSortedItem) obj).value;

            // This is an attempt to obtain a 'unique' id
            // to fully utilise the RecyclerView's setHasStableIds feature.
            // This will drastically reduce 'blinking' when the list changes
            return tcm.getUID();
        } else {
            return position;
        }
    }

    private SortedItem getItemInGroupItems(int position) {
        int findingPosition = 0;
        for (int groupIndex = 0; groupIndex < groupItems.size(); groupIndex++) {
            if (findingPosition == position) {
                return groupItems.get(groupIndex);
            }
            if (groupItems.get(groupIndex) instanceof GroupTokensSortedItem) {
                GroupTokensSortedItem groupTokensSortedItem = (GroupTokensSortedItem) groupItems.get(groupIndex);
                findingPosition++;
                for (int i = 0; i < groupTokensSortedItem.value.getItems().size(); i++) {
                    if (findingPosition == position) {
                        return groupTokensSortedItem.value.getItems().get(i);
                    }
                    findingPosition++;
                }
            } else {
                findingPosition++;
            }
        }
        return null;
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TokenHolder.VIEW_TYPE: {
                TokenHolder tokenHolder = new TokenHolder(parent, assetService, tokensService, realm);
                tokenHolder.setOnTokenClickListener(onTokenClickListener);
                holder = tokenHolder;
                break;
            }
            case TokenGridHolder.VIEW_TYPE: {
                TokenGridHolder tokenGridHolder = new TokenGridHolder(R.layout.item_token_grid, parent, assetService, tokensService);
                tokenGridHolder.setOnTokenClickListener(onTokenClickListener);
                holder = tokenGridHolder;
                break;
            }
            case ManageTokensHolder.VIEW_TYPE:
                holder = new ManageTokensHolder(R.layout.layout_manage_tokens, parent);
                break;
            case WarningHolder.VIEW_TYPE:
                holder = new WarningHolder(R.layout.item_warning, parent);
                break;
            case AssetInstanceScriptHolder.VIEW_TYPE:
                holder = new AssetInstanceScriptHolder(R.layout.item_ticket, parent, null, assetService, false);
                break;
            case VIEWTYPE_BASE_ETH_MAINNET_LABEL:
            case VIEWTYPE_BASE_ETH_OTHER_LABEL:
            case VIEWTYPE_BASE_ETH_SYMBLOX_LABEL:
            case VIEWTYPE_BASE_ETH_BINANCE_LABEL:
            case VIEWTYPE_BASE_ETH_HECO_LABEL:
                holder = new GroupTokensHolder(R.layout.item_group_token, parent);
                break;
            default:
                // NB to save ppl a lot of effort this view doesn't show - item_total_balance has height coded to 1dp.
            case TotalBalanceHolder.VIEW_TYPE: {
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
            }
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(BinderViewHolder holder, int position) {
        SortedItem sortedItem = getItemInGroupItems(position);
        sortedItem.view = holder;
        holder.bind(sortedItem.value);
    }

    public void onRViewRecycled(RecyclerView.ViewHolder holder)
    {
        ((BinderViewHolder<?>)holder).onDestroyView();
    }

    @Override
    public int getItemViewType(int position) {
        SortedItem sortedItem = getItemInGroupItems(position);
        return (sortedItem == null) ? 0 : sortedItem.viewType;
    }

    @Override
    public int getItemCount() {
        int totalSize = 0;
        for (int groupIndex = 0; groupIndex < groupItems.size(); groupIndex++) {
            if (groupItems.get(groupIndex) instanceof GroupTokensSortedItem) {
                GroupTokensSortedItem groupTokensSortedItem = (GroupTokensSortedItem) groupItems.get(groupIndex);
                if (groupTokensSortedItem.value.getItems().size() > 0) {
                    totalSize++;
                    totalSize += groupTokensSortedItem.value.getItems().size();
                }
            } else {
                totalSize++;
            }
        }
        return totalSize;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
        if (!isFinishSettingup()) {
            addManageTokensLayout();
            addGroupToken(VIEWTYPE_BASE_ETH_SYMBLOX_LABEL, "VELAS", EthereumNetworkRepository.VELAS_MAINNET_ID, walletAddress, ContextCompat.getColor(context, R.color.section_velas)
                    , 0);
            addGroupToken(VIEWTYPE_BASE_ETH_MAINNET_LABEL, "ETH", EthereumNetworkRepository.MAINNET_ID, walletAddress, ContextCompat.getColor(context, R.color.section_eth), 0);
            addGroupToken(VIEWTYPE_BASE_ETH_BINANCE_LABEL, "BINANCE", EthereumNetworkRepository.BINANCE_MAIN_ID, walletAddress, ContextCompat.getColor(context, R.color.section_binance), 0);
            addGroupToken(VIEWTYPE_BASE_ETH_HECO_LABEL, "HECO", EthereumNetworkRepository.HECO_ID, walletAddress, ContextCompat.getColor(context, R.color.section_heco), 0);
            addGroupToken(VIEWTYPE_BASE_ETH_OTHER_LABEL, "OTHERS", -1, walletAddress, 0, 0);
        }
    }

    private boolean isFinishSettingup() {
        for (int groupIndex = 0; groupIndex < groupItems.size(); groupIndex++) {
            if (groupItems.get(groupIndex) instanceof ManageTokensSortedItem) {
                return true;
            }
        }
        return false;
    }

    private void addManageTokensLayout() {
        if (walletAddress != null && !walletAddress.isEmpty()) {
            groupItems.add(new ManageTokensSortedItem(new ManageTokensData(walletAddress), 0));
        }
    }

    public void addWarning(WarningData data) {
        groupItems.add(new WarningSortedItem(data, 1));
    }

    public void removeBackupWarning() {
        for (int i = 0; i < groupItems.size(); i++) {
            if (groupItems.get(i).viewType == WarningHolder.VIEW_TYPE) {
                groupItems.removeItemAt(i);
                notifyItemRemoved(i);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void setTokens(TokenCardMeta[] tokens) {
        populateTokens(tokens, false);
    }

    /**
     * Update a single item in the recycler view
     *
     * @param token
     */
    public void updateToken(TokenCardMeta token, boolean notify) {
        if (canDisplayToken(token)) {
            //does this token already exist with a different weight (ie name has changed)?
            removeMatchingTokenDifferentWeight(token);
            int position = -1;
            TokenSortedItem tsi;
            if (gridFlag) {
                tsi = new TokenSortedItem(TokenGridHolder.VIEW_TYPE, token, token.nameWeight);
            } else {
                tsi = new TokenSortedItem(TokenHolder.VIEW_TYPE, token, token.nameWeight);
            }
            if (debugView) tsi.debug();
            for (int groupIndex = 0; groupIndex < groupItems.size(); groupIndex++) {
                position++;
                if (groupItems.get(groupIndex) instanceof GroupTokensSortedItem) {
                    GroupTokenData groupTokenData = ((GroupTokenData) groupItems.get(groupIndex).value);
                    if ((groupTokenData.getChainId() == token.getChain()) && isTokenGrouped(groupTokenData)) {
                        position += groupTokenData.getItems().add(tsi);
                        break;
                    } else if (groupTokenData.getChainId() > 0) {
                        position += groupTokenData.getItems().size();
                    } else {
                        position += groupTokenData.getItems().add(tsi);
                    }
                }
            }

            if (notify) notifyItemChanged(position);
        } else {
            removeToken(token);
        }
    }

    private boolean isTokenGrouped(GroupTokenData groupTokenData) {
        return (groupTokenData.getChainId() == EthereumNetworkRepository.MAINNET_ID ||
                groupTokenData.getChainId() == EthereumNetworkRepository.VELAS_MAINNET_ID ||
                groupTokenData.getChainId() == EthereumNetworkRepository.BINANCE_MAIN_ID ||
                groupTokenData.getChainId() == EthereumNetworkRepository.HECO_ID);
    }

    private void removeMatchingTokenDifferentWeight(TokenCardMeta token) {
        int notifyItemAtIndex = 0;
        for (int groupIndex = 0; groupIndex < groupItems.size(); groupIndex++) {
            if (groupItems.get(groupIndex) instanceof GroupTokensSortedItem) {
                notifyItemAtIndex++;
                SortedList<SortedItem> items = ((GroupTokenData) groupItems.get(groupIndex).value).getItems();
                for (int i = 0; i < items.size(); i++) {
                    notifyItemAtIndex++;
                    if (items.get(i) instanceof TokenSortedItem) {
                        TokenSortedItem tsi = (TokenSortedItem) items.get(i);
                        if (tsi.value.equals(token)) {
                            if (tsi.value.nameWeight != token.nameWeight) {
                                notifyItemChanged(notifyItemAtIndex);
                                items.removeItemAt(i);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private TokenCardMeta getToken(int chainId, String tokenAddress) {
        String id = TokensRealmSource.databaseKey(chainId, tokenAddress);
        for (int groupIndex = 0; groupIndex < groupItems.size(); groupIndex++) {
            if (groupItems.get(groupIndex) instanceof GroupTokensSortedItem) {
                SortedList<SortedItem> items = ((GroupTokenData) groupItems.get(groupIndex).value).getItems();
                for (int i = 0; i < items.size(); i++) {
                    Object si = items.get(i);
                    if (si instanceof TokenSortedItem) {
                        TokenSortedItem tsi = (TokenSortedItem) si;
                        if (tsi.value.tokenId.equalsIgnoreCase(id)) {
                            return tsi.value;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void removeToken(TokenCardMeta token) {
        for (int groupIndex = 0; groupIndex < groupItems.size(); groupIndex++) {
            if (groupItems.get(groupIndex) instanceof GroupTokensSortedItem) {
                SortedList<SortedItem> items = ((GroupTokenData) groupItems.get(groupIndex).value).getItems();
                for (int i = 0; i < items.size(); i++) {
                    Object si = items.get(i);
                    if (si instanceof TokenSortedItem) {
                        TokenSortedItem tsi = (TokenSortedItem) si;
                        TokenCardMeta thisToken = tsi.value;
                        if (thisToken.tokenId.equalsIgnoreCase(token.tokenId)) {
                            items.removeItemAt(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void removeToken(int chainId, String tokenAddress) {
        String id = TokensRealmSource.databaseKey(chainId, tokenAddress);
        for (int groupIndex = 0; groupIndex < groupItems.size(); groupIndex++) {
            if (groupItems.get(groupIndex) instanceof GroupTokensSortedItem) {
                SortedList<SortedItem> items = ((GroupTokenData) groupItems.get(groupIndex).value).getItems();
                for (int i = 0; i < items.size(); i++) {
                    Object si = items.get(i);
                    if (si instanceof TokenSortedItem) {
                        TokenSortedItem tsi = (TokenSortedItem) si;
                        TokenCardMeta thisToken = tsi.value;
                        if (thisToken.tokenId.equalsIgnoreCase(id)) {
                            items.removeItemAt(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean canDisplayToken(TokenCardMeta token) {
        if (token == null) return false;
        //Add token to display list if it's the base currency, or if it has balance
        boolean allowThroughFilter = CustomViewSettings.filterToken(token, true, context);
        allowThroughFilter = checkTokenValue(token, allowThroughFilter);
        //for popular tokens, choose if we display or not
        if (allowThroughFilter && tokensService != null) {
            allowThroughFilter = tokensService.shouldDisplayPopularToken(token);
        }

        switch (filterType) {
            case FILTER_ASSETS:
                if (token.isEthereum()) {
                    allowThroughFilter = false;
                }
                break;
            case FILTER_CURRENCY:
                if (!token.isEthereum()) {
                    allowThroughFilter = false;
                }
                break;
            case FILTER_COLLECTIBLES:
                if (!(token.isNFT())) {
                    allowThroughFilter = false;
                }
                break;
            default:
                break;
        }

        return allowThroughFilter;
    }

    // This checks to see if the token is likely malformed
    private boolean checkTokenValue(TokenCardMeta token, boolean allowThroughFilter) {
        return allowThroughFilter && token.nameWeight < Integer.MAX_VALUE;
    }

    private void populateTokens(TokenCardMeta[] tokens, boolean clear) {
        for (int i = 0; i < groupItems.size(); i++) {
            if (groupItems.get(i) instanceof GroupTokensSortedItem) {
                GroupTokensSortedItem groupTokensSortedItem = (GroupTokensSortedItem) groupItems.get(i);
                groupTokensSortedItem.value.getItems().beginBatchedUpdates();
                if (clear) {
                    groupTokensSortedItem.value.getItems().clear();
                }
                for (TokenCardMeta token : tokens) {
                    updateToken(token, false);
                }
                groupTokensSortedItem.value.getItems().endBatchedUpdates();
            }
        }
    }

    public void setTotal(BigDecimal totalInCurrency) {
        total = new TotalBalanceSortedItem(totalInCurrency);
        //see if we need an update
        int notifyItemAtIndex = 0;
        for (int groupIndex = 0; groupIndex < groupItems.size(); groupIndex++) {
            notifyItemAtIndex++;
            if (groupItems.get(groupIndex) instanceof GroupTokensSortedItem) {
                SortedList<SortedItem> items = ((GroupTokenData) groupItems.get(groupIndex).value).getItems();
                items.beginBatchedUpdates();
                for (int i = 0; i < items.size(); i++) {
                    Object si = items.get(i);
                    notifyItemAtIndex++;
                    if (si instanceof TotalBalanceSortedItem) {
                        items.remove((TotalBalanceSortedItem) si);
                        items.add(total);
                        notifyItemChanged(notifyItemAtIndex);
                        break;
                    }
                }
                items.endBatchedUpdates();
            }
        }
    }

    private void filterAdapterItems() {
        //now filter all the tokens accordingly and refresh display
        List<TokenCardMeta> filterTokens = new ArrayList<>();

        for (int i = 0; i < groupItems.size(); i++) {
            if (groupItems.get(i) instanceof GroupTokensSortedItem) {
                GroupTokensSortedItem groupTokensSortedItem = (GroupTokensSortedItem) groupItems.get(i);
                groupTokensSortedItem.value.getItems().beginBatchedUpdates();
                for (int tokenIndex = 0; tokenIndex < groupTokensSortedItem.value.getItems().size(); tokenIndex++) {
                    Object si = groupTokensSortedItem.value.getItems().get(tokenIndex);
                    if (si instanceof TokenSortedItem) {
                        TokenSortedItem tsi = (TokenSortedItem) si;
                        if (canDisplayToken(tsi.value)) {
                            filterTokens.add(tsi.value);
                        }
                    }
                }
                groupTokensSortedItem.value.getItems().endBatchedUpdates();
            }
        }
        populateTokens(filterTokens.toArray(new TokenCardMeta[0]), true);
    }

    public void setFilterType(int filterType) {
        this.filterType = filterType;
        gridFlag = filterType == FILTER_COLLECTIBLES;
        filterAdapterItems();
    }

    public void clear() {
        for (int i = 0; i < groupItems.size(); i++) {
            if (groupItems.get(i) instanceof GroupTokensSortedItem) {
                GroupTokensSortedItem groupTokensSortedItem = (GroupTokensSortedItem) groupItems.get(i);
                groupTokensSortedItem.value.clear();
            }
        }
        notifyDataSetChanged();
    }

    public boolean hasBackupWarning() {
        return groupItems.size() > 0 && groupItems.get(0).viewType == WarningHolder.VIEW_TYPE;
    }

    public void setScrollToken(ContractLocator importToken) {
        scrollToken = importToken;
    }

    public int getScrollPosition() {
        if (scrollToken != null) {
            int findingIndex = 0;
            for (int i = 0; i < groupItems.size(); i++) {
                findingIndex++;
                if (groupItems.get(i) instanceof GroupTokensSortedItem) {
                    GroupTokenData groupTokenData = (GroupTokenData) groupItems.get(i).value;
                    for (int tokenIndex = 0; tokenIndex < groupTokenData.getItems().size(); tokenIndex++) {
                        Object si = groupTokenData.getItems().get(tokenIndex);
                        findingIndex++;
                        if (si instanceof TokenSortedItem) {
                            TokenSortedItem tsi = (TokenSortedItem) si;
                            TokenCardMeta token = tsi.value;
                            if (scrollToken.equals(token)) {
                                scrollToken = null;
                                return findingIndex;
                            }
                        }
                    }
                }
            }
        }

        return -1;
    }

    public void onDestroy(RecyclerView recyclerView)
    {
        //ensure all holders have their realm listeners cleaned up
        for (int childCount = recyclerView.getChildCount(), i = 0; i < childCount; ++i)
        {
            ((BinderViewHolder<?>)recyclerView.getChildViewHolder(recyclerView.getChildAt(i))).onDestroyView();
        }
    }

    public void setDebug()
    {
        debugView = true;
    }
}
