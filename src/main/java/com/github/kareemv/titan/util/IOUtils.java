package com.github.kareemv.titan.util;

import java.io.File;

public class IOUtils {
    public static final File USER_DATA_DIRECTORY = new File(System.getProperty("user.home") + File.separator + ".titan");
    public static final File WALLETS_DIRECTORY = new File(USER_DATA_DIRECTORY + File.separator + "wallets");
    public static final File ETH_WALLETS_DIRECTORY = new File(WALLETS_DIRECTORY + File.separator + "ethereum");
    public static final File SOL_WALLETS_DIRECTORY = new File(WALLETS_DIRECTORY + File.separator + "solana");

    public static void createUserDataDirectories() {
        if (!USER_DATA_DIRECTORY.exists()) {
            USER_DATA_DIRECTORY.mkdir();
        }
        if (!WALLETS_DIRECTORY.exists()) {
            WALLETS_DIRECTORY.mkdir();
        }
        if (!ETH_WALLETS_DIRECTORY.exists()) {
            WALLETS_DIRECTORY.mkdir();
        }
        if (!SOL_WALLETS_DIRECTORY.exists()) {
            WALLETS_DIRECTORY.mkdir();
        }
    }

    public static void clearUserData() {
        File[] files = USER_DATA_DIRECTORY.listFiles();
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
    }
}