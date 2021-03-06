package com.alphawallet.app.entity.tokens;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.TicketRangeElement;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.VelasUtils;
import com.alphawallet.app.viewmodel.BaseViewModel;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.tools.Convert;

import org.web3j.abi.datatypes.Function;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.alphawallet.app.repository.EthereumNetworkBase.hasRealValue;

public class Token implements Parcelable, Comparable<Token>
{
    public final static int TOKEN_BALANCE_PRECISION = 4;
    private final static int TOKEN_BALANCE_FOCUS_PRECISION = 5;

    public final TokenInfo tokenInfo;
    public BigDecimal balance;
    public BigDecimal pendingBalance;
    public long updateBlancaTime;
    private String tokenWallet;
    protected ContractType contractType;
    public long lastBlockCheck = 0;
    private final String shortNetworkName;
    public boolean balanceChanged;
    public boolean walletUIUpdateRequired;
    public boolean hasTokenScript;
    public long lastTxCheck;
    public long txSync;
    public long lastTxTime;
    private int nameWeight;
    public int itemViewHeight;

    private final Map<BigInteger, Map<String, TokenScriptResult.Attribute>> resultMap = new ConcurrentHashMap<>(); //Build result map for function parse, per tokenId
    private Map<BigInteger, List<String>> functionAvailabilityMap = null;

    public String getNetworkName() { return shortNetworkName; }

    public Token(TokenInfo tokenInfo, BigDecimal balance, long updateBlancaTime, String networkName, ContractType type) {
        this.tokenInfo = tokenInfo;
        if (balance == null)
        {
            balance = BigDecimal.ZERO;
        }
        this.balance = balance;
        this.updateBlancaTime = updateBlancaTime;
        this.shortNetworkName = networkName;
        this.contractType = type;
        this.pendingBalance = balance;
        this.txSync = 0;
        this.lastTxCheck = 0;
        this.lastBlockCheck = 0;
        this.lastTxTime = 0;
        balanceChanged = false;
        walletUIUpdateRequired = false;
        hasTokenScript = false;
        resultMap.clear();
    }

    protected Token(Parcel in) {
        tokenInfo = in.readParcelable(TokenInfo.class.getClassLoader());
        balance = new BigDecimal(in.readString());
        updateBlancaTime = in.readLong();
        int readType = in.readInt();
        shortNetworkName = in.readString();
        pendingBalance = new BigDecimal(in.readString());
        tokenWallet = in.readString();
        lastBlockCheck = in.readLong();
        lastTxCheck = in.readLong();
        txSync = in.readLong();
        lastTxTime = in.readLong();
        hasTokenScript = in.readByte() == 1;
        functionAvailabilityMap = in.readHashMap(List.class.getClassLoader());

        balanceChanged = false;
        if (readType <= ContractType.CREATION.ordinal())
        {
            contractType = ContractType.values()[readType];
        }
    }

    public String getStringBalance()
    {
        String value;
        BigDecimal ethBalance = getCorrectedBalance(18);
        if (ethBalance.equals(BigDecimal.ZERO)) //zero balance
        {
            value = "0";
        }
        else if (ethBalance.compareTo(BigDecimal.valueOf(0.000001)) < 0) //very low balance
        {
            value = "~0.00";
        }
        else if (balance.compareTo(Convert.toWei(BigDecimal.TEN, Convert.Unit.GETHER)) > 0) //too big
        {
            NumberFormat sci_formate = new DecimalFormat("0.#####E0");
            value = sci_formate.format(ethBalance);
            value = value.replace("E", "e+");
        }
        else //otherwise display in standard pattern to 4 dp
        {
            DecimalFormat df = new DecimalFormat("###,###,###,##0.####");
            df.setRoundingMode(RoundingMode.DOWN);
            value = df.format(ethBalance);
        }

        return value;
    }

    public boolean hasPositiveBalance() {
        if (balance != null) return !balance.equals(BigDecimal.ZERO);
        else return false;
    }

    public boolean independentUpdate()
    {
        return false;
    }

    public String getFullBalance() {
        if (balance != null) return balance.toString();
        else return "0";
    }

    public Asset getAssetForToken(String tokenId) {
        return null;
    }
    public List<BigInteger> getUniqueTokenIds()
    {
        List<BigInteger> uniqueIds = new ArrayList<>();
        if (isNonFungible())
        {
            for (BigInteger id : getArrayBalance())
            {
                if (!uniqueIds.contains(id) && !id.equals(BigInteger.ZERO)) uniqueIds.add(id);
            }
        }
        else
        {
            uniqueIds.add(BigInteger.ZERO);
        }

        return uniqueIds;
    }

