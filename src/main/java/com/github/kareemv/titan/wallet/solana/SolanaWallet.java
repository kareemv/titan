package com.github.kareemv.titan.wallet.solana;

import com.github.kareemv.titan.Titan;
import com.github.kareemv.titan.util.IOUtils;
import com.github.kareemv.titan.util.UnitUtils;
import com.github.kareemv.titan.wallet.Wallet;
import com.github.kareemv.titan.wallet.WalletType;
import com.github.kareemv.titan.wallet.exception.BalanceUpdateException;
import com.github.kareemv.titan.wallet.exception.TransactionException;
import com.github.kareemv.titan.wallet.exception.WalletException;
import com.github.kareemv.titan.wallet.solana.encryption.SolanaWalletEncryptor;
import com.github.kareemv.titan.wallet.token.SplTokenBalance;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.sol4k.Keypair;
import org.sol4k.PublicKey;
import org.sol4k.Transaction;
import org.sol4k.instruction.TransferInstruction;

public class SolanaWallet implements Wallet {
  private String name;
  private final String address;
  private BigInteger balance; // Represented in lamports
  private final Keypair keypair;
  private static final WalletType walletType = WalletType.SOLANA;
  private List<SplTokenBalance> splTokenBalances = new ArrayList<>();

  private SolanaWallet(String name, Keypair keypair) {
    this.name = name;
    this.keypair = keypair;
    this.address = keypair.getPublicKey().toString();
    this.balance = BigInteger.ZERO;
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
    return UnitUtils.convertLamportsToSol(this.balance);
  }

  @Override
  public String getDisplayBalanceCurrency() {
    return "SOL";
  }

  public List<SplTokenBalance> getSplTokenBalances() {
    return this.splTokenBalances;
  }

  public void updateBalance() throws BalanceUpdateException {
    try {
      this.balance = Titan.INSTANCE.solanaClient.getBalance(this.keypair.getPublicKey());
    } catch (Exception e) {
      throw new BalanceUpdateException("Failed to update balance", e);
    }
  }

  public void updateSplTokenBalances() throws BalanceUpdateException {
    

  }

  public String sendSolTo(String recipientAddress, BigDecimal amount) throws TransactionException {
    try {
      PublicKey senderPublicKey = this.keypair.getPublicKey();
      TransferInstruction transferInstruction =
          new TransferInstruction(
              senderPublicKey,
              new PublicKey(recipientAddress),
              UnitUtils.convertSolToLamports(amount));
      Transaction transaction =
          new Transaction(
              Titan.INSTANCE.solanaClient.getLatestBlockhash(),
              transferInstruction,
              senderPublicKey);
      transaction.sign(this.keypair);
      return Titan.INSTANCE.solanaClient.sendTransaction(transaction);
    } catch (Exception e) {
      throw new TransactionException("Failed to send SOL", e);
    }
  }

  public static SolanaWallet createNew(String name) throws WalletException {
    try {
      Keypair keypair = Keypair.generate();
      String fileName = IOUtils.SOL_WALLETS_DIRECTORY + File.separator + name + ".json";
      SolanaWalletEncryptor.encryptWalletToFile(keypair, Titan.INSTANCE.password, fileName);
      return new SolanaWallet(name, keypair);
    } catch (Exception e) {
      throw new WalletException("Failed to create new Ethereum wallet: \"" + name + "\"", e);
    }
  }

  public static SolanaWallet createFromPrivateKey(String name, String privateKey) throws Exception {
    try {
      Keypair keypair = Keypair.fromSecretKey(privateKey.getBytes());
      String fileName = IOUtils.SOL_WALLETS_DIRECTORY + File.separator + name + ".json";
      SolanaWalletEncryptor.encryptWalletToFile(keypair, Titan.INSTANCE.password, fileName);
      return new SolanaWallet(name, keypair);
    } catch (Exception e) {
      throw new WalletException("Failed to import Solana wallet: \"" + name + "\"", e);
    }
  }

  public static SolanaWallet loadFromFile(String fileName) throws WalletException {
    try {
      return new SolanaWallet(
          fileName.split("\\.")[0],
          SolanaWalletEncryptor.decryptWalletFromFile(
              Titan.INSTANCE.password, IOUtils.SOL_WALLETS_DIRECTORY + File.separator + fileName));
    } catch (Exception e) {
      throw new WalletException("Failed to load Solana wallet file: \"" + fileName + "\"", e);
    }
  }
}
