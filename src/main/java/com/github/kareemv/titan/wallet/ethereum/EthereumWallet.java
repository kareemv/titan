package com.github.kareemv.titan.wallet.ethereum;

import com.github.kareemv.titan.Titan;
import com.github.kareemv.titan.util.IOUtils;
import com.github.kareemv.titan.wallet.Wallet;
import com.github.kareemv.titan.wallet.WalletType;
import com.github.kareemv.titan.wallet.exception.BalanceUpdateException;
import com.github.kareemv.titan.wallet.exception.TransactionException;
import com.github.kareemv.titan.wallet.exception.WalletException;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

public class EthereumWallet implements Wallet {
  private String name;
  private final String address;
  private BigDecimal balance; // Represented in ETH
  private final Credentials credentials;
  private static final WalletType walletType = WalletType.ETHEREUM;

  private EthereumWallet(String name, Credentials credentials) {
    this.name = name;
    this.credentials = credentials;
    this.address = credentials.getAddress();
    this.balance = BigDecimal.ZERO;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getAddress() {
    return this.address;
  }

  @Override
  public WalletType getWalletType() {
    return walletType;
  }

  @Override
  public BigDecimal getDisplayBalance() {
    return this.balance.scale() > 6
        ? this.balance.setScale(6, BigDecimal.ROUND_DOWN)
        : this.balance;
  }

  @Override
  public String getDisplayBalanceCurrency() {
    return "ETH";
  }

  @Override
  public BigDecimal getDisplayBalanceUSD() {
    return this.balance.multiply(Titan.INSTANCE.ethUsdPrice);
  }

  @Override
  public void updateBalance() throws BalanceUpdateException {
    try {
      this.balance =
          Convert.fromWei(
              Titan.INSTANCE
                  .ethereumClient
                  .ethGetBalance(address, org.web3j.protocol.core.DefaultBlockParameterName.LATEST)
                  .send()
                  .getBalance()
                  .toString(),
              Convert.Unit.ETHER);
    } catch (Exception e) {
      throw new BalanceUpdateException("Failed to update balance", e);
    }
  }

  @Override
  public String sendFundsTo(String recipientAddress, BigDecimal amount)
      throws TransactionException {
    return sendEthTo(recipientAddress, amount);
  }

  private String sendEthTo(String recipientAddress, BigDecimal amount) throws TransactionException {
    try {
      long chainId = Titan.INSTANCE.ethereumClient.ethChainId().send().getChainId().longValue();
      BigInteger baseFeePerGas =
          Titan.INSTANCE
              .ethereumClient
              .ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
              .send()
              .getBlock()
              .getBaseFeePerGas();
      BigInteger maxPriorityFeePerGas =
          Titan.INSTANCE.ethereumClient.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
      BigInteger maxFeePerGas = baseFeePerGas.add(maxPriorityFeePerGas);
      BigInteger gasLimit = BigInteger.valueOf(21000);

      TransactionReceipt transactionReceipt =
          Transfer.sendFundsEIP1559(
                  Titan.INSTANCE.ethereumClient,
                  credentials,
                  recipientAddress,
                  amount,
                  Convert.Unit.ETHER,
                  gasLimit,
                  maxPriorityFeePerGas,
                  maxFeePerGas)
              .send();

      System.out.println(transactionReceipt.getTransactionHash());

      return transactionReceipt.getTransactionHash();
    } catch (Exception e) {
      e.printStackTrace();
      throw new TransactionException("Failed to send ETH", e);
    }
  }

  public static EthereumWallet createNew(String name) throws WalletException {
    try {
      String fileName =
          WalletUtils.generateNewWalletFile(
              Titan.INSTANCE.password, IOUtils.ETH_WALLETS_DIRECTORY, true);
      boolean renamed =
          new File(IOUtils.ETH_WALLETS_DIRECTORY, fileName)
              .renameTo(new File(IOUtils.ETH_WALLETS_DIRECTORY, name + ".json"));
      if (!renamed) {
        new File(IOUtils.ETH_WALLETS_DIRECTORY, fileName).delete();
        throw new Exception();
      }
      return new EthereumWallet(
          name,
          WalletUtils.loadCredentials(
              Titan.INSTANCE.password,
              IOUtils.ETH_WALLETS_DIRECTORY + File.separator + name + ".json"));
    } catch (Exception e) {
      throw new WalletException("Failed to create new Ethereum wallet: \"" + name + "\"", e);
    }
  }

  public static EthereumWallet createFromPrivateKey(String name, String privateKey)
      throws WalletException {
    try {
      EthereumWallet w = new EthereumWallet(name, Credentials.create(privateKey));
      String fileName =
          WalletUtils.generateWalletFile(
              Titan.INSTANCE.password,
              w.credentials.getEcKeyPair(),
              IOUtils.ETH_WALLETS_DIRECTORY,
              true);
      boolean renamed =
          new File(IOUtils.ETH_WALLETS_DIRECTORY, fileName)
              .renameTo(new File(IOUtils.ETH_WALLETS_DIRECTORY, name + ".json"));
      if (!renamed) {
        new File(IOUtils.ETH_WALLETS_DIRECTORY, fileName).delete();
        throw new Exception();
      }
      w.updateBalance();
      return w;
    } catch (Exception e) {
      throw new WalletException("Failed to import Ethereum wallet: \"" + name + "\"", e);
    }
  }

  public static EthereumWallet loadFromFile(String fileName, String password) throws WalletException {
    try {
      EthereumWallet wallet =
          new EthereumWallet(
              fileName.split("\\.")[0],
              WalletUtils.loadCredentials(
                  password, IOUtils.ETH_WALLETS_DIRECTORY + File.separator + fileName));
      wallet.updateBalance();
      return wallet;
    } catch (Exception e) {
      throw new WalletException("Failed to load Ethereum wallet file: \"" + fileName + "\"", e);
    }
  }
}