    public void addAssetToTokenBalanceAssets(Asset asset) {
        //only for ERC721, see override in ERC721Token
    }

    public static final Creator<Token> CREATOR = new Creator<Token>() {
        @Override
        public Token createFromParcel(Parcel in) {
            return new Token(in);
        }

        @Override
        public Token[] newArray(int size) {
            return new Token[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(tokenInfo, flags);
        dest.writeString(balance == null ? "0" : balance.toString());
        dest.writeLong(updateBlancaTime);
        dest.writeInt(contractType.ordinal());
        dest.writeString(shortNetworkName);
        dest.writeString(pendingBalance == null ? "0" : pendingBalance.toString());
        dest.writeString(tokenWallet);
        dest.writeLong(lastBlockCheck);
        dest.writeLong(lastTxCheck);
        dest.writeLong(txSync);
        dest.writeLong(lastTxTime);
        dest.writeByte(hasTokenScript?(byte)1:(byte)0);
        dest.writeMap(functionAvailabilityMap);
    }

    public void setRealmBalance(RealmToken realmToken)
    {
        if (balance != null)
        {
            realmToken.setBalance(balance.toString());
        }
        else
        {
            realmToken.setBalance("0");
        }
    }

    public void setIsTerminated(RealmToken realmToken)
    {
        realmToken.setUpdateTime(-1);
        updateBlancaTime = -1;
    }
    public boolean isTerminated() { return (updateBlancaTime == -1); }

    public String getAddress() {
        return tokenInfo.address;
    }

    public String getFullName()
    {
        if (isTerminated()) return TokensService.EXPIRED_CONTRACT;
        if (isBad()) return TokensService.UNKNOWN_CONTRACT;
        String name = tokenInfo.getName() == null ? "" : tokenInfo.getName();
        String symbol = (tokenInfo.symbol == null || tokenInfo.symbol.length() == 0) ? "" : " (" + tokenInfo.symbol.toUpperCase() + ")";
        return name + symbol;
    }

    public String getFullName(AssetDefinitionService assetDefinition, int count)
    {
        //override contract name with TS defined name
        String name = assetDefinition != null ? assetDefinition.getTokenName(tokenInfo.chainId, tokenInfo.address, count) : null;
        if (name != null) {
            String symbol = (tokenInfo.symbol == null || tokenInfo.symbol.length() == 0) ? "" : " (" + tokenInfo.symbol.toUpperCase() + ")";
            return sanitiseString(name + symbol);
        } else {
            return sanitiseString(getFullName());
        }
    }

    private String sanitiseString(String str)
    {
        StringBuilder sb = new StringBuilder();
        for (char ch : str.toCharArray())
        {
            if (Character.isAlphabetic(ch) || Character.isDigit(ch) || Character.isIdeographic(ch) || ch < 65530)
            {
                sb.append(ch);
            }
            else
            {
                sb.append("*");
            }
        }

        return sb.toString();
    }

    public String getSymbol()
    {
        if (tokenInfo.symbol == null) return "";
        else return tokenInfo.symbol.toUpperCase();
    }

    public void clickReact(BaseViewModel viewModel, Context context)
    {
        viewModel.showErc20TokenDetail(context, tokenInfo.address, tokenInfo.symbol, tokenInfo.decimals, this);
    }

    public BigDecimal getCorrectedBalance(int scale)
    {
        if (balance == null || balance.equals(BigDecimal.ZERO)) return BigDecimal.ZERO;
        int decimals = 18;
        if (tokenInfo != null) decimals = tokenInfo.decimals;
        BigDecimal decimalDivisor = BigDecimal.valueOf(Math.pow(10, decimals));
        return decimals > 0
               ? balance.divide(decimalDivisor, scale, RoundingMode.DOWN).stripTrailingZeros() : balance;
    }

    public int getContractType()
    {
        switch (contractType)
        {
            case ERC20:
                return R.string.erc20;
            case ETHEREUM:
                return 0; //don't display 'ethereum' as contract type
            default:
                return 0;
        }
    }

    public List<Asset> getTokenAssets() {
        return null;
    }

    public List<BigInteger> ticketIdStringToIndexList(String userList)
    {
        return null;
    }

    /**
     * Produce a string CSV of integer IDs given an input list of values
     * @param idList
     * @param keepZeros
     * @return
     */
    public static String bigIntListToString(List<BigInteger> idList, boolean keepZeros)
    {
        if (idList == null) return "";
        String displayIDs = "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (BigInteger id : idList)
        {
            if (!keepZeros && id.compareTo(BigInteger.ZERO) == 0) continue;
            if (!first)
            {
                sb.append(",");
            }
            first = false;

            sb.append(Numeric.toHexStringNoPrefix(id));
            displayIDs = sb.toString();
        }

        return displayIDs;
    }

    public List<Integer> stringIntsToIntegerList(String userList)
    {
        List<Integer> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                Integer intId = Integer.parseInt(trim);
                idList.add(intId);
            }
        }
        catch (Exception e)
        {
            idList = new ArrayList<>();
        }

        return idList;
    }

