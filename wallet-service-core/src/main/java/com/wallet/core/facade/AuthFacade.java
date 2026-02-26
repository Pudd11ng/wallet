package com.wallet.core.facade;

public interface AuthFacade {
    /**
     * Fetches the username of a user based on their User ID.
     * @param userId The ID
     * @return The actual username
     */
    String fetchUsername(String userId);
}
