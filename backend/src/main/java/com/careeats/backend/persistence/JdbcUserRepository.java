package com.careeats.backend.persistence;

import com.careeats.backend.reg.domain.*;
import com.careeats.backend.reg.infrastructure.UserRepo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Transactional
public class JdbcUserRepository extends UserRepo {
    private final JdbcTemplate jdbc;
    public JdbcUserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public long nextId() { return jdbc.queryForObject("SELECT nextval('careeats.user_ids')", Long.class); }
    @Override public void lockForWrite() { DatabaseLock.acquire(jdbc); }
    @Override public void save(User user) {
        Objects.requireNonNull(user);
        lockForWrite();
        // Registration is insert-only; duplicate IDs/emails cannot overwrite another account.
        jdbc.update("INSERT INTO careeats.app_users(id,name,email,password_hash,role) VALUES (?,?,?,?,?)",
            user.getId(),user.getName(),user.getEmail(),user.getPasswordHash(),user.getRole().name());
    }
    @Override public User findByEmail(String email) {
        return jdbc.query("SELECT * FROM careeats.app_users WHERE email=?", (rs,n) ->
            new User(rs.getLong("id"),rs.getString("name"),rs.getString("email"),
                rs.getString("password_hash"),Role.valueOf(rs.getString("role"))),email)
            .stream().findFirst().orElse(null);
    }
    @Override public boolean emailExists(String email) { return findByEmail(email) != null; }
}