    public String integerListToString(List<Integer> intList, boolean keepZeros)
    {
        if (intList == null) return "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Integer id : intList)
        {
            if (!keepZeros && id == 0) continue;
            if (!first)sb.append(",");
            sb.append(String.valueOf(id));
            first = false;
        }

        return sb.toString();
    }

    public int getTicketCount()
    {
        return balance.intValue();
    }

    public boolean addressMatches(String contractAddress)
    {
        String checkAddress = Numeric.cleanHexPrefix(contractAddress);
        String ourAddress = Numeric.cleanHexPrefix(getAddress());
        return ourAddress.equalsIgnoreCase(checkAddress);
    }

    public boolean isToken()
    {
        return (contractType != ContractType.ETHEREUM);
    }

    public boolean checkRealmBalanceChange(RealmToken realmToken)
    {
        if (contractType == null || contractType.ordinal() != realmToken.getInterfaceSpec()) return true;
        String currentState = realmToken.getBalance();
        if (currentState == null) return true;
        if (tokenInfo.getName() != null && realmToken.getName() == null) return true; //signal to update database if correct name has been fetched (node timeout etc)
        if (tokenInfo.getName() == null && realmToken.getName() != null) return true;
        if (tokenInfo.symbol == null && realmToken.getSymbol() != null) return true;
        if (tokenInfo.getName() != null && realmToken.getName() != null) return true;
        if (tokenInfo.symbol != null && realmToken.getSymbol() == null) return true;
        if (tokenInfo.getName() != null && (!tokenInfo.getName().equals(realmToken.getName()) || !tokenInfo.symbol.equals(realmToken.getSymbol()))) return true;
        String currentBalance = getFullBalance();
        return !currentState.equals(currentBalance);
    }

    private Map<String, String> restoreAuxData(String data)
    {
        Map<String, String> aux = null;
        if (data != null && data.length() > 0)
        {
            String[] set = data.split(",");
            aux = new ConcurrentHashMap<>();
            for (int i = 0; i < (set.length - 1); i+=2)
            {
                aux.put(set[i], set[i+1]);
            }
        }

        return aux;
    }

    public void setIsEthereum()
    {
        contractType = ContractType.ETHEREUM;
    }

    public boolean isBad()
    {
        return tokenInfo == null || (tokenInfo.symbol == null && tokenInfo.getName() == null);
    }

    public boolean checkTokenWallet(String address)
    {
        return tokenWallet != null && tokenWallet.equalsIgnoreCase(address);
    }

    public void setTokenWallet(String address)
    {
        this.tokenWallet = address;
    }

    public void setupRealmToken(RealmToken realmToken)
    {
        lastBlockCheck = realmToken.getLastBlock();
        txSync = realmToken.getTXUpdateTime();
        lastTxCheck = realmToken.getLastTxTime();
        lastTxTime = realmToken.getLastTxTime();
        tokenInfo.isEnabled = realmToken.getEnabled();
    }

    public boolean checkBalanceChange(Token token)
    {
        if (token != null && tokenInfo.decimals != token.tokenInfo.decimals) return true;
        else return token != null && (!getFullBalance().equals(token.getFullBalance()) || !getFullName().equals(token.getFullName()));
    }

