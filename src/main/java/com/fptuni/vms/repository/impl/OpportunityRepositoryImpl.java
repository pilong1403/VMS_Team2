// src/main/java/com/fptuni/vms/repository/impl/OpportunityRepositoryImpl.java
package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.Category;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.repository.OpportunityRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OpportunityRepositoryImpl implements OpportunityRepository {

    private final JdbcTemplate jdbc;

    public OpportunityRepositoryImpl(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static Opportunity.OpportunityStatus toStatus(String s) {
        if (s == null || s.isBlank()) return null;
        return Opportunity.OpportunityStatus.valueOf(s.trim().toUpperCase());
    }

    private static Timestamp ts(LocalDateTime t) {
        return t == null ? null : Timestamp.valueOf(t);
    }

    private static final RowMapper<Opportunity> M = (rs, i) -> {
        Opportunity o = new Opportunity();
        o.setOppId(rs.getInt("opp_id"));

        Organization g = new Organization();
        g.setOrgId(rs.getInt("org_id"));
        o.setOrganization(g);

        Category c = new Category();
        c.setCategoryId(rs.getInt("category_id"));
        o.setCategory(c);

        o.setTitle(rs.getString("title"));
        o.setSubtitle(rs.getString("subtitle"));
        o.setLocation(rs.getString("location"));
        o.setThumbnailUrl(rs.getString("thumbnail_url"));
        o.setNeededVolunteers(rs.getInt("needed_volunteers"));

        o.setStatus(toStatus(rs.getString("status")));

        Timestamp st = rs.getTimestamp("start_time");
        o.setStartTime(st != null ? st.toLocalDateTime() : null);
        Timestamp et = rs.getTimestamp("end_time");
        o.setEndTime(et != null ? et.toLocalDateTime() : null);
        Timestamp ct = rs.getTimestamp("created_at");
        o.setCreatedAt(ct != null ? ct.toLocalDateTime() : null);
        return o;
    };

    @Override
    public List<Opportunity> findByOrgIdPaged(int orgId, int offset, int limit, String q, Integer categoryId, String status) {
        StringBuilder sb = new StringBuilder("""
            SELECT opp_id, org_id, category_id, title, subtitle, location, thumbnail_url,
                   needed_volunteers, status, start_time, end_time, created_at
              FROM dbo.opportunities
             WHERE org_id = ?
        """);
        List<Object> args = new ArrayList<>();
        args.add(orgId);

        if (q != null && !q.isBlank()) {
            sb.append(" AND (title LIKE ? OR subtitle LIKE ? OR location LIKE ?) ");
            String like = "%" + q.trim() + "%";
            args.add(like); args.add(like); args.add(like);
        }
        if (categoryId != null) {
            sb.append(" AND category_id = ? ");
            args.add(categoryId);
        }
        if (status != null && !status.isBlank()) {
            sb.append(" AND status = ? ");
            args.add(status.trim().toUpperCase());
        }

        sb.append(" ORDER BY created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY ");
        args.add(offset);
        args.add(limit);

        return jdbc.query(sb.toString(), M, args.toArray());
    }

    @Override
    public int countByOrgId(int orgId, String q, Integer categoryId, String status) {
        StringBuilder sb = new StringBuilder("SELECT COUNT(*) FROM dbo.opportunities WHERE org_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(orgId);

        if (q != null && !q.isBlank()) {
            sb.append(" AND (title LIKE ? OR subtitle LIKE ? OR location LIKE ?) ");
            String like = "%" + q.trim() + "%";
            args.add(like); args.add(like); args.add(like);
        }
        if (categoryId != null) {
            sb.append(" AND category_id = ? ");
            args.add(categoryId);
        }
        if (status != null && !status.isBlank()) {
            sb.append(" AND status = ? ");
            args.add(status.trim().toUpperCase());
        }

        Integer c = jdbc.queryForObject(sb.toString(), Integer.class, args.toArray());
        return c == null ? 0 : c;
    }

    @Override
    public Optional<Opportunity> findByIdAndOrg(int oppId, int orgId) {
        String sql = """
            SELECT opp_id, org_id, category_id, title, subtitle, location, thumbnail_url,
                   needed_volunteers, status, start_time, end_time, created_at
              FROM dbo.opportunities
             WHERE opp_id = ? AND org_id = ?
        """;
        List<Opportunity> list = jdbc.query(sql, M, oppId, orgId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Opportunity save(Opportunity o) {
        if (o.getOppId() == null || o.getOppId() == 0) {
            String sql = """
                INSERT INTO dbo.opportunities
                  (org_id, category_id, title, subtitle, location, thumbnail_url,
                   needed_volunteers, status, start_time, end_time, created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,SYSDATETIME())
            """;
            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, o.getOrganization().getOrgId());
                ps.setInt(2, o.getCategory().getCategoryId());
                ps.setString(3, o.getTitle());
                ps.setString(4, o.getSubtitle());
                ps.setString(5, o.getLocation());
                ps.setString(6, o.getThumbnailUrl());
                ps.setInt(7, o.getNeededVolunteers());
                String status = (o.getStatus() != null ? o.getStatus() : Opportunity.OpportunityStatus.OPEN).name();
                ps.setString(8, status);
                ps.setTimestamp(9, ts(o.getStartTime()));
                ps.setTimestamp(10, ts(o.getEndTime()));
                return ps;
            }, kh);
            if (kh.getKey() != null) {
                o.setOppId(kh.getKey().intValue());
            }
            return o;
        } else {
            String sql = """
                UPDATE dbo.opportunities
                   SET category_id = ?,
                       title = ?,
                       subtitle = ?,
                       location = ?,
                       thumbnail_url = ?,
                       needed_volunteers = ?,
                       status = ?,
                       start_time = ?,
                       end_time = ?
                 WHERE opp_id = ? AND org_id = ?
            """;
            jdbc.update(sql,
                    o.getCategory().getCategoryId(),
                    o.getTitle(),
                    o.getSubtitle(),
                    o.getLocation(),
                    o.getThumbnailUrl(),
                    o.getNeededVolunteers(),
                    (o.getStatus() != null ? o.getStatus() : Opportunity.OpportunityStatus.OPEN).name(),
                    ts(o.getStartTime()),
                    ts(o.getEndTime()),
                    o.getOppId(),
                    o.getOrganization().getOrgId()
            );
            return o;
        }
    }

    @Override
    public boolean deleteByIdAndOrg(int oppId, int orgId) {
        String sql = "DELETE FROM dbo.opportunities WHERE opp_id = ? AND org_id = ?";
        return jdbc.update(sql, oppId, orgId) > 0;
    }

    @Override
    public List<Opportunity> findRecentByOrg(int orgId, LocalDateTime from, LocalDateTime to) {
        String sql = """
            SELECT opp_id, org_id, category_id, title, subtitle, location, thumbnail_url,
                   needed_volunteers, status, start_time, end_time, created_at
              FROM dbo.opportunities
             WHERE org_id = ? AND created_at BETWEEN ? AND ?
             ORDER BY created_at DESC
        """;
        return jdbc.query(sql, M, orgId, ts(from), ts(to));
    }
}
