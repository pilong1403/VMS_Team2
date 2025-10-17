// src/main/java/com/fptuni/vms/repository/impl/OrganizationRepositoryImpl.java
package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.Organization;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.OrganizationRepository;
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
public class OrganizationRepositoryImpl implements OrganizationRepository {

    private final JdbcTemplate jdbc;

    public OrganizationRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Organization> MAPPER = (rs, i) -> {
        Organization o = new Organization();
        o.setOrgId(rs.getInt("org_id"));

        User owner = new User();
        owner.setUserId(rs.getInt("owner_id"));
        o.setOwner(owner);

        o.setName(rs.getString("name"));
        o.setDescription(rs.getString("description"));

        Timestamp c = rs.getTimestamp("created_at");
        o.setCreatedAt(c != null ? c.toLocalDateTime() : null);

        String rsStatus = rs.getString("reg_status");
        if (rsStatus != null) o.setRegStatus(Organization.RegStatus.valueOf(rsStatus));

        o.setRegDocUrl(rs.getString("reg_doc_url"));
        o.setRegDocCloudId(rs.getString("reg_doc_cloud_id"));
        o.setRegNote(rs.getString("reg_note"));

        Timestamp sub = rs.getTimestamp("reg_submitted_at");
        o.setRegSubmittedAt(sub != null ? sub.toLocalDateTime() : null);

        Integer reviewedById = rs.getObject("reg_reviewed_by", Integer.class);
        if (reviewedById != null) {
            User reviewer = new User();
            reviewer.setUserId(reviewedById);
            o.setRegReviewedBy(reviewer);
        }

        Timestamp rAt = rs.getTimestamp("reg_reviewed_at");
        o.setRegReviewedAt(rAt != null ? rAt.toLocalDateTime() : null);

        return o;
    };

    @Override
    public Optional<Organization> findByOwnerId(Integer ownerId) {
        String sql = """
            SELECT org_id, owner_id, name, description, created_at, reg_status,
                   reg_doc_url, reg_doc_cloud_id, reg_note, reg_submitted_at,
                   reg_reviewed_by, reg_reviewed_at
            FROM dbo.organizations
            WHERE owner_id = ?
        """;
        List<Organization> list = jdbc.query(sql, MAPPER, ownerId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public boolean existsByOwner(User owner) {
        if (owner == null || owner.getUserId() == null) return false;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.organizations WHERE owner_id = ?",
                Integer.class, owner.getUserId()
        );
        return count != null && count > 0;
    }

    @Override
    public Organization save(Organization o) {
        // Chỉ xử lý INSERT cho luồng submit đăng ký
        String sql = """
            INSERT INTO dbo.organizations (
                owner_id, name, description, reg_status,
                reg_doc_url, reg_doc_cloud_id, reg_note, reg_submitted_at, created_at
            ) VALUES (?,?,?,?,?,?,?,?,SYSDATETIME())
        """;

        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, o.getOwner().getUserId());
            ps.setString(2, o.getName());
            ps.setString(3, o.getDescription());
            ps.setString(4, o.getRegStatus() != null ? o.getRegStatus().name() : Organization.RegStatus.PENDING.name());
            ps.setString(5, o.getRegDocUrl());
            ps.setString(6, o.getRegDocCloudId());
            ps.setString(7, o.getRegNote());
            LocalDateTime submitted = o.getRegSubmittedAt();
            ps.setTimestamp(8, submitted != null ? Timestamp.valueOf(submitted) : null);
            return ps;
        }, kh);

        if (kh.getKey() != null) o.setOrgId(kh.getKey().intValue());
        return o;
    }
}