    public String getPendingDiff()
    {
        if (!isEthereum() || pendingBalance == null || balance.equals(pendingBalance)) return null;
        else
        {
            String prefix = "";
            BigDecimal diff = pendingBalance.subtract(balance);
            if (diff.compareTo(BigDecimal.ZERO) > 0) prefix = "+";
            String diffStr = prefix + BalanceUtils.getScaledValue(diff, tokenInfo.decimals, TOKEN_BALANCE_PRECISION);
            if (diffStr.startsWith("~")) diffStr = null;
            return diffStr;
        }
    }

    public void setRealmInterfaceSpec(RealmToken realmToken)
    {
        if (isEthereum()) contractType = ContractType.ETHEREUM;
        realmToken.setInterfaceSpec(contractType.ordinal());
    }

    public void setInterfaceSpecFromRealm(RealmToken realm)
    {
        if (realm.getInterfaceSpec() > ContractType.CREATION.ordinal())
        {
            //need to re-sync this contract
            this.contractType = ContractType.NOT_SET;
        }
        else
        {
            this.contractType = ContractType.values()[realm.getInterfaceSpec()];
        }
    }

    public void setRealmLastBlock(RealmToken realmToken)
    {
        realmToken.setLastBlock(lastBlockCheck);
        realmToken.setLastTxTime(lastTxTime);
    }

