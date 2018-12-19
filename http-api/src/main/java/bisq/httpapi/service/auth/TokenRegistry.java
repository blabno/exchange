package bisq.httpapi.service.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenRegistry {

    private static final long TTL = 30 * 60 * 1000;

    private Map<String, Long> tokens = new HashMap<>();

    public String generateToken() {
        String uuid;
        do {
            uuid = UUID.randomUUID().toString();
        } while (tokens.containsKey(uuid));
        tokens.put(uuid, System.currentTimeMillis());
        removeTimeoutTokens();
        return uuid;
    }

    boolean isValidToken(String token) {
        Long createDate = tokens.get(token);
        if (createDate == null || isTimeout(createDate)) {
            tokens.remove(token);
            return false;
        } else {
            return true;
        }
    }

    private boolean isTimeout(Long createDate) {
        return System.currentTimeMillis() - createDate > TTL;
    }

    private void removeTimeoutTokens() {
        tokens.values().removeIf(this::isTimeout);
    }

    public void clear() {
        tokens.clear();
    }
}
