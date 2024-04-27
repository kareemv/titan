package com.github.kareemv.titan.wallet.solana;

import com.github.kareemv.titan.Titan;
import com.github.kareemv.titan.util.IOUtils;
import com.github.kareemv.titan.util.UnitUtils;
import com.github.kareemv.titan.wallet.Wallet;
import com.github.kareemv.titan.wallet.WalletType;
import com.github.kareemv.titan.wallet.ethereum.EthereumWallet;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.core.Transaction;
import org.p2p.solanaj.programs.SystemProgram;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.math.BigDecimal;

public class SolanaWallet implements Wallet {
    private String name;
    private String address;
    private long balance; // Represented in lamports
    private Account account;
    private final WalletType walletType = WalletType.SOLANA;

    private SolanaWallet(String name, Account account) {
        this.name = name;
        this.account = account;
        this.address = account.getPublicKey().toString();
        this.balance = 0L;
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
        return this.walletType;
    }

    @Override
    public BigDecimal getDisplayBalance() {
        return null;
    }

    public void updateBalance() throws Exception {
        this.balance = Titan.INSTANCE.solanaClient.getApi().getBalance(this.account.getPublicKey());
    }

    public String sendSolTo(String recipientAddress, BigDecimal amount) throws Exception {
        Transaction transaction = new Transaction();
        transaction.addInstruction(SystemProgram.transfer(this.account.getPublicKey(), new PublicKey(recipientAddress), UnitUtils.convertSolToLamports(amount)));
        return Titan.INSTANCE.solanaClient.getApi().sendTransaction(transaction, this.account);
    }

    public static SolanaWallet createNew(String name) throws Exception {
        // TODO: Implement encryption and save-to-file functionality for Solana wallets.
        return null;
    }

    public static SolanaWallet createFromPrivateKey(String name, String privateKey) throws Exception {
        // TODO: Implement encryption and save-to-file functionality for Solana wallets.
        return null;
    }
}