    /**
     * Stub functions - these are intended to be overridden in inherited classes.
     */
    public void setInterfaceSpec(ContractType type)
    {
        contractType = type;
    }
    public ContractType getInterfaceSpec() { return contractType; }
    public int interfaceOrdinal()
    {
        return 0;
    }
    public BigInteger getTokenID(int index)
    {
        return BigInteger.valueOf(-1);
    }
    public Function getTransferFunction(String to, List<BigInteger> transferData) throws NumberFormatException
    {
        return null;
    }
    public Function getSpawnPassToFunction(BigInteger expiry, List<BigInteger> tokenIds, int v, byte[] r, byte[] s, String recipient)
    {
        return new Function(
                "spawnPassTo",
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(expiry),
                              getDynArray(tokenIds),
                              new org.web3j.abi.datatypes.generated.Uint8(v),
                              new org.web3j.abi.datatypes.generated.Bytes32(r),
                              new org.web3j.abi.datatypes.generated.Bytes32(s),
                              new org.web3j.abi.datatypes.Address(recipient)),
                Collections.emptyList());
    }
    public Function getTradeFunction(BigInteger expiry, List<BigInteger> tokenIds, int v, byte[] r, byte[] s)
    {
        return new Function(
                "trade",
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint256(expiry),
                              getDynArray(tokenIds),
                              new org.web3j.abi.datatypes.generated.Uint8(v),
                              new org.web3j.abi.datatypes.generated.Bytes32(r),
                              new org.web3j.abi.datatypes.generated.Bytes32(s)),
                Collections.emptyList());
    }

    public void checkIsMatchedInXML(AssetDefinitionService assetService) { }
    public int[] getTicketIndices(String ticketIds) { return new int[0]; }
    public boolean contractTypeValid() { return !(contractType == ContractType.NOT_SET || contractType == ContractType.OTHER); }
    public List<BigInteger> getArrayBalance() { return new ArrayList<>(); }
    public List<BigInteger> getNonZeroArrayBalance() { return new ArrayList<>(Arrays.asList(BigInteger.ZERO)); }
    public boolean isMatchedInXML() { return false; }

    public String getOperationName(Transaction transaction, Context ctx)
    {
        String name;
        if (isEthereum())
        {
            if (isMyWallet(transaction.from))
            {
                name = ctx.getString(R.string.sent);
            }
            else
            {
                name = ctx.getString(R.string.received);
            }
        }
        else
        {
            name = transaction.getOperationName(ctx);
        }


        return name;
    }

    private boolean isMyWallet(String from) {
        return (from.equalsIgnoreCase(tokenWallet) || from.equalsIgnoreCase(VelasUtils.ethToVlx(tokenWallet)));
    }

    /* Raw string value for balance */
    public String getScaledBalance()
    {
        return balance.divide(new BigDecimal(Math.pow(10, tokenInfo.decimals)), 18, RoundingMode.DOWN).toString();
    }

    /**
     * Balance in human readable form, variable decimal width - will only display decimals if present (eg 14.5, 20, 32.5432)
     * @return formatted balance
     */
    public String getFormattedBalance()
    {
        return BalanceUtils.getScaledValue(balance, tokenInfo.decimals, TOKEN_BALANCE_PRECISION);
    }

    /**
     * Balance in human readable form, fixed decimal width eg 14.5000
     * @return formatted balance
     */
    public String getFixedFormattedBalance()
    {
        return BalanceUtils.getScaledValueFixed(balance, tokenInfo.decimals, TOKEN_BALANCE_PRECISION);
    }

    /**
     * Convert a CSV string of Hex values into a BigInteger List
     * @param integerString CSV string of hex ticket id's
     * @return
     */
    public List<BigInteger> stringHexToBigIntegerList(String integerString)
    {
        List<BigInteger> idList = new ArrayList<>();

        try
        {
            String[] ids = integerString.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                BigInteger val = Numeric.toBigInt(trim);
                idList.add(val);
            }
        }
        catch (Exception e)
        {
            idList = new ArrayList<>();
        }

        return idList;
    }

    /**
     * Fetch the base native value attached to this transaction
     * @param transaction
     * @return
     */
    public String getTransactionValue(Transaction transaction, int precision)
    {
        if (transaction.hasError()) return "";
        else if (transaction.value.equals("0")) return "0";
        return transaction.getPrefix(this) + BalanceUtils.getScaledValueFixed(new BigDecimal(transaction.value), tokenInfo.decimals, precision);
    }

    /**
     * Fetch the Token value of this transaction eg +20 DAI, or if a native chain transaction then return the value
     *
     * @param transaction
     * @return
     */
    public String getTransactionResultValue(Transaction transaction, int precision)
    {
        String txResultValue = isEthereum() ? getTransactionValue(transaction, precision) : transaction.getOperationResult(this, precision);
        return txResultValue + " " + getSymbol();
    }

    public boolean hasArrayBalance()
    {
        return false;
    }

    public String getTokenName(AssetDefinitionService assetService, int count)
    {
        //see if this token is covered by any contract
        if (assetService.hasDefinition(tokenInfo.chainId, tokenInfo.address))
        {
            if (tokenInfo.getName() != null) return tokenInfo.getName();
            else return assetService.getAssetDefinition(tokenInfo.chainId, getAddress()).getTokenName(count);
        }
        else
        {
            return tokenInfo.getName();
        }
    }

    public boolean hasRealValue()
    {
        return EthereumNetworkRepository.hasRealValue(tokenInfo.chainId);
    }

    public boolean getIsSent(Transaction transaction)
    {
        return isMyWallet(transaction.from);
    }

    public String getWallet()
    {
        return tokenWallet;
    }

    public List<BigInteger> pruneIDList(String ticketIds, int quantity)
    {
        return new ArrayList<>();
    }

    public boolean equals(Token token)
    {
        return token != null && tokenInfo.chainId == token.tokenInfo.chainId && getAddress().equals(token.getAddress());
    }

    public String getTokenTitle()
    {
        return tokenInfo.getName();
    }

    public boolean isERC875() { return false; }
    public boolean isERC721() { return false; }
    public boolean isNonFungible() { return false; }
    public boolean isERC20()
    {
        return contractType == ContractType.ERC20;
    }
    public boolean isEthereum()
    {
        return contractType == ContractType.ETHEREUM;
    }
    public boolean isERC721Ticket() { return false; }
    public boolean hasGroupedTransfer() { return false; } //Can the NFT token's transfer function handle multiple tokens?
    public boolean checkSelectionValidity(List<BigInteger> selection) //check a selection of ID's for Transfer/Redeem/Sell
    {
        return selection.size() != 0 && (selection.size() == 1 || hasGroupedTransfer());
    }


    public BigDecimal getCorrectedAmount(String newAmount)
    {
        if (newAmount == null || newAmount.length() == 0) return BigDecimal.ZERO;

        try
        {
            BigDecimal bd = new BigDecimal(newAmount);
            BigDecimal factor = new BigDecimal(Math.pow(10, tokenInfo.decimals));
            return bd.multiply(factor).setScale(0, RoundingMode.DOWN).stripTrailingZeros();
        }
        catch (Exception e)
        {
            //
        }
        return BigDecimal.ZERO;
    }

    public String getShortName() {
        if (isTerminated() || isBad()) return "";
        return tokenInfo.getName() != null ? tokenInfo.getName() : tokenInfo.symbol != null ? tokenInfo.symbol : "";
    }

    public boolean groupWithToken(TicketRange currentRange, TicketRangeElement e, long currentTime)
    {
        //default is no grouping
        return false;
    }

    /**
     * This function takes a list of tokenIds, and returns a BigInteger list suitable for this token's transfer function
     * For most token contracts this is the list of tokenIds but for ERC875 this is the list converted to indices
     * @param tokenIds
     * @return
     */
    public List<BigInteger> getTransferListFormat(List<BigInteger> tokenIds)
    {
        return tokenIds;
    }
    protected org.web3j.abi.datatypes.DynamicArray getDynArray(List<BigInteger> indices)
    {
        return new org.web3j.abi.datatypes.DynamicArray<>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(indices, org.web3j.abi.datatypes.generated.Uint256.class));
    }

    @Override
    public boolean equals(Object v)
    {
        boolean retVal = false;

        if (v instanceof Token) {
            Token t = (Token) v;
            retVal = t != null && tokenInfo.chainId == t.tokenInfo.chainId && getAddress().equals(t.getAddress());
        }

        return retVal;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 17 * hash + (this.tokenInfo.getName() != null ? this.tokenInfo.getName().hashCode() : 0);
        return hash;
    }

    @Override
    public int compareTo(@NonNull Token otherToken)
    {
        return nameWeight - otherToken.nameWeight;
    }

    public long getUID()
    {
        String id = getAddress() + "-" + tokenInfo.chainId;
        return id.hashCode();
    }

    public BigDecimal getTxValue(Transaction transaction)
    {
        BigDecimal returnValue = BigDecimal.ZERO;
        try
        {
            if (isEthereum())
            {
                returnValue = new BigDecimal(transaction.value);
            }
            else
            {
                returnValue = transaction.getRawValue();
            }
        }
        catch (Exception e)
        {
            //
        }

        return returnValue;
    }

    public boolean checkBalanceType()
    {
        return true;
    }

    public String getTransactionDestination(Transaction transaction)
    {
        if (isEthereum()) return transaction.to;
        else return transaction.getContract(this);
    }

    public StatusType ethereumTxImage(Transaction tx)
    {
        return isMyWallet(tx.from) ? (tx.to.equals(tx.from) ? StatusType.SELF : StatusType.SENT)
                : StatusType.RECEIVE;
    }

