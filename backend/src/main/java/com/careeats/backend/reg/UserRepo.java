package com.careeats.backend.reg;

import java.util.HashMap;
import java.util.Map;

public class UserRepo {

    private Map<String, User> users = new HashMap<>();

    //stores user, and thier email is the key
    public void save(User user) {
        users.put(user.getEmail(), user);
    }

    //find user using email (key)
    public User findByEmail(String email) {
        return users.get(email);
    }

    //true if the email exists as a key, false otherwise
    public boolean emailExists(String email) {
        return users.containsKey(email);
    }
}
