package com.fptuni.vms.model;

import java.util.List;

public class UploadVolunteerRow {
    private int rowIndex;
    private User user;
    private List<String> errors;

    public UploadVolunteerRow() {}

    public UploadVolunteerRow(int rowIndex, User user, List<String> errors) {
        this.rowIndex = rowIndex;
        this.user = user;
        this.errors = errors;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public boolean isValid() {
        return errors == null || errors.isEmpty();
    }
}