//    public int getTxImage(Transaction transaction)
//    {
//        int asset;
//        if (isEthereum())
//        {
//            asset = ethereumTxImage(transaction);
//        }
//        else
//        {
//            asset = transaction.getOperationImage(this);
//        }
//
//        return asset;
//    }

    public StatusType getTxStatus(Transaction transaction)
    {
        StatusType status = transaction.getTransactionStatus();
        if (status == null)
        {
            if (isEthereum())
            {
                status = ethereumTxImage(transaction);
            }
            else
            {
                status = transaction.getOperationImage(this);
            }
        }

        return status;
    }

    public TokenScriptResult.Attribute getAttributeResult(String attrId, BigInteger tokenId)
    {
        if (resultMap.containsKey(tokenId))
        {
            return resultMap.get(tokenId).get(attrId);
        }
        else
        {
            return null;
        }
    }

    public void setAttributeResult(BigInteger tokenId, TokenScriptResult.Attribute attrResult)
    {
        Map<String, TokenScriptResult.Attribute> resultSet = resultMap.get(tokenId);
        if (resultSet == null)
        {
            resultSet = new ConcurrentHashMap<>();
            resultMap.put(tokenId, resultSet);
        }

        resultSet.put(attrResult.id, attrResult);
    }

    public void clearResultMap()
    {
        resultMap.clear();
    }

    public void setFunctionAvailability(Map<BigInteger, List<String>> availabilityMap)
    {
        functionAvailabilityMap = availabilityMap;
    }

    public boolean isFunctionAvailable(BigInteger tokenId, String functionName)
    {
        List<String> mapForToken = functionAvailabilityMap.get(tokenId);
        if (mapForToken != null)
        {
            return mapForToken.contains(functionName);
        }
        else
        {
            return false;
        }
    }

    public void setNameWeight(int weight)
    {
        nameWeight = weight;
    }

    public long getTransactionCheckInterval()
    {
        if (hasRealValue() && hasPositiveBalance())
        {
            return 1* DateUtils.MINUTE_IN_MILLIS;
        }
        else if (hasPositiveBalance())
        {
            return 150* DateUtils.SECOND_IN_MILLIS;
        }
        else
        {
            return 0;
        }
    }
}