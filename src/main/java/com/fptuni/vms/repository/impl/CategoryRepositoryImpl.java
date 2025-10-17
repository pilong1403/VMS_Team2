// src/main/java/com/fptuni/vms/repository/impl/CategoryRepositoryImpl.java
package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.Category;
import com.fptuni.vms.repository.CategoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final JdbcTemplate jdbc;

    public CategoryRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Category> M = (rs, i) -> {
        Category c = new Category();
        c.setCategoryId(rs.getInt("category_id"));
        c.setCategoryName(rs.getString("category_name"));
        c.setDescription(rs.getString("description"));
        return c;
    };

    @Override
    public List<Category> findAll() {
        String sql = """
            SELECT category_id, category_name, description
            FROM dbo.categories
            ORDER BY category_name ASC
        """;
        return jdbc.query(sql, M);
    }

    @Override
    public Optional<Category> findById(int id) {
        String sql = """
            SELECT category_id, category_name, description
            FROM dbo.categories
            WHERE category_id = ?
        """;
        List<Category> list = jdbc.query(sql, M, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Category save(Category c) {
        if (c.getCategoryId() == null || c.getCategoryId() == 0) {
            String sql = """
                INSERT INTO dbo.categories (category_name, description)
                VALUES (?, ?)
            """;
            KeyHolder kh = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, c.getCategoryName());
                ps.setString(2, c.getDescription());
                return ps;
            }, kh);
            if (kh.getKey() != null) c.setCategoryId(kh.getKey().intValue());
            return c;
        } else {
            String sql = """
                UPDATE dbo.categories
                   SET category_name = ?, description = ?
                 WHERE category_id = ?
            """;
            jdbc.update(sql, c.getCategoryName(), c.getDescription(), c.getCategoryId());
            return c;
        }
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM dbo.categories WHERE category_id = ?";
        return jdbc.update(sql, id) > 0;
    }
}
