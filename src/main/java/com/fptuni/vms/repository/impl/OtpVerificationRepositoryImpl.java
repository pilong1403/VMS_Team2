// com/fptuni/vms/repository/impl/OtpVerificationRepositoryImpl.java
package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.OtpVerification;
import com.fptuni.vms.model.OtpVerification.Purpose;
import com.fptuni.vms.repository.OtpVerificationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OtpVerificationRepositoryImpl implements OtpVerificationRepository {

    private final JdbcTemplate jdbc;

    public OtpVerificationRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<OtpVerification> MAPPER = (rs, i) -> {
        OtpVerification v = new OtpVerification();
        v.setOtpId(rs.getInt("otp_id"));
        v.setEmail(rs.getString("email"));
        v.setOtpCode(rs.getString("otp_code"));

        Timestamp exp = rs.getTimestamp("expired_at");
        v.setExpiredAt(exp != null ? exp.toLocalDateTime() : null);

        v.setVerified(rs.getBoolean("verified"));

        Timestamp created = rs.getTimestamp("created_at");
        v.setCreatedAt(created != null ? created.toLocalDateTime() : null);

        // purpose: String -> enum (null-safe + valid-only)
        String p = rs.getString("purpose");
        v.setPurpose(p != null && !p.isBlank() ? Purpose.valueOf(p) : null);

        v.setToken(rs.getString("token"));

        Timestamp consumed = rs.getTimestamp("consumed_at");
        v.setConsumedAt(consumed != null ? consumed.toLocalDateTime() : null);

        return v;
    };

    @Override
    public Optional<OtpVerification> findTop1ByEmailAndPurposeOrderByCreatedAtDesc(String email, Purpose purpose) {
        String sql =
                "SELECT TOP 1 otp_id, email, otp_code, expired_at, verified, created_at, purpose, token, consumed_at " +
                        "FROM dbo.otpverification " +
                        "WHERE email = ? AND ISNULL(purpose,'') = ISNULL(?, '') " +
                        "ORDER BY created_at DESC";

        // truyền enum dưới dạng String (name)
        String p = (purpose == null) ? null : purpose.name();
        List<OtpVerification> list = jdbc.query(sql, MAPPER, email, p);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public OtpVerification save(OtpVerification v) {
        if (v.getOtpId() == null || v.getOtpId() == 0) {
            String sql =
                    "INSERT INTO dbo.otpverification (email, otp_code, expired_at, verified, created_at, purpose, token, consumed_at) " +
                            "VALUES (?, ?, ?, ?, SYSDATETIME(), ?, ?, ?)";

            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, v.getEmail());
                ps.setString(2, v.getOtpCode());
                ps.setTimestamp(3, toTs(v.getExpiredAt()));
                ps.setBoolean(4, v.getVerified() != null ? v.getVerified() : false);
                ps.setString(5, v.getPurpose() != null ? v.getPurpose().name() : null); // enum -> String
                ps.setString(6, v.getToken());
                ps.setTimestamp(7, toTs(v.getConsumedAt()));
                return ps;
            }, kh);

            if (kh.getKey() != null) {
                v.setOtpId(kh.getKey().intValue());
            }
            return v;
        } else {
            String sql =
                    "UPDATE dbo.otpverification " +
                            "SET email = ?, otp_code = ?, expired_at = ?, verified = ?, purpose = ?, token = ?, consumed_at = ? " +
                            "WHERE otp_id = ?";

            jdbc.update(sql,
                    v.getEmail(),
                    v.getOtpCode(),
                    toTs(v.getExpiredAt()),
                    v.getVerified() != null ? v.getVerified() : false,
                    v.getPurpose() != null ? v.getPurpose().name() : null, // enum -> String
                    v.getToken(),
                    toTs(v.getConsumedAt()),
                    v.getOtpId()
            );
            return v;
        }
    }

    private static Timestamp toTs(LocalDateTime t) {
        return t == null ? null : Timestamp.valueOf(t);
    }
}
