package com.alphawallet.app.entity;

import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.util.VelasUtils;
import com.alphawallet.token.tools.Numeric;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

/**
 * Created by JB on 21/10/2020.
 */
public class EtherscanEvent
{
    public String blockNumber;
    public long timeStamp;
    public String hash;
    public int nonce;
    String blockHash;
    private String from;
    private String contractAddress;
    private String to;
    String tokenID;
    public String value;
    public String tokenName;
    public String tokenSymbol;
    public String tokenDecimal;
    String gas;
    String gasPrice;
    String gasUsed;

    public String getFrom() {
        return VelasUtils.vlxToEth(from);
    }

    public String getTo() {
        return VelasUtils.vlxToEth(to);
    }

    public String getContractAddress() {
        return VelasUtils.vlxToEth(contractAddress);
    }

    public Transaction createTransaction(@NotNull NetworkInfo networkInfo)
    {
        BigInteger valueBI = BigInteger.ZERO;
        if (value != null && value.length() > 0 && Character.isDigit(value.charAt(0)))
        {
            valueBI = new BigInteger(value);
        }

        String input = Numeric.toHexString(TokenRepository.createTokenTransferData(getTo(), valueBI)); //write the input to the transaction to ensure this is correctly handled elsewhere in the wallet

        return new Transaction(hash, "0", blockNumber, timeStamp, nonce, getFrom(), getContractAddress(), "0", gas, gasPrice, input,
                gasUsed, networkInfo.chainId, false);
    }

    public Transaction createNFTTransaction(@NotNull NetworkInfo networkInfo)
    {
        String input = Numeric.toHexString(TokenRepository.createERC721TransferFunction(getFrom(), getTo(), getContractAddress(), BigInteger.ONE)); //write the input to the transaction to ensure this is correctly handled elsewhere in the wallet

        return new Transaction(hash, "0", blockNumber, timeStamp, nonce, getFrom(), getContractAddress(), "0", gas, gasPrice, input,
                gasUsed, networkInfo.chainId, false);
    }
}
