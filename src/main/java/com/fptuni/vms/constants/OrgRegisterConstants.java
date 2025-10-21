// src/main/java/com/fptuni/vms/constants/OrgRegisterConstants.java
package com.fptuni.vms.constants;

public final class OrgRegisterConstants {
    private OrgRegisterConstants() { throw new AssertionError("No instances"); }

    public static final String SESSION_PENDING_ORG = "PENDING_ORG";
    public static final String ATTR_ERROR = "error";
    public static final String ATTR_EMAIL = "email";

    public static final String VIEW_ORG_REGISTER = "auth/org-register";
    public static final String VIEW_ORG_VERIFY   = "auth/org-register-verify";

    public static final String OTP_PURPOSE_ORG_REGISTER = "ORG_REGISTER";

    public static final String E_SESSION_EXPIRED = "SESSION_EXPIRED";
    public static final String E_SYSTEM_ERROR    = "SYSTEM_ERROR";
    public static final String E_MUST_LOGOUT     = "MUST_LOGOUT";
    public static final String E_OK              = "OK";
}
