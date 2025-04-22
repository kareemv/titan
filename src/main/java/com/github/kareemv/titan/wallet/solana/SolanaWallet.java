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
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
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
    BigDecimal solBalance = UnitUtils.convertLamportsToSol(this.balance);
    return solBalance.scale() > 6 ? solBalance.setScale(6, BigDecimal.ROUND_DOWN) : solBalance;
  }

  @Override
  public String getDisplayBalanceCurrency() {
    return "SOL";
  }

  @Override
  public BigDecimal getDisplayBalanceUSD() {
    BigDecimal solBalance = UnitUtils.convertLamportsToSol(this.balance);
    return solBalance.multiply(Titan.INSTANCE.solUsdPrice);
  }

  @Override
  public void updateBalance() throws BalanceUpdateException {
    try {
      this.balance = Titan.INSTANCE.solanaClient.getBalance(this.keypair.getPublicKey());
    } catch (Exception e) {
      throw new BalanceUpdateException("Failed to update balance", e);
    }
  }

  @Override
  public String sendFundsTo(String recipientAddress, BigDecimal amount)
      throws TransactionException {
    return sendSolTo(recipientAddress, amount);
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
      SolanaWallet wallet = new SolanaWallet(name, keypair);
      wallet.updateBalance();
      return wallet;
    } catch (Exception e) {
      throw new WalletException("Failed to import Solana wallet: \"" + name + "\"", e);
    }
  }

  public static SolanaWallet loadFromFile(String fileName, String password) throws WalletException {
    try {
      SolanaWallet wallet =
          new SolanaWallet(
              fileName.split("\\.")[0],
              SolanaWalletEncryptor.decryptWalletFromFile(
                  password, IOUtils.SOL_WALLETS_DIRECTORY + File.separator + fileName));
      wallet.updateBalance();
      return wallet;
    } catch (Exception e) {
      throw new WalletException("Failed to load Solana wallet file: \"" + fileName + "\"", e);
    }
  }
}
