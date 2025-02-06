package com.example.tb.authentication.service.admin;

import com.example.tb.model.entity.Admin;

public interface AdminImp {
    Admin login(String email, String password);
}
